package com.lmarket.seckill.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SkuInfoVo {
    private Long skuId;
    /**
     * spuId
     */
    private Long spuId;
    /**
     * sku
     */
    private String skuName;
    /**
     * sku
     */
    private String skuDesc;
    /**
     *
     */
    private Long catelogId;
    /**
     * Ʒ
     */
    private Long brandId;
    /**
     * Ĭ
     */
    private String skuDefaultImg;
    /**
     *
     */
    private String skuTitle;
    /**
     *
     */
    private String skuSubtitle;
    /**
     *
     */
    private BigDecimal price;
    /**
     *
     */
    private Long saleCount;
}
