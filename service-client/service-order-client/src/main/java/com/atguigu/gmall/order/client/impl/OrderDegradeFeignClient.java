package com.atguigu.gmall.order.client.impl;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author zr
 * @create 2020-03-28 下午 20:05
 */
@Component
public class OrderDegradeFeignClient implements OrderFeignClient {
    @Override
    public Result<Map<String, Object>> trade() {
        return Result.fail();
    }

    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        return null;
    }

    @Override
    public Long submitOrder(OrderInfo orderInfo) {
        return null;
    }
}
