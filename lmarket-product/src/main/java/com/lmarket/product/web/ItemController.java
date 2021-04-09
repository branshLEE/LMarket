package com.lmarket.product.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ItemController {

    /**
     * 展示sku的详情
     * @param skuId
     * @return
     */
    @GetMapping("/{skuId}.html")
    public String skuItem(Long skuId){

        System.out.println("准备查询"+skuId+"详情");
        return "item";
    }
}
