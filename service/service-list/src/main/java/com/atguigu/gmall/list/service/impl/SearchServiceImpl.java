package com.atguigu.gmall.list.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.list.repository.GoodsRepository;
import com.atguigu.gmall.list.service.SearchService;
import com.atguigu.gmall.model.list.*;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author zr
 * @create 2020-03-23 下午 21:14
 */
@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    // 商品上架
    @Override
    public void upperGoods(Long skuId) {
        Goods goods = new Goods();
        /*
        开始给goods赋值
         */
        // 查询sku对应的平台属性和属性值
        List<BaseAttrInfo> baseAttrInfoList = productFeignClient.getAttrList(skuId);
        if (baseAttrInfoList != null) {
            // 循环获取数据给goods赋值
            List<SearchAttr> searchAttrList = baseAttrInfoList.stream().map(baseAttrInfo -> {
                // 平台属性对象
                SearchAttr searchAttr = new SearchAttr();
                searchAttr.setAttrId(baseAttrInfo.getId());
                searchAttr.setAttrName(baseAttrInfo.getAttrName());
                // 获取原始数据
                List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
                searchAttr.setAttrValue(attrValueList.get(0).getValueName());

                // 返回数据
                return searchAttr;
            }).collect(Collectors.toList());
            // 赋值
            goods.setAttrs(searchAttrList);
        }
        // skuInfo的基本信息
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        if (skuInfo != null) {
            goods.setDefaultImg(skuInfo.getSkuDefaultImg());
            goods.setPrice(skuInfo.getPrice().doubleValue());
            goods.setTitle(skuInfo.getSkuName());
            goods.setId(skuInfo.getId());
            goods.setCreateTime(new Date());

            // 品牌
            BaseTrademark trademark = productFeignClient.getTrademark(skuInfo.getTmId());
            if (trademark != null) {
                goods.setTmId(skuInfo.getTmId());
                goods.setTmName(trademark.getTmName());
                goods.setTmLogoUrl(trademark.getLogoUrl());
            }

            // 分类
            BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            if (categoryView != null) {
                goods.setCategory1Id(categoryView.getCategory1Id());
                goods.setCategory1Name(categoryView.getCategory1Name());
                goods.setCategory2Id(categoryView.getCategory2Id());
                goods.setCategory2Name(categoryView.getCategory2Name());
                goods.setCategory3Id(categoryView.getCategory3Id());
                goods.setCategory3Name(categoryView.getCategory3Name());
            }
        }
        // 将对象保存到es中
        goodsRepository.save(goods);

    }

    // 商品下架
    @Override
    public void lowerGoods(Long skuId) {
        goodsRepository.deleteById(skuId);
    }

    // 更新热点
    @Override
    public void incrHotScore(Long skuId) {
        // 定义key
        String hotSky = "hotScore";
        // 保存数据
        Double hotScore = redisTemplate.opsForZSet().incrementScore(hotSky, "skuId:" + skuId, 1);
        if (hotScore % 10 == 0) {
            Optional<Goods> optional = goodsRepository.findById(skuId);
            Goods goods = optional.get();
            goods.setHotScore(Math.round(hotScore));
            goodsRepository.save(goods);
        }
    }

    // 搜索列表
    @Override
    public SearchResponseVo search(SearchParam searchParam) throws IOException {
        // 构建dsl语句
        SearchRequest searchRequest = buildQueryDsl(searchParam);

        // 执行语句
        SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        // 返回结果集
        SearchResponseVo responseVo = parseSearchResult(response);

//        private Long total;//总记录数
//        private Integer pageSize;//每页显示的内容
//        private Integer pageNo;//当前页面
//        private Long totalPages;
        responseVo.setPageNo(searchParam.getPageNo());
        responseVo.setPageSize(searchParam.getPageSize());
        // 总页数
        long totalPages = (responseVo.getTotal() + searchParam.getPageSize() - 1) / searchParam.getPageSize();
        responseVo.setTotalPages(totalPages);

        return responseVo;
    }

    /**
     * 将es中执行的结果集，转化成封装好的SearchResponseVo
     *
     * @param response es 执行的结果集
     * @return
     */
    private SearchResponseVo parseSearchResult(SearchResponse response) {
        // 声明一个对象
        SearchResponseVo searchResponseVo = new SearchResponseVo();
        /*
        给对象中的属性赋值
        private List<SearchResponseTmVo> trademarkList;
        //所有商品的顶头显示的筛选属性
        private List<SearchResponseAttrVo> attrsList = new ArrayList<>();

        //检索出来的商品信息
        private List<Goods> goodsList = new ArrayList<>();

        private Long total;//总记录数
        private Integer pageSize;//每页显示的内容
        private Integer pageNo;//当前页面
        private Long totalPages;
         */
        // 给上面的属性赋值
        // 赋值品牌
        // 将聚合的数据先变成一个map集合
        Map<String, Aggregation> aggregationMap = response.getAggregations().asMap();
        // 获取品牌的Id的聚合数据 {Aggregation ---> ParsedLongTerms}
        ParsedLongTerms tmIdAgg = (ParsedLongTerms) aggregationMap.get("tmIdAgg");
        List<SearchResponseTmVo> trademarkList = tmIdAgg.getBuckets().stream().map(bucket -> {
            // 声明一个品牌对象
            SearchResponseTmVo searchResponseTmVo = new SearchResponseTmVo();
            // 给品牌Id赋值
            searchResponseTmVo.setTmId(Long.parseLong(((Terms.Bucket) bucket).getKeyAsString()));
            // 获取品牌的名称
            Map<String, Aggregation> tmIdSubMap = ((Terms.Bucket) bucket).getAggregations().asMap();
            // Aggregation --> ParsedStringTerms
            ParsedStringTerms tmNameAgg = (ParsedStringTerms) tmIdSubMap.get("tmNameAgg");
            // 获取到品牌的名称
            String tmName = tmNameAgg.getBuckets().get(0).getKeyAsString();
            // 赋值
            searchResponseTmVo.setTmName(tmName);
            // 获取品牌的logoUrl Aggregation -->ParsedStringTerms
            ParsedStringTerms tmLogoUrlAgg = (ParsedStringTerms) tmIdSubMap.get("tmLogoUrlAgg");
            // 获取logoUrl
            String tmLogoUrl = tmLogoUrlAgg.getBuckets().get(0).getKeyAsString();
            // 赋值
            searchResponseTmVo.setTmLogoUrl(tmLogoUrl);

            // 返回品牌对象
            return searchResponseTmVo;
        }).collect(Collectors.toList());

        // 赋值第一个品牌属性
        searchResponseVo.setTrademarkList(trademarkList);

        // 获取商品：
        // 声明一个集合对象来存储商品
        List<Goods> goodsList = new ArrayList<>();
        // 先需要获取到hits
        SearchHits hits = response.getHits();
        SearchHit[] subHits = hits.getHits();
        if (subHits != null && subHits.length > 0) {
            // 循环获取里面的数据
            for (SearchHit subHit : subHits) {
                // 将每一个hits中的source节点的数据对象转化为Goods
                // subHit.getSourceAsString() 获取source中所有数据
                Goods goods = JSONObject.parseObject(subHit.getSourceAsString(), Goods.class);
                // goods中的商品名称并不是高亮！真正的高亮在highlight中
                if (subHit.getHighlightFields().get("title") != null) {
                    // 说明有高亮的数据
                    Text title = subHit.getHighlightFields().get("title").getFragments()[0];
                    // 替换原来不是高亮的字段
                    goods.setTitle(title.toString());
                }
                // 将每个单独的商品添加到集合
                goodsList.add(goods);
            }
        }
        // 赋值商品集合
        searchResponseVo.setGoodsList(goodsList);

        // 平台属性赋值
        // 获取attrAgg 在编写dsl语句的时候，使用了nested类型来规定attrAgg中的数据
        ParsedNested attrAgg = (ParsedNested) aggregationMap.get("attrAgg");
        // 获取attrAgg下的attrIdAgg聚合数据
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attrIdAgg");
        // 获取attrIdAgg下面的数据
        List<? extends Terms.Bucket> buckets = attrIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(buckets)) {
            // 循环遍历
            List<SearchResponseAttrVo> responseAttrVoList = buckets.stream().map(bucket -> {
                // 返回的平台属性对象
                SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
                // 平台属性Id
                searchResponseAttrVo.setAttrId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());
                // 获取平台属性名 Aggregation -->ParsedStringTerms
                ParsedStringTerms attrNameAgg = ((Terms.Bucket) bucket).getAggregations().get("attrNameAgg");
                // 获取到了平台属性名的集合对象
                List<? extends Terms.Bucket> nameAggBuckets = attrNameAgg.getBuckets();
                // 赋值平台属性名称
                searchResponseAttrVo.setAttrName(nameAggBuckets.get(0).getKeyAsString());
                // 获取平台属性值数据 Aggregation -->ParsedStringTerms
                ParsedStringTerms attrValueAgg = ((Terms.Bucket) bucket).getAggregations().get("attrValueAgg");
                // 获取buckets中的数据
                List<? extends Terms.Bucket> valueAggBuckets = attrValueAgg.getBuckets();
                // 获取valueAggBuckets中的所有数据
                List<String> attrValueList = valueAggBuckets.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
                // 赋值平台属性值
                searchResponseAttrVo.setAttrValueList(attrValueList);

                return searchResponseAttrVo;
            }).collect(Collectors.toList());
            // 赋值整个的平台属性对象
            searchResponseVo.setAttrsList(responseAttrVoList);
        }
        // private Long total;//总记录数
        searchResponseVo.setTotal(hits.totalHits);

        return searchResponseVo;
    }

    // 编写dsl语句
    private SearchRequest buildQueryDsl(SearchParam searchParam) {
        // 构建一个查询器
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // 构建bool
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        // 条件判断 用户输入的是否是关键字
        if (!StringUtils.isEmpty(searchParam.getKeyword())) {
            MatchQueryBuilder title = QueryBuilders.matchQuery("title", searchParam.getKeyword()).operator(Operator.AND);
            boolQueryBuilder.must(title);
        }

        // 品牌检索
        String trademark = searchParam.getTrademark();
        // 判断品牌是否为空
        if (!StringUtils.isEmpty(trademark)) {
            // trademark=2:华为  tmId:tmName  StringUtils apache的。如果这个地方使用spring 那么分割失败！
            String[] split = StringUtils.split(trademark, ":");
            if (split != null && split.length == 2) {
                // term：表示精确匹配 id=？ terms：表示范围匹配 id in (?,?,?)
                boolQueryBuilder.filter(QueryBuilders.termsQuery("tmId", split[0]));
            }
        }

        // 分类id检索
        // 按照分类id检索的时候，一次只能传一个值，用term即可
        if (searchParam.getCategory1Id() != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("category1Id", searchParam.getCategory1Id()));
        }
        if (searchParam.getCategory2Id() != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("category2Id", searchParam.getCategory2Id()));
        }
        if (searchParam.getCategory3Id() != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("category3Id", searchParam.getCategory3Id()));
        }

        // 平台属性 props=23:4G:运行内存    平台属性Id：平台属性值：平台属性名
        String[] props = searchParam.getProps();
        if (props != null && props.length > 0) {
            // 循环遍历
            for (String prop : props) {
                String[] split = StringUtils.split(prop, ":");
                if (split != null && split.length == 3) {
                    // 构建嵌套查询
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    BoolQueryBuilder subBoolQuery = QueryBuilders.boolQuery();
                    // 构建根据平台属性id过滤
                    subBoolQuery.must(QueryBuilders.termQuery("attrs.attrId", split[0]));
                    // 根据平台属性值
                    subBoolQuery.must(QueryBuilders.termQuery("attrs.attrValue", split[1]));
                    // 将subBoolQuery 子查询结果作为独立的对象放入boolQuery
                    boolQuery.must(QueryBuilders.nestedQuery("attrs", subBoolQuery, ScoreMode.None));

                    // boolQuery查询结果赋值总的boolQueryBuilder
                    boolQueryBuilder.filter(boolQuery);
                }
            }
        }
        // 执行当前的query方法
        searchSourceBuilder.query(boolQueryBuilder);

        // 分页
        int from = (searchParam.getPageNo() - 1) * searchParam.getPageSize();
        searchSourceBuilder.from(from); // 表示从第几条数据开始查询
        searchSourceBuilder.size(searchParam.getPageSize());

        // 排序 1:hotScore 2:price    | 后续还会传入参数： price:asc price:desc
        String order = searchParam.getOrder();
        if (!StringUtils.isEmpty(order)) {
            String[] split = StringUtils.split(order, ":");
            if (split != null && split.length == 2) {
                // 声明一个字段
                String field = null;
                switch (split[0]) {
                    case "1":
                        field = "hotScore";
                        break;
                    case "2":
                        field = "price";
                        break;
                }
                // 在asc，desc之间选择
                searchSourceBuilder.sort(field, "asc".equals(split[1]) ? SortOrder.ASC : SortOrder.DESC);
            } else {
                // 如果传递的参数不符合if的要求
                searchSourceBuilder.sort("hotScore", SortOrder.DESC);
            }
        }

        // 高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title");
        highlightBuilder.preTags("<span style=color:red>");
        highlightBuilder.postTags("</span>");
        searchSourceBuilder.highlighter(highlightBuilder);

        // 聚合
        // 1. 聚合品牌
        TermsAggregationBuilder termsAggregationBuilder = AggregationBuilders.terms("tmIdAgg").field("tmId")
                .subAggregation(AggregationBuilders.terms("tmNameAgg").field("tmName"))
                .subAggregation(AggregationBuilders.terms("tmLogoUrlAgg").field("tmLogoUrl"));
        // 添加品牌Agg
        searchSourceBuilder.aggregation(termsAggregationBuilder);

        // 2. 聚合平台属性
        searchSourceBuilder.aggregation(AggregationBuilders.nested("attrAgg", "attrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName"))
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue"))));

        // 过滤结果集
        searchSourceBuilder.fetchSource(new String[]{"id", "defaultImg", "title", "price"}, null);

        // 查询数据 知道在哪个index，哪个type中查询
        SearchRequest searchRequest = new SearchRequest("goods");
        searchRequest.types("info");
        searchRequest.source(searchSourceBuilder);

        System.out.println("dsl:" + searchSourceBuilder.toString());

        // 返回查询请求对象
        return searchRequest;
    }
}
