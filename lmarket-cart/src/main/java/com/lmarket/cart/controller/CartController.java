package com.lmarket.cart.controller;

import com.common.constant.AuthServerConstant;
import com.lmarket.cart.To.UserInfoTo;
import com.lmarket.cart.interceptor.CartInterceptor;
import com.lmarket.cart.service.CartService;
import com.lmarket.cart.vo.CartItem;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.concurrent.ExecutionException;

@Controller
public class CartController {

    @Autowired
    CartService cartService;

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

    /**
     * 添加商品到购物车
     * @return
     */
    @GetMapping("/addToCart")
    public String addToCart(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num, Model model) throws ExecutionException, InterruptedException {

        CartItem cartItem = cartService.addToCart(skuId, num);
        model.addAttribute("item", cartItem);
        return "success";
    }
}
