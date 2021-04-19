package com.lmarket.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.common.utils.PageUtils;
import com.lmarket.member.entity.MemberReceiveAddressEntity;

import java.util.List;
import java.util.Map;

/**
 * 
 *
 * @author branshlee
 * @email branshLEE@gmail.com
 * @date 2021-02-22 23:40:43
 */
public interface MemberReceiveAddressService extends IService<MemberReceiveAddressEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<MemberReceiveAddressEntity> getAddress(Long id);
}

