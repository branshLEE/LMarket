package com.lmarket.search;

import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
public class LmarketSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(LmarketSearchApplication.class, args);
    }

}
