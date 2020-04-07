package com.atguigu.gmall.order.client;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.client.impl.OrderDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * @author zr
 * @create 2020-03-28 下午 20:03
 */
@FeignClient(name = "service-order",fallback = OrderDegradeFeignClient.class)
public interface OrderFeignClient {

    /**
     *确认订单
     * @return
     */
    @GetMapping("api/order/auth/trade")
    Result<Map<String, Object>> trade();

    @GetMapping("api/order/inner/getOrderInfo/{orderId}")
    OrderInfo getOrderInfo(@PathVariable Long orderId);

    /**
     * 提交秒杀订单
     * @param orderInfo
     * @return
     */
    @PostMapping("api/order/inner/seckill/submitOrder")
    Long submitOrder(@RequestBody OrderInfo orderInfo);
}
