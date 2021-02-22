package com.lmarket.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.common.utils.PageUtils;
import com.lmarket.coupon.entity.CouponSpuRelationEntity;

import java.util.Map;

/**
 * 
 *
 * @author branshlee
 * @email branshLEE@gmail.com
 * @date 2021-02-22 23:14:56
 */
public interface CouponSpuRelationService extends IService<CouponSpuRelationEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

