package com.lmarket.member;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootTest
class LmarketMemberApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void testPasswordEncoder(){
		BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

		//$2a$10$ZzcDytlUEGDeP64PTt5oGeYJcJSnYIm7DfPpc.enpmtO0dUY2egiG
		//$2a$10$gZ.cSfr9kZh67zv0RNSUHeGGTmeXWMOaRxbcjR3NnWQGxtIbnAYsy
		String encode = passwordEncoder.encode("1311");
		boolean matches = passwordEncoder.matches("1311", "$2a$10$ZzcDytlUEGDeP64PTt5oGeYJcJSnYIm7DfPpc.enpmtO0dUY2egiG");

		System.out.println(encode+"=>"+matches);
	}
}
