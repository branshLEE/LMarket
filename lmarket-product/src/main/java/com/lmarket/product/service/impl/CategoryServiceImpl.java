package com.lmarket.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.common.utils.PageUtils;
import com.common.utils.Query;
import com.lmarket.product.service.CategoryBrandRelationService;
import com.lmarket.product.vo.Catelog2Vo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.lmarket.product.dao.CategoryDao;
import com.lmarket.product.entity.CategoryEntity;
import com.lmarket.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

//    @Autowired
//    CategoryDao categoryDao;

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    RedissonClient redisson;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        //1、查出所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);
        //2、组装成父子的树型结构
        //2.1、找到所有的一级分类
        List<CategoryEntity> level1Menus = entities.stream().filter((categoryEntity) -> {
            return categoryEntity.getParentCid() == 0;
        }).map((menu) -> {
            menu.setChildren(getChildrens(menu, entities));
            return menu;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());


        return level1Menus;
    }

    @Override
    public void removeMenusByIds(List<Long> asList) {
        baseMapper.deleteBatchIds(asList);
    }

    @Override
    public Long[] findCatelogPath(Long catelogId) {

        List<Long> paths = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, paths);

        Collections.reverse(parentPath);

        return (Long[]) parentPath.toArray(new Long[parentPath.size()]);
    }

    private List<Long> findParentPath(Long catelogId, List<Long> paths) {
        //1、收集当前节点id
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if (byId.getParentCid() != 0) {
            findParentPath(byId.getParentCid(), paths);
        }
        return paths;
    }

    /**
     * 级联更新所有关联的数据
     *
     * @param category
     */
    @CacheEvict(value = "category", allEntries = true) //失效模式
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());

    }

    //每一个需要缓存的数据都指定要放入那个名字的缓存（缓存分区（按业务类型分区））
    @Cacheable(value = "category", key = "#root.method.name") //代表当前方法的结果需要缓存，如果缓存中有，方法不用调用，否则调用方法，最后将方法的结果放入缓存
    @Override
    public List<CategoryEntity> getLevel1Categorys() {

        List<CategoryEntity> categoryEntities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
        return categoryEntities;
    }


    @Cacheable(value = "category", key = "#root.method.name")
    @Override
    public Map<String, List<Catelog2Vo>> getCatelogJson() {
        System.out.println("查询了数据库....");

        List<CategoryEntity> selectList = baseMapper.selectList(null);


        //1、查出所有一级分类
        List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);

        //2、封装数据
        Map<String, List<Catelog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //1、拿到每一个的一级分类，查到这个一级分类的所有二级分类
            List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());
            //2、封装上面的结果
            List<Catelog2Vo> catelog2Vos = null;
            if (categoryEntities != null) {
                catelog2Vos = categoryEntities.stream().map(l2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    //1、找当前二级分类的三级分类封装成vo
                    List<CategoryEntity> level3Catelog = getParent_cid(selectList, l2.getCatId());
                    if (level3Catelog != null) {
                        List<Catelog2Vo.Catelog3Vo> collect = level3Catelog.stream().map(l3 -> {
                            //2、封装成指定的格式
                            Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                            return catelog3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatelog3List(collect);

                    }

                    return catelog2Vo;
                }).collect(Collectors.toList());
            }

            return catelog2Vos;
        }));

        return parent_cid;
    }


    // 产生堆外内存溢出：OutOfDirectMemoryError (lettuce版本升级到5.1.8以上解决，或者更换jedis客户端)

    public Map<String, List<Catelog2Vo>> getCatelogJson1() {
        //给缓存中放json字符串，拿出的json字符串，需要逆转位能使用的对象类型（序列化与反序列化）

        /**
         * 1、空结果缓存：解决缓存穿透
         * 2、设置过期时间（加随机值）：解决缓存雪崩
         * 3、加锁：解决缓存击穿
         */

        //1、加入缓存逻辑,缓存中存的数据是json字符串
        //JSON的好处是跨语言跨平台兼容
        String catelogJSON = redisTemplate.opsForValue().get("catelogJSON");
        if (StringUtils.isEmpty(catelogJSON)) {
            //2、缓存中没有数据,则查询数据库
            Map<String, List<Catelog2Vo>> catelogJsonFromDb = getCatelogJsonFromDbWithRedissonLock();

            return catelogJsonFromDb;
        }
        //转为指定的对象
        TypeReference<Map<String, List<Catelog2Vo>>> typeReference = new TypeReference<>() {
        };
        Map<String, List<Catelog2Vo>> result = JSON.parseObject(catelogJSON, typeReference);
        return result;
    }

    /**
     * 缓存里的数据需要和数据库的保持一致（缓存数据一致性）
     * @return
     */
    public Map<String, List<Catelog2Vo>> getCatelogJsonFromDbWithRedissonLock() {
        //1、锁的名字，粒度越细,则越快越好
        //锁的粒度：具体缓存的是某个数据
        RLock lock = redisson.getLock("CatelogJson-Lock");
        lock.lock();

        Map<String, List<Catelog2Vo>> dataFromDb;
        try {
            dataFromDb = getDataFromDb();
        } finally {
            lock.unlock();
        }

        return dataFromDb;
    }

    public Map<String, List<Catelog2Vo>> getCatelogJsonFromDbWithRedisLock() {
        //1、占分布式锁，去redis占坑
        String uuid = UUID.randomUUID().toString();

        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", "111", 300, TimeUnit.SECONDS);
        if (lock) {
            System.out.println("获取分布式锁成功...");
            //加锁成功...执行进数据库业务
            //redisTemplate.expire("lock", 30, TimeUnit.SECONDS); //2、设置锁的过期时间(必须和加锁是同步的，一个原子操作)
            Map<String, List<Catelog2Vo>> dataFromDb;
            try {
                dataFromDb = getDataFromDb();
            } finally {
                //Lua脚本
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                //删除锁（原子性）
                Long lock1 = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList("lock"), uuid);
            }

            //获取锁的值，然后对比，对比成功后删除（这个一定要是原子操作：使用redis+Lua脚本）
//            String lockValue = redisTemplate.opsForValue().get("lock");
//            if(uuid.equals(lockValue)){
//                redisTemplate.delete("lock"); //删除锁
//            }


            return dataFromDb;
        } else {
            //加锁失败，重试。类似本地锁synchronized()
            //休眠100ms再重试
            System.out.println("获取分布式锁失败...等待重试");
            try {
                Thread.sleep(200);
            } catch (Exception e) {

            }
            return getCatelogJsonFromDbWithRedisLock(); //自旋
        }
    }

    //从数据库查询并封装分类数据
    private Map<String, List<Catelog2Vo>> getDataFromDb() {
        String catelogJSON = redisTemplate.opsForValue().get("catelogJSON");
        if (!StringUtils.isEmpty(catelogJSON)) {
            //缓存不为空，直接返回
            TypeReference<Map<String, List<Catelog2Vo>>> typeReference = new TypeReference<>() {
            };
            Map<String, List<Catelog2Vo>> result = JSON.parseObject(catelogJSON, typeReference);
            return result;
        }

        System.out.println("查询了数据库....");


        /**
         * 1、将数据库的多次查询变为一次
         */
        List<CategoryEntity> selectList = baseMapper.selectList(null);


        //1、查出所有一级分类
        List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);

        //2、封装数据
        Map<String, List<Catelog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //1、拿到每一个的一级分类，查到这个一级分类的所有二级分类
            List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());
            //2、封装上面的结果
            List<Catelog2Vo> catelog2Vos = null;
            if (categoryEntities != null) {
                catelog2Vos = categoryEntities.stream().map(l2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    //1、找当前二级分类的三级分类封装成vo
                    List<CategoryEntity> level3Catelog = getParent_cid(selectList, l2.getCatId());
                    if (level3Catelog != null) {
                        List<Catelog2Vo.Catelog3Vo> collect = level3Catelog.stream().map(l3 -> {
                            //2、封装成指定的格式
                            Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                            return catelog3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatelog3List(collect);

                    }

                    return catelog2Vo;
                }).collect(Collectors.toList());
            }

            return catelog2Vos;
        }));

        //3、将查到的数据放入缓存，将对象转为json放入缓存中
        String s = JSON.toJSONString(parent_cid);
        redisTemplate.opsForValue().set("catelogJSON", s, 1, TimeUnit.DAYS);
        return parent_cid;
    }

    //加本地锁
    public Map<String, List<Catelog2Vo>> getCatelogJsonFromDbWithLocalLock() {

        synchronized (this) {
            return getDataFromDb();
        }
    }

    private List<CategoryEntity> getParent_cid(List<CategoryEntity> selectList, Long parent_cid) {
        List<CategoryEntity> collect = selectList.stream().filter(item -> item.getParentCid() == parent_cid).collect(Collectors.toList());
        //return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", v.getCatId()));
        return collect;
    }


    //递归查找所有菜单的子菜单
    private List<CategoryEntity> getChildrens(CategoryEntity root, List<CategoryEntity> all) {

        List<CategoryEntity> children = all.stream().filter(categoryEntity -> {
            return categoryEntity.getParentCid() == root.getCatId();
        }).map(categoryEntity -> {
            //1、找子菜单
            categoryEntity.setChildren(getChildrens(categoryEntity, all));
            return categoryEntity;
        }).sorted((menu1, menu2) -> {
            //2、菜单的排序
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());


        return children;
    }

}