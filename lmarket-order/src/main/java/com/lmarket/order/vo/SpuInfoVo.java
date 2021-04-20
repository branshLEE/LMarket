package com.lmarket.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class SpuInfoVo {
    private Long id;
    /**
     *
     */
    private String spuName;
    /**
     *
     */
    private String spuDescription;
    /**
     *
     */
    private Long catelogId;
    /**
     * Ʒ
     */
    private Long brandId;
    /**
     *
     */
    private BigDecimal weight;
    /**
     *
     */
    private Integer publishStatus;
    /**
     *
     */
    private Date createTime;
    /**
     *
     */
    private Date updateTime;
}
