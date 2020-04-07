package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * @author zr
 * @create 2020-03-31 下午 21:58
 */
@Controller
public class PaymentController {

    @Autowired
    private OrderFeignClient orderFeignClient;

    @GetMapping("pay.html")
    public String pay(HttpServletRequest request) {
        String orderId = request.getParameter("orderId");
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(Long.parseLong(orderId));

        request.setAttribute("orderInfo", orderInfo);
        return "payment/pay";
    }

    @GetMapping("pay/success.html")
    public String success() {
        return "payment/success";
    }
}
