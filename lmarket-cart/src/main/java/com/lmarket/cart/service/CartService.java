package com.lmarket.cart.service;

import com.lmarket.cart.vo.Cart;
import com.lmarket.cart.vo.CartItem;
import org.springframework.ui.Model;

import java.util.concurrent.ExecutionException;

public interface CartService {

    /**
     * 将商品添加到购物车
     * @param skuId
     * @param num
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;

    /**
     * 获取购物车中的某个购物项
     * @param skuId
     * @return
     */
    CartItem getCartItem(Long skuId);

    /**
     * 获取整个购物车数据
     * @param model
     * @return
     */
    Cart getCart(Model model) throws ExecutionException, InterruptedException;

    /**
     * 清空购物车数据
     * @param cartKey
     */
    void clearCart(String cartKey);

    /**
     * 勾选购物项
     * @param skuId
     * @param check
     */
    void checkItem(Long skuId, Integer check);

    /**
     * 修改购物项数量
     * @param skuId
     * @param num
     */
    void changeItemCount(Long skuId, Integer num);

    /**
     * 删除购物项
     * @param skuId
     */
    void deleteItem(Long skuId);
}
