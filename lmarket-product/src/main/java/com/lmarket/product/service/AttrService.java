package com.lmarket.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.common.utils.PageUtils;
import com.lmarket.product.entity.AttrEntity;
import com.lmarket.product.vo.AttrRespVo;
import com.lmarket.product.vo.AttrVo;

import java.util.Map;

/**
 * 
 *
 * @author branshlee
 * @email branshLEE@gmail.com
 * @date 2021-02-22 13:31:35
 */
public interface AttrService extends IService<AttrEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveAttr(AttrVo attr);

    PageUtils queryBaseAttrPage(Map<String, Object> params, Long catelogId);

    AttrRespVo getAttrInfo(Long attrId);
}

