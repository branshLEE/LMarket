package com.lmarket.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.common.to.es.SkuEsModel;
import com.common.utils.R;
import com.lmarket.search.config.LmarketElasticSearchConfig;
import com.lmarket.search.constant.EsConstant;
import com.lmarket.search.feign.ProductFeignService;
import com.lmarket.search.service.LMSearchService;
import com.lmarket.search.vo.AttrResponseVo;
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
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LMSearchServiceImpl implements LMSearchService {

    @Autowired
    private RestHighLevelClient client;

    @Autowired
    ProductFeignService productFeignService;

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
            result = buildSearchResult(response, param);
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
    private SearchResult buildSearchResult(SearchResponse response, SearchParam param) {

        SearchResult result = new SearchResult();
        //1、返回的所有查询到的商品
        SearchHits hits = response.getHits();

        List<SkuEsModel> esModels = new ArrayList<>();

        if(hits.getHits() != null && hits.getHits().length > 0){
            for (SearchHit hit : hits.getHits()) {
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel esModel = JSON.parseObject(sourceAsString, SkuEsModel.class);

                //设置标题高亮
                if(!StringUtils.isEmpty(param.getKeyword())){
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    String s = skuTitle.getFragments()[0].toString();
                    esModel.setSkuTitle(s);
                }

                esModels.add(esModel);
            }
        }
        result.setProducts(esModels);

        //2、当前所有商品涉及到的所有属性信息
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();
        ParsedNested attr_agg = response.getAggregations().get("attr_agg");

        ParsedLongTerms attr_id_agg = attr_agg.getAggregations().get("attr_id_agg");
        for (Terms.Bucket bucket : attr_id_agg.getBuckets()) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            //1、得到属性的id
            long attrId = bucket.getKeyAsNumber().longValue();


            //2、得到属性的名字
            String attrName = ((ParsedStringTerms) bucket.getAggregations().get("attr_name_agg")).getBuckets().get(0).getKeyAsString();

            //3、得到属性的所有值
            List<String> attrValues = ((ParsedStringTerms) bucket.getAggregations().get("attr_value_agg")).getBuckets().stream().map(item -> {
                String keyAsString = item.getKeyAsString();
                return keyAsString;
            }).collect(Collectors.toList());

            attrVo.setAttrId(attrId);
            attrVo.setAttrName(attrName);
            attrVo.setAttrValue(attrValues);
            attrVos.add(attrVo);

        }

        result.setAttrs(attrVos);

        //3、当前所有商品涉及到的所有品牌信息
        List<SearchResult.BrandVo> brandVos = new ArrayList<>();
        ParsedLongTerms brand_agg = response.getAggregations().get("brand_agg");
        for (Terms.Bucket bucket : brand_agg.getBuckets()) {
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            //1、得到品牌的id
            long brandId = bucket.getKeyAsNumber().longValue();

            //2、得到品牌的名字
            String brandName = ((ParsedStringTerms) bucket.getAggregations().get("brand_name_agg")).getBuckets().get(0).getKeyAsString();

            //3、得到品牌的图片
            String brandImg = ((ParsedStringTerms) bucket.getAggregations().get("brand_img_agg")).getBuckets().get(0).getKeyAsString();

            brandVo.setBrandId(brandId);
            brandVo.setBrandName(brandName);
            brandVo.setBrandImg(brandImg);
            brandVos.add(brandVo);
        }

        result.setBrands(brandVos);

        //4、当前所有商品涉及到的所有分类信息
        ParsedLongTerms catelog_agg = response.getAggregations().get("catelog_agg");

        List<SearchResult.CatelogVo> catelogVos = new ArrayList<>();
        List<? extends Terms.Bucket> buckets = catelog_agg.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            SearchResult.CatelogVo catelogVo = new SearchResult.CatelogVo();
            //得到分类id
            String keyAsString = bucket.getKeyAsString();
            catelogVo.setCatelogId(Long.parseLong(keyAsString));

            //得到分类名
            ParsedStringTerms catelog_name_agg = bucket.getAggregations().get("catelog_name_agg");
            String catelog_name = catelog_name_agg.getBuckets().get(0).getKeyAsString();
            catelogVo.setCatelogName(catelog_name);
            catelogVos.add(catelogVo);
        }
        result.setCatelogs(catelogVos);

        //5、分页信息-页码
        result.setPageNum(param.getPageNum());

        //6、分页信息-总记录数
        long total = hits.getTotalHits().value;
        result.setTotal(total);


        //7、分页信息-总页码
        Integer totalPages = Math.toIntExact(total % EsConstant.PRODUCT_PAGESIZE == 0 ? total / EsConstant.PRODUCT_PAGESIZE : (total / EsConstant.PRODUCT_PAGESIZE + 1));
        result.setTotalPages(totalPages);

        List<Integer> pageNavs = new ArrayList<>();
        for(int i=1; i<=totalPages; i++){
            pageNavs.add(i);
        }
        result.setPageNavs(pageNavs);


        //8、构建面包屑导航功能
        if(param.getAttrs() != null && param.getAttrs().size() > 0){

            List<SearchResult.NavVo> collect = param.getAttrs().stream().map(attr -> {
                //1、分析每个attr传过来的查询参数值
                SearchResult.NavVo navVo = new SearchResult.NavVo();

                //attrs=2_5寸:6寸
                String[] s = attr.split("_");
                navVo.setNavValue(s[1]);
                R r = productFeignService.attrInfo(Long.parseLong(s[0]));

                //拿到被用到的attrId
                result.getAttrIds().add(Long.parseLong(s[0]));

                if(r.getCode() == 0){
                    TypeReference<AttrResponseVo> reference = new TypeReference<AttrResponseVo>() {
                    };
                    AttrResponseVo data = r.getData("attr", reference);
                    String name = data.getAttrName();
                    navVo.setNavName(name);
                }else{
                    navVo.setNavName(s[0]);
                }

                //2、取消了这个面包屑以后，我们要跳转到那个地方，将请求地址的url里面的当前置空
                //拿到所有的查询条件，去掉当前
                String replace = replaceQueryString(param, attr, "attrs");
                navVo.setLink("http://search.lmarket.market/list.html?"+replace);

                return navVo;
            }).collect(Collectors.toList());

            result.setNavs(collect);
        }

        //品牌，分类
        if(param.getBrandId() != null && param.getBrandId().size() > 0){
            List<SearchResult.NavVo> navs = result.getNavs();
            SearchResult.NavVo navVo = new SearchResult.NavVo();
            navVo.setNavName("品牌");
            //TODO 远程查询所有品牌
            R r = productFeignService.brandInfo(param.getBrandId());
            if(r.getCode() == 0){
                List<SearchResult.BrandVo> brand = r.getData("brand", new TypeReference<List<SearchResult.BrandVo>>() {
                });
                StringBuffer stringBuffer = new StringBuffer();
                String replace = "";
                for (SearchResult.BrandVo brandVo : brand) {
                    stringBuffer.append(brandVo.getBrandName()+";");
                    replace = replaceQueryString(param, brandVo.getBrandId()+"", "brandId");
                }
                navVo.setNavValue(stringBuffer.toString());
                navVo.setLink("http://search.lmarket.market/list.html?"+replace);
            }

            navs.add(navVo);
        }

        //TODO 分类，不需要导航取消

        return result;


    }

    private String replaceQueryString(SearchParam param, String value, String key) {
        String encode = null;
        try {
            encode = URLEncoder.encode(value, "UTF-8");
            encode = encode.replace("+", "%20"); //浏览器对空格编码和Java不一样
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String replace = param.get_queryString().replace("&"+key+"=" + encode, "");
        return replace;
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
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        //3.3 聚合分析出当前attr_id对应的所有可能的属性值attrValue
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));
        attr_agg.subAggregation(attr_id_agg);
        sourceBuilder.aggregation(attr_agg);


        String s = sourceBuilder.toString();
        System.out.println("构建的DSL"+s);

        SearchRequest searchRequest = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, sourceBuilder);

        return searchRequest;
    }
}
