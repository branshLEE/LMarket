package com.lmarket.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.common.utils.PageUtils;
import com.lmarket.product.entity.SkuInfoEntity;

import java.util.Map;

/**
 * sku
 *
 * @author branshlee
 * @email branshLEE@gmail.com
 * @date 2021-02-22 13:31:35
 */
public interface SkuInfoService extends IService<SkuInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

