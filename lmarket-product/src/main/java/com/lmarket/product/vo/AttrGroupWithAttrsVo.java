package com.lmarket.product.vo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.lmarket.product.entity.AttrEntity;
import lombok.Data;

import java.util.List;

@Data
public class AttrGroupWithAttrsVo {
    /**
     *
     */
    @TableId
    private Long attrGroupId;
    /**
     *
     */
    private String attrGroupName;
    /**
     *
     */
    private Integer sort;
    /**
     *
     */
    private String descript;
    /**
     *
     */
    private String icon;
    /**
     *
     */
    private Long catelogId;

    private List<AttrEntity> attrs;
}
