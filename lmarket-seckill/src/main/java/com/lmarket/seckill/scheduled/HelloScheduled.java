package com.lmarket.seckill.scheduled;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @EnableScheduling 开启定时任务
 * @EnableAsync //开启异步任务功能
 */
//@EnableScheduling
//@Component
//@EnableAsync
//@Slf4j
//public class HelloScheduled {
//
//    @Async
//    @Scheduled(cron = "* * * ? * 2")
//    public void hello() throws InterruptedException {
//        log.info("hello....");
//        Thread.sleep(3000);
//    }
//}
