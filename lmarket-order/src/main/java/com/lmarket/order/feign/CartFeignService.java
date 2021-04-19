package com.lmarket.order.feign;

import com.lmarket.order.vo.OrderItemVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient("lmarket-cart")
public interface CartFeignService {
    @GetMapping("/currentUserCartItems")
    public List<OrderItemVo> getCurrentUserCartItems();
}
