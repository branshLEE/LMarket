package com.lmarket.ware.listener;

import com.alibaba.fastjson.TypeReference;
import com.common.to.mq.OrderTo;
import com.common.to.mq.StockDetailTo;
import com.common.to.mq.StockLockedTo;
import com.common.utils.R;
import com.lmarket.ware.entity.WareOrderTaskDetailEntity;
import com.lmarket.ware.entity.WareOrderTaskEntity;
import com.lmarket.ware.feign.OrderFeignService;
import com.lmarket.ware.service.WareOrderTaskDetailService;
import com.lmarket.ware.service.WareOrderTaskService;
import com.lmarket.ware.service.WareSkuService;
import com.lmarket.ware.vo.OrderVo;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RabbitListener(queues = "stock.release.stock.queue")
public class StockReleaseListener {

    @Autowired
    WareSkuService wareSkuService;

    @RabbitHandler
    public void handleStockLockedRelease(StockLockedTo to, Message message, Channel channel) throws IOException {

        System.out.println("收到解锁库存的消息："+to.getDetial().getSkuId());
        try {
            wareSkuService.unlockStock(to);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }catch (Exception e){
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }

    }

    @RabbitHandler
    public void handleOrderCloseRelease(OrderTo to, Message message, Channel channel) throws IOException {
        System.out.println("收到订单关闭消息，准备解锁库存："+to.getOrderSn());
        try{
            wareSkuService.unlockStock(to);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }catch (Exception e){
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }

    }

}
