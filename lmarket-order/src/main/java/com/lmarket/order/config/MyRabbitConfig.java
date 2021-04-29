package com.lmarket.order.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.annotation.PostConstruct;

@Configuration
@Slf4j
public class MyRabbitConfig {

//    @Autowired
    RabbitTemplate rabbitTemplate;

    @Primary
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory){
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setConnectionFactory(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());

        this.rabbitTemplate = rabbitTemplate;
        initRabbitTemplate();

        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return factory;
    }

    /**
     * 使用json序列化机制，进行消息转换
     * @return
     */
    @Bean
    public MessageConverter messageConverter(){

        return new Jackson2JsonMessageConverter();
    }

    /**
     * 定制RabbitTemplate,确保消息不丢失
     * 生产端消ConfirmCallback,ReturnCallback
     * 消费端ACK机制
     */
//    @PostConstruct
    public void initRabbitTemplate() {
        // ConfirmCallback消息抵达交换机的回调
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            /**
             * 交换机(Exchange)收到消息就会回调
             * CorrelationData 当前消息的唯一关联数据
             * ack 消息是否成功送达
             * cause 失败的原因
             */
            @Override
            public void confirm(CorrelationData correlationData, boolean ack, String cause) {
                log.info("ID:[{}]的消息成功投递到交换机",correlationData.getId());
            }
        });

        // ReturnCallback消息抵达队列的回调
        rabbitTemplate.setReturnCallback(new RabbitTemplate.ReturnCallback() {
            /**
             * 只要消息没有投递给指定的队列就会触发该回调
             *
             * @param message    投递失败的消息
             * @param replayCode 回复的状态码
             * @param replayText 回复的文本内容
             * @param exchange   交换机
             * @param routingKey 路由key
             */
            @Override
            public void returnedMessage(Message message, int replayCode, String replayText, String exchange, String routingKey) {
                System.out.println(message);
                log.error("ID:[{}]的消息失败投递到队列", message.getMessageProperties().getMessageId());
            }
        });

    }

}
