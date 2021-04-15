package com.lmarket.cart.controller;

import com.common.constant.AuthServerConstant;
import com.lmarket.cart.To.UserInfoTo;
import com.lmarket.cart.interceptor.CartInterceptor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;

@Controller
public class CartController {

    /**
     * 跳转到购物车页面
     * 浏览器有一个cookie: user-key 用于表示用户身份，一个月后过期
     * @return
     */
    @GetMapping("/cart.html")
    public String cartListPage(){

        //1、快速得到用户信息，id、user-key
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        System.out.println("userInfoTo............."+userInfoTo);

        return "cartList";
    }
}
