package com.lmarket.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * 1、整合Mybatis-plus
 * 	1)导入依赖
 * 			<dependency>
 *             <groupId>com.baomidou</groupId>
 *             <artifactId>mybatis-plus-boot-starter</artifactId>
 *             <version>LATEST</version>
 *         </dependency>
 *
 *  2）配置
 *  	1、配置数据源；
 *  		1)导入数据库的驱动；
 *  		2)在application.yml里配置数据源相关信息；
 *  	2、配置Mybatis-plus;
 *  		1）使用MapperScan
 *  		2）告诉Mybatis-plus, sql配置文件位置
 */

@EnableRedisHttpSession
@EnableFeignClients(basePackages = "com.lmarket.product.feign")
@EnableDiscoveryClient
@MapperScan("com.lmarket.product.dao")
@SpringBootApplication
public class LmarketProductApplication {

	public static void main(String[] args) {
		SpringApplication.run(LmarketProductApplication.class, args);
	}

}
