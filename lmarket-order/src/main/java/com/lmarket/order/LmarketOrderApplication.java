package com.lmarket.order;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 使用RabbitMQ
 * 1、引入amqp场景
 */

@EnableRabbit
@SpringBootApplication
public class LmarketOrderApplication {

	public static void main(String[] args) {
		SpringApplication.run(LmarketOrderApplication.class, args);
	}

}
