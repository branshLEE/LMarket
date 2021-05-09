package com.lmarket.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.common.to.SeckillOrderTo;
import com.common.utils.PageUtils;
import com.lmarket.order.entity.OrderEntity;
import com.lmarket.order.vo.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 订单
 *
 * @author branshlee
 * @email branshLEE@gmail.com
 * @date 2021-02-22 23:56:18
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 订单确认页返回需要用的数据
     * @return
     */
    OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException;

    SubmitOrderResponseVo submitOrder(OrderSubmitVo vo);

    OrderEntity getOrderStatusByOrderSn(String orderSn);

    void closeOrder(OrderEntity entity);

    /**
     * 获取当前订单的支付信息
     * @param orderSn
     * @return
     */
    PayVo getOrderPay(String orderSn);

    PageUtils queryPageWithOrderItem(Map<String, Object> params);

    List<OrderEntity> listOrder();

    String handlePayResult(PayAsyncVo vo);

    void createSeckillOrder(SeckillOrderTo to);
}

