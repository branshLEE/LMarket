package com.lmarket.ware;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableTransactionManagement
@MapperScan("com.lmarket.ware.dao")
@EnableDiscoveryClient
@SpringBootApplication
public class LmarketWareApplication {

	public static void main(String[] args) {

		SpringApplication.run(LmarketWareApplication.class, args);
	}

}
