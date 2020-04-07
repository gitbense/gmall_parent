package com.atguigu.gmall.activity.controller;

import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.common.util.CacheHelper;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.common.util.MD5;
import com.atguigu.gmall.model.activity.OrderRecode;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.activity.UserRecode;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.client.OrderFeignClient;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.atguigu.gmall.user.client.UserFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * @author zr
 * @create 2020-04-06 下午 18:32
 */
@RestController
@RequestMapping("api/activity/seckill")
public class SeckillGoodsController {

    @Autowired
    private SeckillGoodsService seckillGoodsService;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private OrderFeignClient orderFeignClient;

    // 查询所有秒杀商品
    @GetMapping("findAll")
    public Result findAll() {
        return Result.ok(seckillGoodsService.findAll());
    }

    // 根据Id查询秒杀商品
    @GetMapping("getSeckillGoods/{skuId}")
    public Result getSeckillGoods(@PathVariable Long skuId) {
        SeckillGoods seckillGoods = seckillGoodsService.getSecKillGoodsById(skuId);
        return Result.ok(seckillGoods);
    }

    // 获取下单码
    @GetMapping("auth/getSeckillSkuIdStr/{skuId}")
    public Result getSeckillSkuIdStr(@PathVariable Long skuId, HttpServletRequest request) {
        // 获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        // 通过skuId获取到当前要秒杀的商品
        SeckillGoods seckillGoods = seckillGoodsService.getSecKillGoodsById(skuId);

        if (seckillGoods != null) {
            // 判断当前时间是否在秒杀时间内
            Date curTime = new Date();
            if (DateUtil.dateCompare(seckillGoods.getStartTime(), curTime) && DateUtil.dateCompare(curTime, seckillGoods.getEndTime())) {
                // 生成下单码
                String skuIdStr = MD5.encrypt(userId);
                return Result.ok(skuIdStr);
            }
        }
        return Result.fail().message("获取下单码失败！");
    }

    // url: this.api_name + '/auth/seckillOrder/' + skuId + '?skuIdStr=' + skuIdStr,
    @PostMapping("auth/seckillOrder/{skuId}")
    public Result seckillOrder(@PathVariable Long skuId, HttpServletRequest request) {
        // 获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        // 获取页面传递的下单码
        String skuIdStr = request.getParameter("skuIdStr");
        // 校验下单码，防止用户通过浏览器的地址直接修改下单码
        if (!skuIdStr.equals(MD5.encrypt(userId))) {
            // 请求不合法
            return Result.build(null, ResultCodeEnum.SECKILL_ILLEGAL);
        }
        // 判断缓存标识位 1：可以秒  0：售罄
        // 获取状态位
        String state = (String) CacheHelper.get(skuId.toString());
        // 17:0 没有商品
        // 17:1 有商品
        if (StringUtils.isEmpty(state)) {
            // 请求不合法，状态位是空
            return Result.build(null, ResultCodeEnum.SECKILL_ILLEGAL);
        }
        if ("1".equals(state)) {
            // 可以秒杀
            // 存储用户信息，只存储商品Id，用户Id
            UserRecode userRecode = new UserRecode();
            userRecode.setSkuId(skuId);
            userRecode.setUserId(userId);
            // 发送消息，将获取到秒杀资格的用户存储起来，保证用户的顺序
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_SECKILL_USER, MqConst.ROUTING_SECKILL_USER, userRecode);
        } else {
            // 已经售罄
            return Result.build(null, ResultCodeEnum.SECKILL_FINISH);
        }
        return Result.ok();
    }

    // 查询秒杀状态
    @GetMapping("auth/checkOrder/{skuId}")
    public Result checkOrder(@PathVariable Long skuId, HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        return seckillGoodsService.checkOrder(skuId, userId);
    }

    // 秒杀确认订单
    @GetMapping("auth/trade")
    public Result trade(HttpServletRequest request) {
        // 获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        // 获取用户购买的商品
        OrderRecode orderRecode = (OrderRecode) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).get(userId);
        if (null == orderRecode) {
            return Result.fail().message("非法操作！");
        }
        // 获取里面的数据
        SeckillGoods seckillGoods = orderRecode.getSeckillGoods();

        // 获取用户地址列表
        List<UserAddress> userAddressList = userFeignClient.findUserAddressListByUserId(userId);

        // 声明一个订单明细集合
        List<OrderDetail> orderDetailList = new ArrayList<>();
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setSkuId(seckillGoods.getSkuId());
        orderDetail.setSkuName(seckillGoods.getSkuName());
        // orderDetail.setSkuNum(seckillGoods.getNum());
        orderDetail.setSkuNum(orderRecode.getNum());
        orderDetail.setImgUrl(seckillGoods.getSkuDefaultImg());
        orderDetail.setOrderPrice(seckillGoods.getCostPrice());

        orderDetailList.add(orderDetail);

        // 计算总金额
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(orderDetailList);
        orderInfo.sumTotalAmount();

        // 声明一个map集合
        HashMap<String, Object> map = new HashMap<>();
        map.put("detailArrayList", orderDetailList);
        map.put("userAddressList", userAddressList);
        map.put("totalAmount", orderInfo.getTotalAmount());

        return Result.ok(map);
    }

    @PostMapping("auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo, HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);

        OrderRecode orderRecode = (OrderRecode) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).get(userId);
        if (null == orderRecode) {
            return Result.fail().message("非法操作");
        }
        orderInfo.setUserId(Long.parseLong(userId));

        Long orderId = orderFeignClient.submitOrder(orderInfo);
        if (null == orderId) {
            return Result.fail().message("下单失败，请重新操作");
        }
        // 删除下单信息
        redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).delete(userId);

        // 下单记录
        redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).put(userId, orderId.toString());

        return Result.ok(orderId);
    }
}
