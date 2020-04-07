package com.atguigu.gmall.order.controller;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.atguigu.gmall.user.client.UserFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zr
 * 订单的数据接口
 * @create 2020-03-28 下午 19:37
 */
@RestController
@RequestMapping("api/order")
public class OrderApiController {

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private CartFeignClient cartFeignClient;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private OrderService orderService;

    /**
     * 确认订单
     *
     * @param request
     * @return
     */
    @GetMapping("auth/trade")
    public Result<Map<String, Object>> trade(HttpServletRequest request) {
        // 获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        // 根据用户Id获取收货地址
        List<UserAddress> userAddressList = userFeignClient.findUserAddressListByUserId(userId);
        // 获取购物车中被选中的商品
        List<CartInfo> cartCheckedList = cartFeignClient.getCartCheckedList(userId);
        // 获取流水号
        String tradeNo = orderService.getTradeNo(userId);

        // 声明订单明细集合
        List<OrderDetail> orderDetailList = new ArrayList<>();
        // 循环选中商品集合
        for (CartInfo cartInfo : cartCheckedList) {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            orderDetail.setOrderPrice(cartInfo.getSkuPrice());
            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setSkuNum(cartInfo.getSkuNum());

            // 放入订单明细集合
            orderDetailList.add(orderDetail);
        }
        // 计算总金额
        OrderInfo orderInfo = new OrderInfo();
        // 给订单明细集合赋值
        orderInfo.setOrderDetailList(orderDetailList);
        orderInfo.sumTotalAmount();

        // 用map集合将数据存储起来
        Map<String, Object> map = new HashMap<>();
        // 订单收货地址集合
        map.put("userAddressList", userAddressList);
        // 订单明细集合
        map.put("detailArrayList", orderDetailList);
        // 总件数
        map.put("totalNum", orderDetailList.size());
        // 总金额
        map.put("totalAmount", orderInfo.getTotalAmount());
        //流水号
        map.put("tradeNo", tradeNo);

        // 返回数据
        return Result.ok(map);
    }

    @PostMapping("auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo, HttpServletRequest request) {
        // 获取用户Id
        String userId = AuthContextHolder.getUserId(request);

        // 获取前台页面流水号
        String tradeNo = request.getParameter("tradeNo");
        boolean flag = orderService.checkTradeCode(userId, tradeNo);
        if (!flag) {
            //比较失败
            return Result.fail().message("不能重复提交订单！");
        }
        // 比较成功，删除流水号
        orderService.deleteTradeNo(userId);
        // 验证库存：获取订单明细集合
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            // 返回true，表示有库存，否则没有库存
            boolean result = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
            if (!result) {
                return Result.fail().message(orderDetail.getSkuName() + "库存不足！");
            }
            // 获取到商品的实时价格 查询一下skuInfo.price 与 orderDetail.getOrderPrice()
            BigDecimal skuPrice = productFeignClient.getSkuPrice(orderDetail.getSkuId());
            if (skuPrice.compareTo(orderDetail.getOrderPrice()) != 0) {
                // 价格有变动，更新商品的最新价格{根据用户Id重新查询价格，添加到缓存}
                cartFeignClient.loadCartCache(userId);
                return Result.fail().message(orderDetail.getSkuName() + "价格有变动！");
            }
        }
        orderInfo.setUserId(Long.parseLong(userId));
        // 保存订单，返回订单Id
        Long orderId = orderService.saveOrderInfo(orderInfo);

        return Result.ok(orderId);
    }

    // 将根据订单编号查询订单对象
    @GetMapping("inner/getOrderInfo/{orderId}")
    public OrderInfo getOrderInfo(@PathVariable Long orderId) {
        return orderService.getOrderInfo(orderId);
    }

    /**
     * 拆单业务
     *
     * @param request
     * @return
     */
    // http://localhost:8204/api/order/orderSplit?orderId=xxx&wareSkuMap=xxx
    @RequestMapping("orderSplit")
    public String orderSplit(HttpServletRequest request) {
        // 获取对应的数据
        String orderId = request.getParameter("orderId");
        // 获取仓库Id与商品Id的对照关系
        // [{"wareId":"1","skuIds":["2","10"]},{"wareId":"2","skuIds":["3"]}]
        String wareSkuMap = request.getParameter("wareSkuMap");

        // 拆单，获取子订单集合
        List<OrderInfo> subOrderInfoList = orderService.orderSplit(Long.parseLong(orderId), wareSkuMap);
        // 声明一个存储map的集合
        List<Map<String, Object>> mapList = new ArrayList<>();
        // 遍历子订单集合，将每个orderInfo转化为map
        for (OrderInfo orderInfo : subOrderInfoList) {
            Map<String, Object> map = orderService.initWareOrder(orderInfo);
            // 添加到集合中
            mapList.add(map);
        }
        // 转换集合为json字符串
        return JSON.toJSONString(mapList);
    }

    /**
     * 秒杀提交订单，秒杀订单不需要做前置判断，直接下单
     * @param orderInfo
     * @return
     */
    @PostMapping("inner/seckill/submitOrder")
    public Long submitOrder(@RequestBody OrderInfo orderInfo) {
        Long orderId = orderService.saveOrderInfo(orderInfo);
        return orderId;
    }

}
