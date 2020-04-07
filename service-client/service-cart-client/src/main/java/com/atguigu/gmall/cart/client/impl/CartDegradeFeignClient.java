package com.atguigu.gmall.cart.client.impl;

import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.cart.CartInfo;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author zr
 * @create 2020-03-27 下午 20:46
 */
@Component
public class CartDegradeFeignClient implements CartFeignClient {
    @Override
    public Result addToCart(Long skuId, Integer skuNum) {
        return null;
    }

    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        return null;
    }

    @Override
    public Result loadCartCache(String userId) {
        return null;
    }
}
