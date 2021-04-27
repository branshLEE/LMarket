package com.lmarket.product.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SeckillInfoVo {
    private Long id;
    /**
     *
     */
    private Long promotionId;
    /**
     *
     */
    private Long promotionSessionId;
    /**
     *
     */
    private Long skuId;
    /**
     * 商品秒杀随机码
     */
    private String randomCode;

    /**
     *
     */
    private BigDecimal seckillPrice;
    /**
     *
     */
    private BigDecimal seckillCount;
    /**
     * ÿ
     */
    private BigDecimal seckillLimit;
    /**
     *
     */
    private Integer seckillSort;

    //当前商品秒杀的开始时间和结束时间
    private Long startTime;
    private Long endTime;
}
