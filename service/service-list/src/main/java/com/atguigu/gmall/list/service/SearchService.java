package com.atguigu.gmall.list.service;

import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;

import java.io.IOException;

/**
 * @author zr
 * @create 2020-03-23 下午 21:10
 */
public interface SearchService {

    /**
     * 上架商品
     * @param skuId
     */
    void upperGoods(Long skuId);

    /**
     * 下架商品
     * @param skuId
     */
    void lowerGoods(Long skuId);

    /**
     * 更新热点
     * @param skuId
     */
    void incrHotScore(Long skuId);

    /**
     * 搜索列表
     * @param searchParam
     * @return
     * @throws IOException
     */
    SearchResponseVo search(SearchParam searchParam) throws IOException;
}
