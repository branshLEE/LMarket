package com.lmarket.order.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Configuration
public class LMFeignConfig {

    @Bean("requestInterceptor")
    public RequestInterceptor requestInterceptor(){
        return new RequestInterceptor(){

            @Override
            public void apply(RequestTemplate requestTemplate) {
                //1、通过RequestContextHolder拿到刚进来的请求数据
                ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                HttpServletRequest request = requestAttributes.getRequest(); //老请求

                if(request != null){
                    //同步请求头数据，-》cookie
                    String cookie = request.getHeader("Cookie"); //从老请求获取cookie
                    requestTemplate.header("Cookie", cookie); //新请求拿到老请求的cookie，相当于同步cookie
                }
            }
        };
    }
}
