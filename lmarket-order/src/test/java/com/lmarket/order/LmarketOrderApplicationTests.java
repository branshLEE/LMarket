package com.lmarket.order;

import com.lmarket.order.entity.OrderReturnReasonEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;

@Slf4j
@SpringBootTest
class LmarketOrderApplicationTests {

	@Autowired
	AmqpAdmin amqpAdmin;

	@Autowired
	RabbitTemplate rabbitTemplate;

	@Test
	void contextLoads() {
	}

	/**
	 * 发消息
	 */
	@Test
	void sendMsg(){
		//如果发送的消息是一个对象，则会使用序列化机制，将对象写出去。所以对象必须实现序列化Serializable
		OrderReturnReasonEntity returnReasonEntity = new OrderReturnReasonEntity();
		returnReasonEntity.setId(1L);
		returnReasonEntity.setCreateTime(new Date());
		returnReasonEntity.setName("哈哈哈");

		String msg = "brandLEE";

		//2、发送的对象的消息，可以是一个json
		rabbitTemplate.convertAndSend("hello-java-exchange", "hello.java", returnReasonEntity);
		log.info("消息发送完成{}", returnReasonEntity);
	}

	/**
	 * 创建交换机
	 */
	@Test
	void createExchange(){

		DirectExchange directExchange = new DirectExchange("hello-java-exchange", true, false);
		amqpAdmin.declareExchange(directExchange);
		log.info("Exchange【{}】创建成功", "hello-java-exchange");

	}

	/**
	 * 创建队列
	 */
	@Test
	void createQueue(){
		Queue queue = new Queue("hello-java-queue", true, false, false);
		amqpAdmin.declareQueue(queue);
		log.info("Queue【{}】创建成功", "hello-java-queue");
	}

	/**
	 * 绑定操作
	 */
	@Test
	void createBinding(){
		Binding binding = new Binding("hello-java-queue",
				Binding.DestinationType.QUEUE, "hello-java-exchange", "hello.java", null);
		amqpAdmin.declareBinding(binding);
		log.info("Binding【{}】创建成功", "hello-java-binding");
	}

}
