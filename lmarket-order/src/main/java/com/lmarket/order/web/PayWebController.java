package com.lmarket.order.web;

import com.alipay.api.AlipayApiException;
import com.lmarket.order.config.AlipayTemplate;
import com.lmarket.order.service.OrderService;
import com.lmarket.order.vo.PayVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class PayWebController {

    @Autowired
    AlipayTemplate alipayTemplate;

    @Autowired
    OrderService orderService;

    @ResponseBody
    @GetMapping(value = "/payOrder", produces = "text/html")
    public String payOrder(@RequestParam("orderSn") String orderSn) throws AlipayApiException {

        PayVo payVo = orderService.getOrderPay(orderSn);
        String pay = alipayTemplate.pay(payVo);
//        System.out.println(pay); //返回的pay是一个页面
        return pay;
    }
}
