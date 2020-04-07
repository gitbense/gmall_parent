package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author zr
 * @create 2020-03-14 下午 17:17
 */
@RestController
@RequestMapping("admin/product/baseTrademark")
public class BaseTrademarkController {

    @Autowired
    private BaseTrademarkService baseTrademarkService;

    @ApiOperation("品牌列表分页查询")
    @GetMapping("{page}/{limit}")
    public Result<IPage<BaseTrademark>> index(@ApiParam(name = "page", value = "当前页码", required = true) @PathVariable Long page,
                                              @ApiParam(name = "limit", value = "每页记录数", required = true) @PathVariable Long limit
    ) {
        Page<BaseTrademark> pageParam = new Page<>(page, limit);
        IPage<BaseTrademark> baseTrademarkIPage = baseTrademarkService.selectPage(pageParam);
        return Result.ok(baseTrademarkIPage);
    }

    @ApiOperation("根据id获取BaseTrademark")
    @GetMapping("get/{id}")
    public Result<BaseTrademark> get(@PathVariable Long id) {
        BaseTrademark baseTrademark = baseTrademarkService.getById(id);
        return Result.ok(baseTrademark);
    }

    @ApiOperation("新增BaseTrademark")
    @PostMapping("save")
    public Result save(@RequestBody BaseTrademark baseTrademark) {
        baseTrademarkService.save(baseTrademark);
        return Result.ok();
    }

    @ApiOperation("修改BaseTrademark")
    @PutMapping("update")
    public Result update(@RequestBody BaseTrademark baseTrademark) {
        baseTrademarkService.updateById(baseTrademark);
        return Result.ok();
    }

    @ApiOperation("删除BaseTrademark")
    @DeleteMapping("remove/{id}")
    public Result remove(@PathVariable Long id) {
        baseTrademarkService.removeById(id);
        return Result.ok();
    }

    @ApiOperation("查询全部品牌")
    @GetMapping("getTrademarkList")
    public Result<List<BaseTrademark>> getTrademarkList() {
        List<BaseTrademark> baseTrademarkList = baseTrademarkService.getTrademarkList();
        return Result.ok(baseTrademarkList);
    }

}
