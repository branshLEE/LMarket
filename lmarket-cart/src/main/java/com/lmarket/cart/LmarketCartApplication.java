package com.lmarket.cart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;


@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication
public class LmarketCartApplication {

    public static void main(String[] args) {
        SpringApplication.run(LmarketCartApplication.class, args);
    }

}
