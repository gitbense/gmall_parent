package com.atguigu.gmall.item.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.service.ItemService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author zr
 * @create 2020-03-17 下午 19:21
 */
@RestController
@RequestMapping("api/item")
public class ItemApiController {

    @Autowired
    private ItemService itemService;

    @ApiOperation("获取sku详情信息")
    @GetMapping("{skuId}")
    public Result<Map<String, Object>> getItem(@PathVariable Long skuId) {
        return Result.ok(itemService.getBySkuId(skuId));
    }
}
