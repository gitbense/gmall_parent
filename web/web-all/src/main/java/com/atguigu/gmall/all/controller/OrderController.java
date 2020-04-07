package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.order.client.OrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * @author zr
 * @create 2020-03-28 下午 20:10
 */
@Controller
public class OrderController {

    @Autowired
    private OrderFeignClient orderFeignClient;

    /**
     * 确认订单
     *
     * @param model
     * @return
     */
    @GetMapping("trade.html")
    public String trade(Model model) {
        Result<Map<String, Object>> result = orderFeignClient.trade();
        model.addAllAttributes(result.getData());

        return "order/trade";
    }
}
