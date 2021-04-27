package com.lmarket.seckill.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class SeckillSessionsWithSkus {
    private Long id;
    /**
     *
     */
    private String name;
    /**
     * ÿ
     */
    private Date startTime;
    /**
     * ÿ
     */
    private Date endTime;
    /**
     *
     */
    private Integer status;
    /**
     *
     */
    private Date createTime;

    private List<SeckillSkuVo> relationEntityList;
}
