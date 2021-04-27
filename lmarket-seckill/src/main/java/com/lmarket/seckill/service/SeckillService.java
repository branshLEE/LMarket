package com.lmarket.seckill.service;

import com.lmarket.seckill.to.SeckillSkuRedisTo;
import com.lmarket.seckill.vo.SeckillSkuVo;

import java.util.List;

public interface SeckillService {

    void uploadSeckillSkuLatest3Days();

    List<SeckillSkuRedisTo> getCurrentSeckillSkus();

    SeckillSkuRedisTo getSkuSeckillInfo(Long skuId);
}
