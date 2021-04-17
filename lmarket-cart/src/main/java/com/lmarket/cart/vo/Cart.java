package com.lmarket.cart.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 整个购物车
 * 需要计算的属性，需要重写get()方法，保证每次获取属性都会进行计算
 */
public class Cart {

    List<CartItem> items;

    private Integer countNum; //商品数量

    private Integer countType; //商品类型数量

    private BigDecimal totalAmount; //商品总价

    private BigDecimal reduce = new BigDecimal("0.00"); //减免价格;

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    }

    public Integer getCountNum() {
        int countNum = 0;
        if (items != null && items.size() > 0) {
            for (CartItem item : items) {
                countNum += item.getCount();
            }
        }
        return countNum;
    }

    public Integer getCountType() {
        int countType = 0;
        if (items != null && items.size() > 0) {
            for (CartItem item : items) {
                countType += 1;
            }
        }
        return countType;
    }

    public BigDecimal getTotalAmount() {
        BigDecimal totalAmount = new BigDecimal("0");
        //1、计算购物项总价
        if (items != null && items.size() > 0) {
            for (CartItem item : items) {
                if (item.getCheck()) {
                    BigDecimal totalPrice = item.getTotalPrice();
                    totalAmount = totalAmount.add(totalPrice);
                }
            }
        }

        //2、减去优惠总价
        BigDecimal subtract = totalAmount.subtract(getReduce());
        return subtract;
    }

    public BigDecimal getReduce() {
        return reduce;
    }

    public void setReduce(BigDecimal reduce) {
        this.reduce = reduce;
    }
}
