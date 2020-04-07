package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.client.ItemFeignClient;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

/**
 * @author zr
 * @create 2020-03-18 下午 16:45
 */
@Api(tags = "商品详情页")
@Controller
@RequestMapping
public class ItemController {

    @Autowired
    private ItemFeignClient itemFeignClient;

    @RequestMapping("{skuId}.html")
    public String getItem(@PathVariable Long skuId, Model model) {
        Result<Map<String, Object>> result = itemFeignClient.getItem(skuId);

        //保存数据，将map中的所有数据保存起来
        model.addAllAttributes(result.getData());
        return "item/index";
    }
}
