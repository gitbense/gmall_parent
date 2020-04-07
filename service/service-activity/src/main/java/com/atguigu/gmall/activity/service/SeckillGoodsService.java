package com.atguigu.gmall.activity.service;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.activity.SeckillGoods;

import java.util.List;

/**
 * @author zr
 * @create 2020-04-06 下午 18:24
 */
public interface SeckillGoodsService {

    /**
     * 查询所有秒杀商品
     * @return
     */
    List<SeckillGoods> findAll();

    /**
     * 根据Id查询秒杀商品
     * @param id
     * @return
     */
    SeckillGoods getSecKillGoodsById(Long id);

    /**
     * 预下单
     * @param skuId
     * @param userId
     */
    void seckillOrder(Long skuId, String userId);

    /**
     * 根据商品Id与用户Id查看订单信息
     * @param skuId
     * @param userId
     * @return
     */
    Result checkOrder(Long skuId,String userId);
}
