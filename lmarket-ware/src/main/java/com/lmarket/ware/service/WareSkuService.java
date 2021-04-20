package com.lmarket.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.common.utils.PageUtils;
import com.lmarket.ware.entity.WareSkuEntity;
import com.lmarket.ware.vo.LockStockResult;
import com.lmarket.ware.vo.SkuHasStockVo;
import com.lmarket.ware.vo.WareSkuLockVo;

import java.util.List;
import java.util.Map;

/**
 * 
 *
 * @author branshlee
 * @email branshLEE@gmail.com
 * @date 2021-02-23 00:05:23
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void addStock(Long skuId, Long wareId, Integer skuNum);

    List<SkuHasStockVo> getSkusHasStock(List<Long> skuIds);

    Boolean orderLockStock(WareSkuLockVo vo);
}

