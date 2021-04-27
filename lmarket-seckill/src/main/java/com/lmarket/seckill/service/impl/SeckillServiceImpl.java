package com.lmarket.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.common.to.SkuReductionTo;
import com.common.utils.R;
import com.lmarket.seckill.feign.CouponFeignService;
import com.lmarket.seckill.feign.ProductFeignService;
import com.lmarket.seckill.service.SeckillService;
import com.lmarket.seckill.to.SeckillSkuRedisTo;
import com.lmarket.seckill.vo.SeckillSessionsWithSkus;
import com.lmarket.seckill.vo.SeckillSkuVo;
import com.lmarket.seckill.vo.SkuInfoVo;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    CouponFeignService couponFeignService;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    RedissonClient redissonClient;

    private final String SESSION_CACHE_PREFIX = "seckill:sessions:";

    private final String SKUKILL_CACHE_PREFIX = "seckill:skus";

    private final String SKU_STOCK_SEMAPHORE = "seckill:stock:"; //后面跟商品随机码

    @Override
    public void uploadSeckillSkuLatest3Days() {
        //1、扫描最近3天需要参与秒杀的活动
        R lates3DaySession = couponFeignService.getLates3DaySession();
        if(lates3DaySession.getCode() == 0){
            //上架秒杀商品
            List<SeckillSessionsWithSkus> data = lates3DaySession.getData(new TypeReference<List<SeckillSessionsWithSkus>>() {
            });
            //缓存到redis
            //1、缓存活动信息
            saveSessionInfos(data);
            //2、缓存活动关联的商品信息
            saveSessionSkuInfos(data);
        }
    }

    @Override
    public List<SeckillSkuRedisTo> getCurrentSeckillSkus() {

        //1、确定当前时间属于那个秒杀场次
        long time = new Date().getTime();
        Set<String> keys = redisTemplate.keys(SESSION_CACHE_PREFIX + "*");
        for (String key : keys) {
            //"seckill:sessions:1619568000000_1619575200000"
            String[] s = key.replace(SESSION_CACHE_PREFIX, "").split("_");
            Long start = Long.parseLong(s[0]);
            Long end = Long.parseLong(s[1]);
            if(time >= start && time <= end){
                //2、获取该秒杀场次需要的所有商品信息
                List<String> range = redisTemplate.opsForList().range(key, -100, 100);
                BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
                List<String> list = hashOps.multiGet(range);
                if(list != null){
                    List<SeckillSkuRedisTo> collect = list.stream().map(item -> {
                        SeckillSkuRedisTo redis = JSON.parseObject(item.toString(), SeckillSkuRedisTo.class);
//                        redis.setRandomCode(null); 当前秒杀开始了，则会需要随机码
                        return redis;
                    }).collect(Collectors.toList());
                    return collect;
                }
                break;
            }
        }

        return null;
    }

    private void saveSessionInfos(List<SeckillSessionsWithSkus> sessions){
        sessions.stream().forEach(session->{
            long startTime = session.getStartTime().getTime();
            long endTime = session.getEndTime().getTime();
            String key = SESSION_CACHE_PREFIX + startTime + "_" + endTime;

            //缓存到活动信息
            Boolean hasKey = redisTemplate.hasKey(key);
            //保证幂等性
            if(!hasKey){
                List<String> collect = session.getRelationEntityList().stream().map(item ->
                        item.getPromotionSessionId().toString()+"_"+item.getSkuId().toString())
                        .collect(Collectors.toList());
                redisTemplate.opsForList().leftPushAll(key, collect);
            }


        });
    }

    private void saveSessionSkuInfos(List<SeckillSessionsWithSkus> sessions){
        sessions.stream().forEach(session->{
            //准备hash操作
            BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
            session.getRelationEntityList().stream().forEach(seckillSkuVo -> {
                //生成随机码
                String token = UUID.randomUUID().toString().replace("-", "");

                if(!ops.hasKey(seckillSkuVo.getPromotionSessionId().toString()+"_"+seckillSkuVo.getSkuId().toString())){ //保证幂等性
                    //缓存商品信息
                    SeckillSkuRedisTo skuRedisTo = new SeckillSkuRedisTo();
                    //1、sku的基本信息
                    R skuInfo = productFeignService.getSkuInfo(seckillSkuVo.getSkuId());
                    if(skuInfo.getCode() == 0){
                        SkuInfoVo info = skuInfo.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                        });
                        skuRedisTo.setSkuInfo(info);
                    }

                    //2、sku的秒杀信息
                    BeanUtils.copyProperties(seckillSkuVo, skuRedisTo);
                    //3、设置当前商品的秒杀时间信息
                    skuRedisTo.setStartTime(session.getStartTime().getTime());
                    skuRedisTo.setEndTime(session.getEndTime().getTime());

                    //4、设置随机码
                    skuRedisTo.setRandomCode(token);

                    Boolean hasKey = redisTemplate.hasKey(SKU_STOCK_SEMAPHORE);

                    //5、使用库存作为分布式的信号量(作用是限流)
                    RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + token);
                    //商品可以秒杀的数量作为信号量
                    semaphore.trySetPermits(seckillSkuVo.getSeckillCount());

                    String s = JSON.toJSONString(skuRedisTo);
                    ops.put(seckillSkuVo.getPromotionSessionId().toString()+"_"+seckillSkuVo.getSkuId().toString(), s);
                }
            });
        });
    }
}
