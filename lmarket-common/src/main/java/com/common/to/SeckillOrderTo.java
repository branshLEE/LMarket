package com.common.to;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class SeckillOrderTo {
    /**
     * 订单号
     */
    private String orderSn;
    /**
     * 活动场次Id
     */
    private Long promotionSessionId;
    /**
     * 商品id
     */
    private Long skuId;

    /**
     * 秒杀价格
     */
    private BigDecimal seckillPrice;
    /**
     * 抢购数量
     */
    private Integer num;
    /**
     * 会员id
     */
    private Long memberId;
    /**
     *  sku图片
     */
    private String skuDefaultImg;

}
