package com.atguigu.gmall.item.service;

import java.util.Map;

/**
 * @author zr
 * @create 2020-03-17 下午 19:17
 */
public interface ItemService {

    /**
     * 获取sku详情信息
     * @param skuId
     * @return
     */
    Map<String, Object> getBySkuId(Long skuId);
}
