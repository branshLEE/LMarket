package com.lmarket.order.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 封装订单提交的数据
 */
@Data
public class OrderSubmitVo {
    private Long addrId; //收货地址的id
    private Integer payType; //支付方式

    //去购物车获取数据，直接同步购物车的数据

    //TODO 优惠、发票

    private String orderToken; //防重令牌
    private BigDecimal payPrice; //应付价格 验价，即比对提交的总价格和购物车选中商品的总价格
    private String note; //订单备注

    //用户相关信息，直接去session取出登录的用户

}
