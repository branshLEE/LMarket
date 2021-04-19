package com.lmarket.cart.controller;

import com.common.constant.AuthServerConstant;
import com.lmarket.cart.To.UserInfoTo;
import com.lmarket.cart.interceptor.CartInterceptor;
import com.lmarket.cart.service.CartService;
import com.lmarket.cart.vo.Cart;
import com.lmarket.cart.vo.CartItem;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Controller
public class CartController {

    @Autowired
    CartService cartService;

    @GetMapping("/currentUserCartItems")
    @ResponseBody
    public List<CartItem> getCurrentUserCartItems(){

        return cartService.getUserCartItems();
    }

    @GetMapping("/deleteItem")
    public String deleteItem(@RequestParam("skuId") Long skuId){
        cartService.deleteItem(skuId);

        return "redirect:http://cart.lmarket.com/cart.html";
    }

    @GetMapping("/countItem")
    public String countItem(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num){
        cartService.changeItemCount(skuId, num);

        return "redirect:http://cart.lmarket.com/cart.html";
    }

    @GetMapping("/checkItem")
    public String checkItem(@RequestParam("skuId") Long skuId, @RequestParam("check") Integer check){
        cartService.checkItem(skuId, check);

        return "redirect:http://cart.lmarket.com/cart.html";
    }

    /**
     * 跳转到购物车页面
     * 浏览器有一个cookie: user-key 用于表示用户身份，一个月后过期
     * @return
     */
    @GetMapping("/cart.html")
    public String cartListPage(Model model) throws ExecutionException, InterruptedException {

        //1、快速得到用户信息，id、user-key
//        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
//        System.out.println("userInfoTo............."+userInfoTo);

        //获取购物车信息
        Cart cart = cartService.getCart(model);
        model.addAttribute("cart", cart);

        return "cartList";
    }

    /**
     * 添加商品到购物车
     * @return
     */
    @GetMapping("/addToCart")
    public String addToCart(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num, RedirectAttributes ra) throws ExecutionException, InterruptedException {

        cartService.addToCart(skuId, num);
        ra.addAttribute("skuId", skuId);
        return "redirect:http://cart.lmarket.com/addToCartSuccess.html";
    }

    /**
     * 跳转到成功页
     * @param skuId
     * @param model
     * @return
     */
    @GetMapping("/addToCartSuccess.html")
    public String addToCartSuccessPage(@RequestParam("skuId") Long skuId, Model model){
        //重定向到成功添加购物车页面，再次查询购物车数据即可
        CartItem cartItem = cartService.getCartItem(skuId);
        model.addAttribute("item", cartItem);

        return "success";
    }
}
