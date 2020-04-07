package com.atguigu.gmall.item.client.impl;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.client.ItemFeignClient;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author zr
 * @create 2020-03-18 下午 16:35
 */
@Component
public class ItemDegradeFeignClient implements ItemFeignClient {
    @Override
    public Result<Map<String, Object>> getItem(Long skuId) {
        return Result.fail();
    }
}
