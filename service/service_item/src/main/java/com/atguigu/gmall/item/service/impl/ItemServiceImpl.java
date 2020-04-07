package com.atguigu.gmall.item.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.product.BaseCategoryView;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author zr
 * @create 2020-03-17 下午 19:19
 */
@Service
public class ItemServiceImpl implements ItemService {

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private ListFeignClient listFeignClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    //获取sku详情信息
    @Override
    public Map<String, Object> getBySkuId(Long skuId) {
        // 构建返回map
        Map<String, Object> result = new HashMap<>();
        // 重构代码，异步编排
        // 需要有返回值，获取skuInfo数据
        CompletableFuture<SkuInfo> skuInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            result.put("skuInfo", skuInfo);
            return skuInfo;
        }, threadPoolExecutor);

        // 通过三级分类id查询分类信息
        // 接收任务的处理结果，并消费处理，无返回结果
        CompletableFuture<Void> categoryViewCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            result.put("categoryView", categoryView);
        }, threadPoolExecutor);

        // 获取sku价格
        CompletableFuture<Void> skuPriceCompletableFuture = CompletableFuture.runAsync(() -> {
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            result.put("price", skuPrice);
        }, threadPoolExecutor);

        // 也可以通过skuInfoCompletableFuture获取价格
//        CompletableFuture<Void> skuPriceCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
//            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuInfo.getId());
//            result.put("price", skuPrice);
//        }, threadPoolExecutor);

        // 根据spuId，skuId 查询销售属性集合
        CompletableFuture<Void> spuSaleAttrCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            List<SpuSaleAttr> spuSaleAttrList = productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());
            result.put("spuSaleAttrList", spuSaleAttrList);
        }, threadPoolExecutor);

        // 根据spuId 查询map 集合属性
        CompletableFuture<Void> skuValueIdsMapCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            // 根据spuId 查询map 集合属性
            Map skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
            // 将map集合转换为json字符串保存
            String valuesSkuJson = JSON.toJSONString(skuValueIdsMap); //valuesSkuJson = "{"16|19":15,"15|19":14}"
            result.put("valuesSkuJson", valuesSkuJson);
        }, threadPoolExecutor);

        // 更新商品 incrHotScore
        CompletableFuture<Void> incrHotScoreCompletableFuture = CompletableFuture.runAsync(() -> {
            listFeignClient.incrHotScore(skuId);
        }, threadPoolExecutor);

        // 将所有线程进行汇总
        // allOf 后面加join
        CompletableFuture.allOf(skuInfoCompletableFuture,
                categoryViewCompletableFuture,
                skuPriceCompletableFuture,
                spuSaleAttrCompletableFuture,
                skuValueIdsMapCompletableFuture,
                incrHotScoreCompletableFuture).join();

//        //根据skuId获取skuInfo
//        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
//        //通过三级分类id查询分类信息
//        BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
//        //获取sku价格
//        BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
//        //根据spuId，skuId 查询销售属性集合
//        List<SpuSaleAttr> spuSaleAttrList = productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());
//        //根据spuId 查询map 集合属性
//        Map skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
//        //将map集合转换为json字符串保存
//        String valuesSkuJson = JSON.toJSONString(skuValueIdsMap); //valuesSkuJson = "{"16|19":15,"15|19":14}"
//
//        //保存数据
//        result.put("skuInfo", skuInfo);
//        result.put("categoryView", categoryView);
//        result.put("price", skuPrice);
//        result.put("spuSaleAttrList", spuSaleAttrList);
//        result.put("valuesSkuJson", valuesSkuJson);
        return result;
    }
}
