package com.atguigu.gmall.list.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.service.SearchService;
import com.atguigu.gmall.model.list.Goods;
import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * @author zr
 * @create 2020-03-23 下午 20:37
 */
@RestController
@RequestMapping("api/list")
public class ListApiController {

    @Autowired
    private SearchService searchService;

    @Autowired
    private ElasticsearchRestTemplate restTemplate;

    @ApiOperation("初始化mapping结构到es中")
    @GetMapping("inner/createIndex")
    public Result createIndex() {
        restTemplate.createIndex(Goods.class);
        restTemplate.putMapping(Goods.class);
        return Result.ok();
    }

    @ApiOperation("上架商品")
    @GetMapping("inner/upperGoods/{skuId}")
    public Result upperGoods(@PathVariable Long skuId) {
        searchService.upperGoods(skuId);
        return Result.ok();
    }

    @ApiOperation("下架商品")
    @GetMapping("inner/lowerGoods/{skuId}")
    public Result lowerGoods(@PathVariable Long skuId) {
        searchService.lowerGoods(skuId);
        return Result.ok();
    }

    @ApiOperation("更新商品")
    @GetMapping("inner/incrHotScore/{skuId}")
    public Result incrHotScore(@PathVariable Long skuId) {
        searchService.incrHotScore(skuId);
        return Result.ok();
    }

    @ApiOperation("搜索商品")
    @PostMapping
    public Result list(@RequestBody SearchParam searchParam) throws IOException {
        SearchResponseVo response = searchService.search(searchParam);
        return Result.ok(response);
    }
}
