package com.atguigu.gmall.activity.client;

import com.atguigu.gmall.activity.client.impl.ActivityDegradeFeignClient;
import com.atguigu.gmall.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * @author zr
 * @create 2020-04-06 下午 18:36
 */
@FeignClient(value = "service-activity",fallback = ActivityDegradeFeignClient.class)
public interface ActivityFeignClient {

    /**
     * 查询所有秒杀商品
     * @return
     */
    @GetMapping("api/activity/seckill/findAll")
    Result findAll();

    /**
     * 根据Id查询秒杀商品
     * @param skuId
     * @return
     */
    @GetMapping("api/activity/seckill/getSeckillGoods/{skuId}")
    Result getSeckillGoods(@PathVariable Long skuId);

    /**
     * 秒杀确认订单
     * @return
     */
    @GetMapping("api/activity/seckill/auth/trade")
    Result<Map<String, Object>> trade();
}
