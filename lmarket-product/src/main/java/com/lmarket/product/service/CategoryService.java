package com.lmarket.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.common.utils.PageUtils;
import com.lmarket.product.entity.CategoryEntity;

import java.util.List;
import java.util.Map;

/**
 * 
 *
 * @author branshlee
 * @email branshLEE@gmail.com
 * @date 2021-02-22 13:31:35
 */
public interface CategoryService extends IService<CategoryEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<CategoryEntity> listWithTree();

    void removeMenusByIds(List<Long> asList);

}

