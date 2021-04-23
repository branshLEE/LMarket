package com.common.to.mq;

import lombok.Data;

@Data
public class StockDetailTo {

    private Long id;
    /**
     * sku_id
     */
    private Long skuId;
    /**
     * sku_name
     */
    private String skuName;
    /**
     *
     */
    private Integer skuNum;
    /**
     *
     */
    private Long taskId;

    private Long wareId;

    private Integer lockStatus;
}
