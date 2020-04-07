package com.atguigu.gmall.product.client;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.impl.ProductDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author zr
 * @create 2020-03-18 下午 12:23
 */
@FeignClient(name = "service-product", fallback = ProductDegradeFeignClient.class)
public interface ProductFeignClient {

    @GetMapping("api/product/inner/getSkuInfo/{skuId}")
    SkuInfo getSkuInfo(@PathVariable("skuId") Long skuId);

    @GetMapping("api/product/inner/getCategoryView/{category3Id}")
    BaseCategoryView getCategoryView(@PathVariable("category3Id") Long category3Id);

    @GetMapping("api/product/inner/getSkuPrice/{skuId}")
    BigDecimal getSkuPrice(@PathVariable("skuId") Long skuId);

    @GetMapping("api/product/inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable("skuId") Long skuId, @PathVariable("spuId") Long spuId);

    @GetMapping("api/product/inner/getSkuValueIdsMap/{spuId}")
    Map getSkuValueIdsMap(@PathVariable("spuId") Long spuId);

    @GetMapping("api/product/getBaseCategoryList")
    Result<List<JSONObject>> getBaseCategoryList();

    @GetMapping("api/product/inner/getTrademark/{tmId}")
    BaseTrademark getTrademark(@PathVariable("tmId") Long tmId);

    @GetMapping("api/product/inner/getAttrList/{skuId}")
    List<BaseAttrInfo> getAttrList(@PathVariable("skuId") Long skuId);
}
