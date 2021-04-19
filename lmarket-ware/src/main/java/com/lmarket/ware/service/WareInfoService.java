package com.lmarket.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.common.utils.PageUtils;
import com.lmarket.ware.entity.WareInfoEntity;
import com.lmarket.ware.vo.FareVo;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 
 *
 * @author branshlee
 * @email branshLEE@gmail.com
 * @date 2021-02-23 00:05:23
 */
public interface WareInfoService extends IService<WareInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 根据收货地址计算运费
     * @return
     */
    FareVo getFare(Long addrId);

}

