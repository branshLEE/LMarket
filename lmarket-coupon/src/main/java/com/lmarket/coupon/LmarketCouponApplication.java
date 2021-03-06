package com.lmarket.coupon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class LmarketCouponApplication {

	public static void main(String[] args) {
		SpringApplication.run(LmarketCouponApplication.class, args);
	}

}
