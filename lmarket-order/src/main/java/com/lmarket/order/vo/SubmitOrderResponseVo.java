package com.lmarket.order.vo;

import com.lmarket.order.entity.OrderEntity;
import lombok.Data;

@Data
public class SubmitOrderResponseVo {

    private OrderEntity order;
    private Integer code; //错误状态码 0-成功
}
