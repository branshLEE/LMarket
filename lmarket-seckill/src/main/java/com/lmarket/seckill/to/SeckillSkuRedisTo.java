package com.lmarket.seckill.to;

import com.lmarket.seckill.vo.SkuInfoVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class SeckillSkuRedisTo {
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
    private Integer seckillCount;
    /**
     * ÿ
     */
    private Integer seckillLimit;
    /**
     *
     */
    private Integer seckillSort;

    //当前商品秒杀的开始时间和结束时间
    private Long startTime;
    private Long endTime;

    //sku的详细信息
    private SkuInfoVo skuInfo;
}
