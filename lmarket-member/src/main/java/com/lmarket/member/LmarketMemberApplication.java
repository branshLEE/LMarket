package com.lmarket.member;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class LmarketMemberApplication {

	public static void main(String[] args) {
		SpringApplication.run(LmarketMemberApplication.class, args);
	}

}
