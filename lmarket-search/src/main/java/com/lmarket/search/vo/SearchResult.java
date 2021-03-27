package com.lmarket.search.vo;

import com.common.to.es.SkuEsModel;
import lombok.Data;

import java.util.List;

@Data
public class SearchResult {

    //查询到的所有商品信息
    private List<SkuEsModel> product;

    /**
     * 分页信息
     */
    private Integer pageNum; //当前页码
    private Long total; //总记录数
    private Integer totalPages; //总页码

    private List<BrandVo> brands; //当前查询到的结果，所有涉及到的品牌
    private List<CatelogVo> catelogs; //当前查询到的结果，所有涉及到的分类
    private List<AttrVo> attrs; //当前查询到的结果，所有涉及到的属性

    //==========================以上返回给页面所有的信息==============================


    @Data
    public static class BrandVo{
        private Long brandId;
        private String brandName;
        private String brandImg;
    }

    @Data
    public static class CatelogVo{
        private Long catelogId;
        private String catelogName;
    }

    @Data
    public static class AttrVo{
        private Long attrId;
        private String attrName;
        private List<String> attrValue;
    }
}