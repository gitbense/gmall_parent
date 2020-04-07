package com.atguigu.gmall.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.HttpClientUtil;
import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.order.service.OrderService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * @author zr
 * @create 2020-03-28 下午 21:06
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitService rabbitService;

    @Value("${ware.url}")
    private String wareUrl;

    // 保存订单
    @Override
    @Transactional
    public Long saveOrderInfo(OrderInfo orderInfo) {
        // 保存OrderInfo OrderDetail
        // 页面提交的orderInfo中，没有总金额，订单状态，用户Id，订单交易编号，创建时间，过期时间，订单描述，进度状态。
        // 赋值总金额
        orderInfo.sumTotalAmount();
        // 赋值订单状态
        orderInfo.setOrderStatus(OrderStatus.UNPAID.name());
        // 订单交易编号
        String outTradeNo = "ATGUIGU" + System.currentTimeMillis() + "" + new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        // 赋值创建时间
        orderInfo.setCreateTime(new Date());
        // 过期时间1天
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 1);
        orderInfo.setExpireTime(calendar.getTime());

        // 订单描述：可以给固定字符串，也可以获取商品名称
        // 获取订单明细集合
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        StringBuffer tradeBody = new StringBuffer();
        for (OrderDetail orderDetail : orderDetailList) {
            tradeBody.append(orderDetail.getSkuName() + "");
        }
        if (tradeBody.toString().length() > 100) {
            orderInfo.setTradeBody(tradeBody.toString().substring(0, 100));
        }
        orderInfo.setTradeBody(tradeBody.toString());
        // 设置进度状态
        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name());
        // 添加orderInfo
        orderInfoMapper.insert(orderInfo);

        // 添加订单明细
        for (OrderDetail orderDetail : orderDetailList) {
            // 赋值orderId
            orderDetail.setOrderId(orderInfo.getId());
            // 添加
            orderDetailMapper.insert(orderDetail);
        }
        // 发送延时队列，如果定时未支付，取消订单
        rabbitService.sendDelayMessage(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL, MqConst.ROUTING_ORDER_CANCEL, orderInfo.getId(), MqConst.DELAY_TIME);
        // 返回订单Id
        return orderInfo.getId();
    }

    // 生成流水号
    @Override
    public String getTradeNo(String userId) {
        // 定义key
        String tradeNoKey = "user:" + userId + ":tradeCode";
        // 定义流水号
        String tradeNo = UUID.randomUUID().toString().replace("-", "");
        // 存入缓存
        redisTemplate.opsForValue().set(tradeNoKey, tradeNo);
        return tradeNo;
    }

    // 比较流水号
    @Override
    public boolean checkTradeCode(String userId, String tradeCodeNo) {
        // 定义key
        String tradeNoKey = "user:" + userId + ":tradeCode";
        // 查询缓存
        String redisTradeNo = (String) redisTemplate.opsForValue().get(tradeNoKey);

        return tradeCodeNo.equals(redisTradeNo);
    }

    // 删除流水号
    @Override
    public void deleteTradeNo(String userId) {
        // 定义key
        String tradeNoKey = "user:" + userId + ":tradeCode";
        // 删除
        redisTemplate.delete(tradeNoKey);
    }

    // 验证库存
    @Override
    public boolean checkStock(Long skuId, Integer skuNum) {
        // 调用库存系统的接口 http://localhost:9001/hasStock?skuId=xxx&num=xxx 远程调用
        String result = HttpClientUtil.doGet(wareUrl + "/hasStock?skuId=" + skuId + "&num=" + skuNum);
        return "1".equals(result);
    }

    // 处理过期订单
    @Override
    public void execExpiredOrder(Long orderId) {
        // orderInfo
        updateOrderStatus(orderId, ProcessStatus.CLOSED);
        // paymentInfo
        // paymentFeignClient.closePayment(orderId)
        // 取消交易
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE, MqConst.ROUTING_PAYMENT_CLOSE, orderId);
    }

    // 根据订单Id 修改订单的状态
    @Override
    public void updateOrderStatus(Long orderId, ProcessStatus processStatus) {
        // 更新数据
        // update order_info set order_status = CLOSED,process_status = CLOSED where id = orderId
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(processStatus.name());
        orderInfo.setOrderStatus(processStatus.getOrderStatus().name());

        orderInfoMapper.updateById(orderInfo);
    }

    // 根据订单Id 查询订单信息
    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        QueryWrapper<OrderDetail> orderDetailQueryWrapper = new QueryWrapper<>();
        orderDetailQueryWrapper.eq("order_id", orderId);
        List<OrderDetail> orderDetailList = orderDetailMapper.selectList(orderDetailQueryWrapper);

        orderInfo.setOrderDetailList(orderDetailList);
        return orderInfo;
    }

    // 发送消息给库存
    @Override
    public void sendOrderStatus(Long orderId) {
        updateOrderStatus(orderId, ProcessStatus.NOTIFIED_WARE);

        String wareJson = initWareOrder(orderId);
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_WARE_STOCK, MqConst.ROUTING_WARE_STOCK, wareJson);
    }

    // 根据orderId 获取json 字符串
    private String initWareOrder(Long orderId) {
        // 通过orderId获取orderInfo
        OrderInfo orderInfo = getOrderInfo(orderId);

        // 将orderInfo中部分数据转换为map
        Map<String, Object> map = initWareOrder(orderInfo);

        return JSON.toJSONString(map);
    }

    // 将orderInfo中部分数据转换为map
    @Override
    public Map<String, Object> initWareOrder(OrderInfo orderInfo) {
        Map<String, Object> map = new HashMap<>();
        map.put("orderId", orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel", orderInfo.getConsigneeTel());
        map.put("orderComment", orderInfo.getOrderComment());
        map.put("orderBody", orderInfo.getTradeBody());
        map.put("deliveryAddress", orderInfo.getDeliveryAddress());
        map.put("paymentWay", "2");
        map.put("wareId", orderInfo.getWareId()); // 仓库Id，减库存，拆单时使用

        ArrayList<Map> mapArrayList = new ArrayList<>();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            HashMap<String, Object> orderDetailMap = new HashMap<>();
            orderDetailMap.put("skuId", orderDetail.getSkuId());
            orderDetailMap.put("skuNum", orderDetail.getSkuNum());
            orderDetailMap.put("skuName", orderDetail.getSkuName());

            mapArrayList.add(orderDetailMap);
        }
        map.put("details", mapArrayList);
        return map;
    }

    // 拆单接口
    @Override
    public List<OrderInfo> orderSplit(long orderId, String wareSkuMap) {
        // 声明一个子订单集合
        List<OrderInfo> subOrderInfoList = new ArrayList<>();
        /*
        1、获取原始订单
        2、分解wareSkuMap[{"wareId":"1","skuIds":["17"]},{"wareId":"2","skuIds":["21"]}]对象
        3、创建子订单
        4、给子订单赋值
        5、保存子订单到数据
        6、修改订单状态
         */
        // 获取原始订单
        OrderInfo orderInfoOrgin = getOrderInfo(orderId);
        // 分解wareSkuMap,转为map
        List<Map> mapList = JSON.parseArray(wareSkuMap, Map.class);
        if (mapList != null && mapList.size() > 0) {
            // 循环判断
            for (Map map : mapList) {
                // 获取仓库Id
                String wareId = (String) map.get("wareId");
                // 获取仓库Id对应的商品Id
                List<String> skuIdList = (List<String>) map.get("skuIds");
                // 子订单
                OrderInfo subOrderInfo = new OrderInfo();
                // 给子订单赋值
                BeanUtils.copyProperties(orderInfoOrgin, subOrderInfo);
                // 为防止子订单主键冲突，将id设置为null
                subOrderInfo.setId(null);
                // 赋值父Id
                subOrderInfo.setParentOrderId(orderId);
                // 设置仓库Id
                subOrderInfo.setWareId(wareId);
                // 设置子订单明细
                // 获取原始订单明细
                List<OrderDetail> orderDetailList = orderInfoOrgin.getOrderDetailList();
                // 声明订单明细集合
                List<OrderDetail> orderDetailArrayList = new ArrayList<>();
                // 判断
                if (orderDetailList != null && orderDetailList.size() > 0) {
                    for (OrderDetail orderDetail : orderDetailList) {
                        for (String skuId : skuIdList) {
                            if (Long.parseLong(skuId) == orderDetail.getSkuId()) {
                                // 添加订单明细
                                orderDetailArrayList.add(orderDetail);
                            }
                        }
                    }
                }
                // 子订单集合放入子订单中
                subOrderInfo.setOrderDetailList(orderDetailArrayList);
                // 计算价格
                subOrderInfo.sumTotalAmount();
                // 保存子订单
                saveOrderInfo(subOrderInfo);
                // 添加子订单到集合中
                subOrderInfoList.add(subOrderInfo);
            }
        }
        // 修改原始订单状态
        updateOrderStatus(orderId, ProcessStatus.SPLIT);

        return subOrderInfoList;
    }
}
