package com.lmarket.order.vo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 订单确认页需要展示的数据
 */

public class OrderConfirmVo {

    //收货地址
    @Getter @Setter
    List<MemberAddressVo> address;

    //所有选中的购物项
    @Getter @Setter
    List<OrderItemVo> items;

    //发票信息

    //优惠券信息
    @Getter @Setter
    Integer integration;

    @Getter @Setter
    Map<Long, Boolean> stocks;

    @Getter @Setter
    String orderToken; //订单防重令牌

    BigDecimal total; //订单总额

    public Integer getCount(){
        Integer i = 0;
        if(items != null){
            for (OrderItemVo item : items) {
                i += item.getCount();
            }
        }
        return i;
    }

    public BigDecimal getTotal() {
        BigDecimal total = new BigDecimal("0");
        if(items != null){
            for (OrderItemVo item : items) {
                BigDecimal add = total.add(item.getPrice().multiply(new BigDecimal(item.getCount().toString())));
                total = total.add(add);
            }
        }
        return total;
    }

    BigDecimal payPrice; //应付价格

    public BigDecimal getPayPrice() {
        return getTotal();
    }
}
