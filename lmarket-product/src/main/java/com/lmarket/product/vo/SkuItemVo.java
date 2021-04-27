package com.lmarket.product.vo;

import com.lmarket.product.entity.SkuImagesEntity;
import com.lmarket.product.entity.SkuInfoEntity;
import com.lmarket.product.entity.SpuInfoDescEntity;
import lombok.Data;

import java.util.List;

@Data
public class SkuItemVo {
    //1、sku基本信息获取 pms_sku_info
    SkuInfoEntity info;

    //2、sku图片信息 pms_sku_images
    List<SkuImagesEntity> images;

    //3、获取spu的销售属性组合
    List<SkuItemSaleAttrVo> saleAttr;

    //4、获取spu的介绍
    SpuInfoDescEntity desp;

    //5、获取spu的规格参数信息
    List<SpuItemAttrGroupVo> groupAttrs;

    boolean hasStock = true;

    SeckillInfoVo seckillInfo; //当前商品的秒杀优惠信息

}
