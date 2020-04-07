package com.atguigu.gmall.list.client.impl;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;
import org.springframework.stereotype.Component;

/**
 * @author zr
 * @create 2020-03-24 上午 8:34
 */
@Component
public class ListDegradeFeignClient implements ListFeignClient {
    @Override
    public Result incrHotScore(Long skuId) {
        return null;
    }

    @Override
    public Result<SearchResponseVo> list(SearchParam searchParam) {
        return Result.fail();
    }

    @Override
    public Result upperGoods(Long skuId) {
        return null;
    }

    @Override
    public Result lowerGoods(Long skuId) {
        return null;
    }
}
