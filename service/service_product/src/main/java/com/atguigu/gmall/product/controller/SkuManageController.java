package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SpuImage;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.product.service.ManageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author zr
 * @create 2020-03-16 下午 18:18
 */
@Api(tags = "商品SKU接口")
@RestController
@RequestMapping("admin/product")
public class SkuManageController {

    @Autowired
    private ManageService manageService;

    @ApiOperation("根据spuId查询商品图片列表")
    @GetMapping("spuImageList/{spuId}")
    public Result<List<SpuImage>> getSpuImageList(@PathVariable Long spuId) {
        return Result.ok(manageService.getSpuImageList(spuId));
    }

    @ApiOperation("根据spuId查询销售属性集合")
    @GetMapping("spuSaleAttrList/{spuId}")
    public Result<List<SpuSaleAttr>> getSpuSaleAttrList(@PathVariable Long spuId) {
        return Result.ok(manageService.getSpuSaleAttrList(spuId));
    }

    @ApiOperation("保存sku")
    @PostMapping("saveSkuInfo")
    public Result saveSkuInfo(@RequestBody SkuInfo skuInfo) {
        manageService.saveSkuInfo(skuInfo);
        return Result.ok();
    }
}
