package com.lmarket.search.service.impl;

import com.lmarket.search.config.LmarketElasticSearchConfig;
import com.lmarket.search.constant.EsConstant;
import com.lmarket.search.service.LMSearchService;
import com.lmarket.search.vo.SearchParam;
import com.lmarket.search.vo.SearchResult;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class LMSearchServiceImpl implements LMSearchService {

    @Autowired
    private RestHighLevelClient client;

    //去es检索
    @Override
    public SearchResult search(SearchParam param) {
        //动态构建出查询需要的DSL
        SearchResult result = null;

        //1、准备检索请求
        SearchRequest searchRequest = buildSearchRequest(param);

        try {
            //2、执行检索请求
            SearchResponse response = client.search(searchRequest, LmarketElasticSearchConfig.COMMON_OPTIONS);

            //3、分析响应数据封装成需要的格式
            result = buildSearchResult(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 构建结果数据
     * @param response
     * @return
     */
    private SearchResult buildSearchResult(SearchResponse response) {

        return null;
    }


    /**
     * 准备检索请求
     * 模糊匹配，过滤（按属性，分类，品牌，价格区间，库存），排序，分页，高亮，聚合分析
     * @return
     */
    private SearchRequest buildSearchRequest(SearchParam param) {

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder(); //构建DSL语句

        /**
         * 模糊匹配，过滤（按属性，分类，品牌，价格区间，库存）
         */
        //1、构建bool-query
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        sourceBuilder.query(boolQueryBuilder);

        //1.1 must-模糊匹配
        if(!StringUtils.isEmpty(param.getKeyword())){
            boolQueryBuilder.must(QueryBuilders.matchQuery("skuTitle", param.getKeyword()));
        }
        //1.2 bool -filter 按照三级分类Id查询
        if(param.getCatelog3Id() != null){

            boolQueryBuilder.filter(QueryBuilders.termQuery("catelogId", param.getCatelog3Id()));
        }
        //1.2 按照品牌id查询
        if(param.getBrandId() != null && param.getBrandId().size() > 0){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId", param.getBrandId()));
        }
        //1.2 按照属性进行查询
        if(param.getAttrs() != null && param.getAttrs().size() > 0){


            //attrs=1_5寸:8寸&attrs=2_16G:8G
            for (String attr : param.getAttrs()) {
                BoolQueryBuilder nestedboolbuilder = QueryBuilders.boolQuery();

                String[] s = attr.split("_");
                String attrId = s[0]; //检索的属性id
                String[] attrValues = s[1].split(":"); //该属性的检索的值
                nestedboolbuilder.must(QueryBuilders.termsQuery("attrs.attrId", attrId));
                nestedboolbuilder.must(QueryBuilders.termsQuery("attrs.attrValue", attrValues));

                //每个s[]都得生成一个nested查询
                NestedQueryBuilder nestedQueryBuilder = QueryBuilders.nestedQuery("attrs", nestedboolbuilder, ScoreMode.None);
                boolQueryBuilder.filter(nestedQueryBuilder);
            }
        }

        //1.2 按照库存查询
        boolQueryBuilder.filter(QueryBuilders.termsQuery("hasStock", param.getHasStock() == 1));

        //1.2 按照价格区间查询 1_500/_500/500_
        if(!StringUtils.isEmpty(param.getSkuPrice())){
            RangeQueryBuilder skuPrice = QueryBuilders.rangeQuery("skuPrice");

            String[] s = param.getSkuPrice().split("_");
            if(s.length == 2){
                //区间
                skuPrice.gte(s[0]).lte(s[1]);
            }
            if(param.getSkuPrice().startsWith("_")){
                skuPrice.gte(null).lte(s[1]);
            }
            if(param.getSkuPrice().endsWith("_")){
                skuPrice.gte(s[0]);
            }

            boolQueryBuilder.filter(skuPrice);
        }

        //把之前所有的条件都进行封装
        sourceBuilder.query(boolQueryBuilder);

        /**
         * 排序，分页，高亮
         */
        //2.1 排序
        if(!StringUtils.isEmpty(param.getSort())){
            String sort = param.getSort();
            //sort=hostScore_asc/desc
            String[] s = sort.split("_");
            SortOrder order = s[1].equalsIgnoreCase("asc") ? SortOrder.ASC : SortOrder.DESC;
            sourceBuilder.sort(s[0], order);
        }
        //2.2 分页
        //from = (pageNum-1)*size
        sourceBuilder.from((param.getPageNum()-1)*EsConstant.PRODUCT_PAGESIZE);
        sourceBuilder.size(EsConstant.PRODUCT_PAGESIZE);

        //2.3 高亮
        if(!StringUtils.isEmpty(param.getKeyword())){
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("skuTitle");
            highlightBuilder.preTags("<b style='color:red'>");
            highlightBuilder.postTags("</b>");
            sourceBuilder.highlighter(highlightBuilder);
        }

        /**
         * 聚合分析
         */
        //1、品牌聚合
        TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg");
        brand_agg.field("brandId").size(50);

        //1.1 品牌聚合的子聚合
        brand_agg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        brand_agg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));

        sourceBuilder.aggregation(brand_agg);

        //2、分类聚合
        TermsAggregationBuilder catelog_agg = AggregationBuilders.terms("catelog_agg").field("catelogId").size(20);
        catelog_agg.subAggregation(AggregationBuilders.terms("catelog_name_agg").field("catelogName").size(1));
        sourceBuilder.aggregation(catelog_agg);

        //3、属性集合
        NestedAggregationBuilder attr_agg = AggregationBuilders.nested("attr_agg", "attrs");
        //3.1 聚合出当前所有的attrId
        TermsAggregationBuilder attr_id_agg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId");
        //3.2 聚合分析出当前attr_id对应的名字
        attr_agg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        //3.3 聚合分析出当前attr_id对应的所有可能的属性值attrValue
        attr_agg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));
        attr_agg.subAggregation(attr_id_agg);
        sourceBuilder.aggregation(attr_agg);


        String s = sourceBuilder.toString();
        System.out.println("构建的DSL"+s);

        SearchRequest searchRequest = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, sourceBuilder);

        return searchRequest;
    }
}
