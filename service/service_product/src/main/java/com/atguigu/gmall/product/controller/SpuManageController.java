package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseSaleAttr;
import com.atguigu.gmall.model.product.SpuInfo;
import com.atguigu.gmall.product.service.ManageService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author zr
 * @create 2020-03-15 下午 14:43
 */
@RestController
@RequestMapping("admin/product")
public class SpuManageController {

    @Autowired
    private ManageService manageService;

    @ApiOperation("查询所有销售属性")
    @GetMapping("baseSaleAttrList")
    public Result<List<BaseSaleAttr>> getBaseSaleAttrList() {
        List<BaseSaleAttr> spuInfoList = manageService.getBaseSaleAttrList();
        return Result.ok(spuInfoList);
    }

    @ApiOperation("保存spu")
    @PostMapping("saveSpuInfo")
    public Result saveSpuInfo(@RequestBody SpuInfo spuInfo) {
        manageService.saveSpuInfo(spuInfo);
        return Result.ok();
    }
}
