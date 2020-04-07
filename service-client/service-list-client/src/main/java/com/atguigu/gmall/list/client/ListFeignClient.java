package com.atguigu.gmall.list.client;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.client.impl.ListDegradeFeignClient;
import com.atguigu.gmall.model.list.SearchParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author zr
 * @create 2020-03-24 上午 8:33
 */
@FeignClient(value = "service-list",fallback = ListDegradeFeignClient.class)
public interface ListFeignClient {

    /**
     * 更新商品 热度
     * @param skuId
     * @return
     */
    @GetMapping("api/list/inner/incrHotScore/{skuId}")
    Result incrHotScore(@PathVariable("skuId") Long skuId);

    /**
     * 搜索商品
     * @param searchParam
     * @return
     */
    @PostMapping("api/list/")
    Result list(@RequestBody SearchParam searchParam);

    /**
     * 上架商品
     * @param skuId
     * @return
     */
    @GetMapping("api/list/inner/upperGoods/{skuId}")
    Result upperGoods(@PathVariable("skuId") Long skuId);

    /**
     * 下架商品
     * @param skuId
     * @return
     */
    @GetMapping("inner/lowerGoods/{skuId}")
    Result lowerGoods(@PathVariable("skuId") Long skuId);
}
