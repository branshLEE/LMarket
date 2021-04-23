package com.lmarket.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.common.exception.NoStockException;
import com.common.to.mq.StockDetailTo;
import com.common.to.mq.StockLockedTo;
import com.lmarket.ware.entity.WareOrderTaskDetailEntity;
import com.lmarket.ware.entity.WareOrderTaskEntity;
import com.lmarket.ware.feign.OrderFeignService;
import com.lmarket.ware.service.WareOrderTaskDetailService;
import com.lmarket.ware.service.WareOrderTaskService;
import com.lmarket.ware.to.SkuWareHasStockTo;
import com.common.utils.R;
import com.lmarket.ware.feign.ProductFeignService;
import com.lmarket.ware.vo.OrderItemVo;
import com.lmarket.ware.vo.OrderVo;
import com.lmarket.ware.vo.SkuHasStockVo;
import com.lmarket.ware.vo.WareSkuLockVo;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.common.utils.PageUtils;
import com.common.utils.Query;

import com.lmarket.ware.dao.WareSkuDao;
import com.lmarket.ware.entity.WareSkuEntity;
import com.lmarket.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    WareSkuDao wareSkuDao;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    WareOrderTaskService orderTaskService;

    @Autowired
    WareOrderTaskDetailService orderTaskDetailService;

    @Autowired
    OrderFeignService orderFeignService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    /**
     * 解锁库存
     */
    private void unLockStock(Long skuId, Long wareId, Integer num, Long taskDetailId){
        wareSkuDao.unLockStock(skuId, wareId, num);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {

        QueryWrapper<WareSkuEntity> queryWrapper = new QueryWrapper<>();
        String skuId = (String) params.get("skuId");
        if(!StringUtils.isEmpty(skuId)){
            queryWrapper.eq("sku_id", skuId);
        }

        String wareId = (String) params.get("wareId");
        if(!StringUtils.isEmpty(wareId)){
            queryWrapper.eq("ware_id", wareId);
        }

        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        //1、判断如果还没有这个库存记录，则新增
        List<WareSkuEntity> entities = wareSkuDao.selectList(new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId));
        if(entities.size() == 0 || entities == null){
            WareSkuEntity skuEntity = new WareSkuEntity();
            skuEntity.setSkuId(skuId);
            skuEntity.setStock(skuNum);
            skuEntity.setWareId(wareId);
            skuEntity.setStockLocked(0);
            //TODO 远程查询sku的名字，如果失败，整个事务不需要回滚
            //1、自己catch异常
            //TODO 还可以用别的方法让异常出现之后不回滚
            try{
                R info = productFeignService.info(skuId);
                Map<String, Object> data = (Map<String, Object>) info.get("skuInfo");
                if(info.getCode() == 0){
                    skuEntity.setSkuName((String) data.get("skuName"));
                }
            }catch (Exception e){

            }

            wareSkuDao.insert(skuEntity);
        }else{
            wareSkuDao.addStock(skuId, wareId, skuNum);
        }
    }

    @Override
    public List<SkuHasStockVo> getSkusHasStock(List<Long> skuIds) {

        List<SkuHasStockVo> collect = skuIds.stream().map(skuId -> {
            SkuHasStockVo vo = new SkuHasStockVo();

            //查询当前sku的总库存量
            //select sum(stock-stock_locked) from wms_ware_sku where sku_id = 1
            //SELECT SUM(stock-CASE WHEN stock_locked IS NULL THEN 0 ELSE stock_locked END) counts FROM `wms_ware_sku` where sku_id = 1
            Long count = baseMapper.getSkuStock(skuId);
            vo.setSkuId(skuId);
            vo.setHasStock(count == null ? false : count > 0);

            return vo;
        }).collect(Collectors.toList());

        return collect;
    }

    /**
     * 为某个订单锁定库存
     * 默认只要是运行时异常都会回滚
     * @param vo
     * @return
     */
    @Transactional(rollbackFor = NoStockException.class)
    @Override
    public Boolean orderLockStock(WareSkuLockVo vo) {
        /**
         * 保存库存工作单的详情（追溯）
         */
        WareOrderTaskEntity taskEntity = new WareOrderTaskEntity();
        taskEntity.setOrderSn(vo.getOrderSn());
        orderTaskService.save(taskEntity);

        //TODO 按照下单的收货地址，找到一个就近仓库，锁定库存

        //1、找到每个商品在哪些仓库都有库存
        List<OrderItemVo> locks = vo.getLocks();

        List<SkuWareHasStockTo> collect = locks.stream().map(item -> {
            SkuWareHasStockTo stock = new SkuWareHasStockTo();
            Long skuId = item.getSkuId();
            stock.setSkuId(skuId);
            stock.setNum(item.getCount());
            //查询这个商品在哪里有库存
            List<Long> wareIds = wareSkuDao.ListWareIdHasSkuStock(skuId, stock.getNum());
            stock.setWareId(wareIds);
            return stock;
        }).collect(Collectors.toList());

        //2、锁定库存
        for (SkuWareHasStockTo hasStock : collect) {
            Boolean skuStock = false;
            Long skuId = hasStock.getSkuId();
            List<Long> wareIds = hasStock.getWareId();
            if(CollectionUtils.isEmpty(wareIds)){
                //没有任何仓库有该商品的库存
                throw new NoStockException(skuId);
            }
            for (Long wareId : wareIds) {
                //成功就返回1，否则是0
                Long count = wareSkuDao.lockSkuStock(skuId, wareId, hasStock.getNum());
                if(count == 1){
                    System.out.println("库存锁定成功："+skuId);
                    //告诉MQ，库存锁定成功
                    WareOrderTaskDetailEntity detailEntity = new WareOrderTaskDetailEntity(null, skuId, null, hasStock.getNum(), taskEntity.getId(), wareId, 1);
                    orderTaskDetailService.save(detailEntity);
                    StockLockedTo stockLockedTo = new StockLockedTo();
                    stockLockedTo.setId(taskEntity.getId());
                    StockDetailTo stockDetailTo = new StockDetailTo();
                    BeanUtils.copyProperties(detailEntity, stockDetailTo);
                    stockLockedTo.setDetial(stockDetailTo);
                    rabbitTemplate.convertAndSend("stock-event-exchange",
                            "stock.locked",
                            stockLockedTo);
                    skuStock = true;
                    break;
                }else{
                    //当前仓库锁失败，重试下一个仓库
                }
            }
            if(skuStock == false){
                //当前商品所有仓库都没有锁住
                throw new NoStockException(skuId);
            }
        }

        //3、来到这里，肯定全部都是锁定成功过的

        return true;
    }

    @Override
    public void unlockStock(StockLockedTo to) {

        StockDetailTo detial = to.getDetial();
        Long detialId = detial.getId(); //工作单详情id

        WareOrderTaskDetailEntity byId = orderTaskDetailService.getById(detialId);
        if(byId != null){
            //解锁
            Long id = to.getId(); //库存工作单的id
            WareOrderTaskEntity taskEntity = orderTaskService.getById(id);
            String orderSn = taskEntity.getOrderSn(); //根据订单号查询订单的状态
            R r = orderFeignService.getOrderStatus(orderSn);
            if(r.getCode() == 0){
                //订单数据返回成功
                OrderVo data = r.getData(new TypeReference<OrderVo>() {
                });
                //4-订单为取消状态或者订单不存在，才可以解锁
                if(data == null || data.getStatus() == 4){
                    unLockStock(detial.getSkuId(), detial.getWareId(), detial.getSkuNum(), detialId);
                }
            }else{
                //远程查库存失败
                //消息拒绝以后重新放到队列里，可以继续消费解锁
                throw new RuntimeException("远程服务失败");
            }

        }else{
            //查无此工作单id，则无需解锁
        }
    }

}