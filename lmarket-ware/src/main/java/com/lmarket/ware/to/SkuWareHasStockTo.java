package com.lmarket.ware.to;

import lombok.Data;

import java.util.List;

@Data
public class SkuWareHasStockTo {
    private Long skuId;
    private Integer num;
    private List<Long> wareId;
}
