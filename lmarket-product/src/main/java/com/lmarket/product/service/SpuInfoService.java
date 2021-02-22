package com.lmarket.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.common.utils.PageUtils;
import com.lmarket.product.entity.SpuInfoEntity;

import java.util.Map;

/**
 * spu
 *
 * @author branshlee
 * @email branshLEE@gmail.com
 * @date 2021-02-22 13:31:35
 */
public interface SpuInfoService extends IService<SpuInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

