package com.atguigu.gmall.product.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.ManageService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 该控制器负责给外部提供信息
 *
 * @author zr
 * @create 2020-03-17 下午 19:31
 */
@RestController
@RequestMapping("api/product")
public class ProductApiController {

    @Autowired
    private ManageService manageService;

    @ApiOperation("根据skuId获取skuInfo")
    @GetMapping("inner/getSkuInfo/{skuId}")
    public SkuInfo getSkuInfo(@PathVariable Long skuId) {
        return manageService.getSkuInfo(skuId);
    }

    @ApiOperation("通过三级分类id查询分类信息")
    @GetMapping("inner/getCategoryView/{category3Id}")
    public BaseCategoryView getCategoryView(@PathVariable Long category3Id) {
        return manageService.getCategoryViewByCategory3Id(category3Id);
    }

    @ApiOperation("获取sku价格")
    @GetMapping("inner/getSkuPrice/{skuId}")
    public BigDecimal getSkuPrice(@PathVariable Long skuId) {
        return manageService.getSkuPrice(skuId);
    }

    @ApiOperation("根据spuId，skuId 查询销售属性集合")
    @GetMapping("inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable Long skuId, @PathVariable Long spuId) {
        return manageService.getSpuSaleAttrListCheckBySku(skuId, spuId);
    }

    @ApiOperation("根据spuId 查询map 集合属性")
    @GetMapping("inner/getSkuValueIdsMap/{spuId}")
    public Map getSkuValueIdsMap(@PathVariable Long spuId) {
        return manageService.getSkuValueIdsMap(spuId);
    }

    @ApiOperation("获取全部分类信息")
    @GetMapping("getBaseCategoryList")
    public Result<List<JSONObject>> getBaseCategoryList() {
        List<JSONObject> baseCategoryList = manageService.getBaseCategoryList();
        return Result.ok(baseCategoryList);
    }

    @ApiOperation("通过品牌Id 来查询数据")
    @GetMapping("inner/getTrademark/{tmId}")
    public BaseTrademark getTrademark(@PathVariable Long tmId) {
        return manageService.getTrademarkByTmId(tmId);
    }

    @ApiOperation("通过skuId 查询对应的平台属性和属性值")
    @GetMapping("inner/getAttrList/{skuId}")
    public List<BaseAttrInfo> getAttrList(@PathVariable Long skuId) {
        return manageService.getAttrList(skuId);
    }
}
