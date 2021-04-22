package com.lmarket.thirdparty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;


@EnableDiscoveryClient
@SpringBootApplication
public class LmarketThirdPartyApplication {

    public static void main(String[] args) {
        SpringApplication.run(LmarketThirdPartyApplication.class, args);
    }

}
