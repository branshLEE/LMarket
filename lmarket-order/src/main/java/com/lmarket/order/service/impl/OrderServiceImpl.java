package com.lmarket.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.common.utils.R;
import com.common.vo.MemberResponseVo;
import com.lmarket.order.feign.CartFeignService;
import com.lmarket.order.feign.MemberFeignService;
import com.lmarket.order.feign.WmsFeignService;
import com.lmarket.order.interceptor.LoginUserInterceptor;
import com.lmarket.order.vo.MemberAddressVo;
import com.lmarket.order.vo.OrderConfirmVo;
import com.lmarket.order.vo.OrderItemVo;
import com.lmarket.order.vo.SkuStockVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.common.utils.PageUtils;
import com.common.utils.Query;

import com.lmarket.order.dao.OrderDao;
import com.lmarket.order.entity.OrderEntity;
import com.lmarket.order.service.OrderService;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    @Autowired
    MemberFeignService memberFeignService;

    @Autowired
    CartFeignService cartFeignService;

    @Autowired
    WmsFeignService wmsFeignService;

    @Autowired
    ThreadPoolExecutor executor;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        OrderConfirmVo confirmVo = new OrderConfirmVo();
        MemberResponseVo memberResponseVo = LoginUserInterceptor.localUser.get();

        //异步之前先获取请求
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        CompletableFuture<Void> getAddressFuture = CompletableFuture.runAsync(() -> {
            //1、远程查询所有的收货地址列表
            RequestContextHolder.setRequestAttributes(requestAttributes); //共享请求头

            List<MemberAddressVo> address = memberFeignService.getAddress(memberResponseVo.getMemberId());
            confirmVo.setAddress(address);
        }, executor);

        CompletableFuture<Void> getCartItemsFuture = CompletableFuture.runAsync(() -> {
            //2、远程查询购物车所有选中的购物项
            RequestContextHolder.setRequestAttributes(requestAttributes); //共享请求头

            List<OrderItemVo> items = cartFeignService.getCurrentUserCartItems();
            confirmVo.setItems(items);
        }, executor).thenRunAsync(()->{
            List<OrderItemVo> items = confirmVo.getItems();
            List<Long> collect = items.stream().map(item -> item.getSkuId()).collect(Collectors.toList());
            R hasStock = wmsFeignService.getSkusHasStock(collect);
            List<SkuStockVo> data = hasStock.getData(new TypeReference<List<SkuStockVo>>() {
            });
            if(data != null){
                Map<Long, Boolean> map = data.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                confirmVo.setStocks(map);
            }
        }, executor);


        //3、查询用户积分
        Integer integration = memberResponseVo.getIntegration();
        confirmVo.setIntegration(integration);

        //4、其他数据自动计算

        //TODO 5、防重令牌

        CompletableFuture.allOf(getAddressFuture, getCartItemsFuture).get();

        return confirmVo;
    }

}