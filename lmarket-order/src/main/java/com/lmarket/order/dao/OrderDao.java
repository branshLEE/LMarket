package com.lmarket.order.dao;

import com.lmarket.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author branshlee
 * @email branshLEE@gmail.com
 * @date 2021-02-22 23:56:18
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
	
}
