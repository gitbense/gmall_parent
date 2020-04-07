package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author zr
 * @create 2020-03-13 下午 18:14
 */
@Api(tags = "商品基础属性接口")
@RestController
@RequestMapping("admin/product")
public class BaseManageController {

    @Autowired
    private ManageService manageService;

    @ApiOperation("获取一级分类")
    @GetMapping("getCategory1")
    public Result<List<BaseCategory1>> getCategory1() {
        List<BaseCategory1> category1List = manageService.getCategory1();
        return Result.ok(category1List);
    }

    @ApiOperation("获取二级分类")
    @GetMapping("getCategory2/{category1Id}")
    public Result<List<BaseCategory2>> getCategory2(@PathVariable Long category1Id) {
        List<BaseCategory2> category2List = manageService.getCategory2(category1Id);
        return Result.ok(category2List);
    }

    @ApiOperation("获取三级分类")
    @GetMapping("getCategory3/{category2Id}")
    public Result<List<BaseCategory3>> getCategory3(@PathVariable Long category2Id) {
        List<BaseCategory3> category3List = manageService.getCategory3(category2Id);
        return Result.ok(category3List);
    }

    @ApiOperation("根据分类id获取平台属性")
    @GetMapping("attrInfoList/{category1Id}/{category2Id}/{category3Id}")
    public Result<List<BaseAttrInfo>> attrInfoList(@PathVariable Long category1Id,
                                                   @PathVariable Long category2Id,
                                                   @PathVariable Long category3Id) {
        List<BaseAttrInfo> attrInfoList = manageService.getAttrInfoList(category1Id, category2Id, category3Id);
        return Result.ok(attrInfoList);
    }

    @ApiOperation("添加or修改平台属性")
    @PostMapping("saveAttrInfo")
    public Result saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo) {
        manageService.saveAttrInfo(baseAttrInfo);
        return Result.ok();
    }

    @ApiOperation("根据平台属性ID获取平台属性，修改时回显")
    @GetMapping("getAttrValueList/{attrId}")
    public Result<List<BaseAttrValue>> getAttrValueList(@PathVariable Long attrId) {
        BaseAttrInfo baseAttrInfo = manageService.getAttrInfo(attrId);
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        return Result.ok(attrValueList);
    }

    @ApiOperation("spu分页查询")
    @GetMapping("{page}/{limit}")
    public Result<IPage<SpuInfo>> index(@ApiParam(name = "page", value = "当前页码", required = true) @PathVariable Long page,
                                        @ApiParam(name = "limit", value = "每页记录数", required = true) @PathVariable Long limit,
                                        @ApiParam(name = "spuInfo", value = "查询对象", required = false) SpuInfo spuInfo
    ) {
        Page<SpuInfo> pageParam = new Page<>(page, limit);
        IPage<SpuInfo> spuInfoIPage = manageService.selectPage(pageParam, spuInfo);
        return Result.ok(spuInfoIPage);
    }

    @ApiOperation("SKU分页列表")
    @GetMapping("list/{page}/{limit}")
    public Result<IPage<SkuInfo>> index(@ApiParam(name = "page", value = "当前页码", required = true) @PathVariable Long page,
                                        @ApiParam(name = "limit", value = "每页记录数", required = true) @PathVariable Long limit) {
        Page<SkuInfo> pageParam = new Page<>(page, limit);
        IPage<SkuInfo> skuInfoIPage = manageService.selectPage(pageParam);
        return Result.ok(skuInfoIPage);
    }

    @ApiOperation("商品上架")
    @GetMapping("onSale/{skuId}")
    public Result onSale(@PathVariable Long skuId) {
        manageService.onSale(skuId);
        return Result.ok();
    }

    @ApiOperation("商品下架")
    @GetMapping("cancelSale/{skuId}")
    public Result cancelSale(@PathVariable Long skuId) {
        manageService.cancelSale(skuId);
        return Result.ok();
    }
}
