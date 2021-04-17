package com.lmarket.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.common.utils.R;
import com.lmarket.cart.To.UserInfoTo;
import com.lmarket.cart.fegin.ProductFeignService;
import com.lmarket.cart.interceptor.CartInterceptor;
import com.lmarket.cart.service.CartService;
import com.lmarket.cart.vo.Cart;
import com.lmarket.cart.vo.CartItem;
import com.lmarket.cart.vo.SkuInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    ThreadPoolExecutor executor;

    private final String CART_PREFIX = "lmarket:cart:";

    @Override
    public CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();



        String res = (String) cartOps.get(skuId.toString());
        if(StringUtils.isEmpty(res)){
            //购物车无此商品


            //2、添加新商品到购物车
            CartItem cartItem = new CartItem();

            CompletableFuture<Void> getSkuInfoTask = CompletableFuture.runAsync(() -> {
                //1、远程查询当前要添加的商品信息
                R skuInfo = productFeignService.getSkuInfo(skuId);
                SkuInfoVo data = skuInfo.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                });


                cartItem.setCheck(true);
                cartItem.setCount(num);
                cartItem.setImages(data.getSkuDefaultImg());
                cartItem.setTitle(data.getSkuTitle());
                cartItem.setSkuId(data.getSkuId());
                cartItem.setPrice(data.getPrice());
            }, executor);


            CompletableFuture<Void> getSkuSaleAttrVAlues = CompletableFuture.runAsync(() -> {
                //3、远程查询sku的组合信息
                List<String> values = productFeignService.getSkuSaleAttrValues(skuId);
                cartItem.setSkuAttr(values);
            }, executor);

            CompletableFuture.allOf(getSkuInfoTask, getSkuSaleAttrVAlues).get();
            String s = JSON.toJSONString(cartItem);
            cartOps.put(skuId.toString(), s);

            return cartItem;

        }
        else{
            //购物车有此商品，修改数量即可

            CartItem cartItem = JSON.parseObject(res, CartItem.class);
            cartItem.setCount(cartItem.getCount()+num);

            cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));

            return cartItem;

        }

    }

    @Override
    public CartItem getCartItem(Long skuId) {

        BoundHashOperations<String, Object, Object> cartOps = getCartOps();

        String  o = (String) cartOps.get(skuId.toString());
        CartItem cartItem = JSON.parseObject(o, CartItem.class);
        return cartItem;
    }

    @Override
    public Cart getCart(Model model) throws ExecutionException, InterruptedException {

        Cart cart = new Cart();
        //1、区分页面是否登录状态
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        if(userInfoTo.getUserId() != null){
            //登录
            String cartKey = CART_PREFIX + userInfoTo.getUserId();
            BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(cartKey);
            //如果临时购物车的数据没有合并
            List<CartItem> tempCartItems = getCartItems(CART_PREFIX + userInfoTo.getUserKey());
            if(tempCartItems != null){
                //临时购物车有购物项，需要合并到在线购物车
                for (CartItem item : tempCartItems) {
                    addToCart(item.getSkuId(), item.getCount());
                }

                //清除临时购物车的数据
                clearCart(CART_PREFIX + userInfoTo.getUserKey());
            }

            //获取登录后的购物车的数据
            List<CartItem> cartItems = getCartItems(cartKey);
            cart.setItems(cartItems);

        }else{
            String cartKey = CART_PREFIX + userInfoTo.getUserKey();

            //获取临时购物车的所有购物项
            List<CartItem> cartItems = getCartItems(cartKey);
            cart.setItems(cartItems);
        }

        return cart;
    }

    /**
     * 获取到我们要操作的购物车
     * @return
     */
    private BoundHashOperations<String, Object, Object> getCartOps() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();

        String cartKey = "";
        if (userInfoTo.getUserId() != null) {
            cartKey = CART_PREFIX + userInfoTo.getUserId();
        } else {
            cartKey = CART_PREFIX + userInfoTo.getUserKey();
        }

        BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(cartKey);

        return operations;
    }

    private List<CartItem> getCartItems(String cartKey){

        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(cartKey);
        List<Object> values = hashOps.values();
        if(values != null && values.size() > 0){
            List<CartItem> collect = values.stream().map(obj -> {
                String str = (String) obj;
                CartItem cartItem = JSON.parseObject(str, CartItem.class);
                return cartItem;
            }).collect(Collectors.toList());

            return collect;
        }
        return null;
    }

    @Override
    public void clearCart(String cartKey){
        redisTemplate.delete(cartKey);
    }

    @Override
    public void checkItem(Long skuId, Integer check) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        CartItem cartItem = getCartItem(skuId);
        cartItem.setCheck(check == 1 ? true:false);
        String s = JSON.toJSONString(cartItem);
        cartOps.put(skuId.toString(), s);
    }

    @Override
    public void changeItemCount(Long skuId, Integer num) {
        CartItem cartItem = getCartItem(skuId);
        cartItem.setCount(num);

        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
    }

    @Override
    public void deleteItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.delete(skuId.toString());
    }
}
