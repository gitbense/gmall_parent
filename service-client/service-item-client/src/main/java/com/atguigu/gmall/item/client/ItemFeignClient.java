package com.atguigu.gmall.item.client;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.client.impl.ItemDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * @author zr
 * @create 2020-03-18 下午 16:34
 */
@FeignClient(name = "service-item",fallback = ItemDegradeFeignClient.class)
public interface ItemFeignClient {

    @GetMapping("api/item/{skuId}")
    Result<Map<String, Object>> getItem(@PathVariable("skuId") Long skuId);
}
