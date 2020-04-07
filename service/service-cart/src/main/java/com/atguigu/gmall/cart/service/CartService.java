package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.model.cart.CartInfo;

import java.util.List;

/**
 * @author zr
 * @create 2020-03-27 上午 11:08
 */
public interface CartService {

    /**
     * 添加购物车
     * @param skuId 商品Id
     * @param userId 用户Id
     * @param skuNum 商品数量
     */
    void addToCart(Long skuId,String userId,Integer skuNum);

    /**
     * 通过用户Id查询购物车列表
     * @param userId
     * @param userTempId
     * @return
     */
    List<CartInfo> getCartList(String userId,String userTempId);

    /**
     * 更改选中状态
     * @param userId
     * @param isChecked
     * @param skuId
     */
    void checkCart(String userId,Integer isChecked,Long skuId);

    /**
     * 删除购物车
     * @param skuId
     * @param userId
     */
    void deleteCart(Long skuId,String userId);

    /**
     * 根据用户id查询购物车列表
     * @param userId
     * @return
     */
    List<CartInfo> getCartCheckedList(String userId);

    List<CartInfo> loadCartCache(String userId);
}
