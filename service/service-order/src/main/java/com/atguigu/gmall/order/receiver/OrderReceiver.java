package com.atguigu.gmall.order.receiver;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Map;

/**
 * @author zr
 * @create 2020-03-31 下午 20:46
 */
@Component
public class OrderReceiver {

    @Autowired
    private OrderService orderService;

    // 获取监听到的消息队列
    @RabbitListener(queues = MqConst.QUEUE_ORDER_CANCEL)
    public void orderCancel(Long orderId, Message message, Channel channel) throws IOException {
        if (orderId != null) {
            // 判断当前订单的状态，如果是未付款，关闭订单
            // 扩展：判断当前订单是否真正的支付{在支付宝是否有交易记录}
            /**
             *  面试可能会问你，关单的业务逻辑！
             *  判断 paymentInfo 中有没有交易记录，是否是未付款！
             *  判断 orderInfo 订单的状态
             *  判断在支付宝中是否有交易记录。
             *  如果有交易记录{扫描了二维码} alipayService.checkPayment(orderId)
             *  如果在支付宝中有交易记录，调用关闭支付的订单接口。如果正常关闭了，那么说明，用户根本没有付款。如果关闭失败。
             *      说明用户已经付款成功了。 发送消息队列更新订单的状态！ 通知仓库，减库存。
             *      rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY,MqConst.ROUTING_PAYMENT_PAY,paymentInfo.getOrderId());
             *  关闭订单：
             *      1.  用户没有点到付款，二维码都没有出现。
             *      2.  系统出现了二维码，但是用户并没有扫描。
             *      3.  系统出现了二维码，用户扫了，但是没有输入密码。
             */
            OrderInfo orderInfo = orderService.getById(orderId);
            if (orderInfo != null && orderInfo.getOrderStatus().equals(ProcessStatus.UNPAID.getOrderStatus().name())) {
                // 修改订单状态，变为CLOSED
                orderService.execExpiredOrder(orderId);
            }
        }
        // 手动ack
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    // 订单支付，更改订单状态与通知扣减库存
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_PAYMENT_PAY, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_PAYMENT_PAY),
            key = {MqConst.ROUTING_PAYMENT_PAY}
    ))
    public void paySuccess(Long orderId, Message message, Channel channel) throws IOException {
        if (orderId != null) {
            // 防止重复消费
            OrderInfo orderInfo = orderService.getById(orderId);
            if (orderInfo != null && orderInfo.getOrderStatus().equals(ProcessStatus.UNPAID.getOrderStatus().name())) {
                // 支付成功，修改订单状态为已支付
                orderService.updateOrderStatus(orderId, ProcessStatus.PAID);
                // 发送消息，通知仓库
                orderService.sendOrderStatus(orderId);
            }
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    // 扣减库存成功，更新订单状态
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_WARE_ORDER, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_WARE_ORDER),
            key = {MqConst.ROUTING_WARE_ORDER}
    ))
    public void updateOrderStatus(String msgJson, Message message, Channel channel) throws IOException {
        if (!StringUtils.isEmpty(msgJson)) {
            Map<String, Object> map = JSON.parseObject(msgJson, Map.class);
            String orderId = (String) map.get("orderId");
            String status = (String) map.get("status");
            if ("DEDUCTED".equals(status)) {
                // 减库存成功，修改订单状态为已支付
                orderService.updateOrderStatus(Long.parseLong(orderId), ProcessStatus.WAITING_DELEVER);
            } else {
                /*
                减库存失败，远程调用其他仓库看是否有库存
                true:   orderService.sendOrderStatus(orderId); orderService.updateOrderStatus(orderId, ProcessStatus.NOTIFIED_WARE);
                false:  1.  补货  | 2.   人工客服。
                */
                orderService.updateOrderStatus(Long.parseLong(orderId), ProcessStatus.STOCK_EXCEPTION);

            }
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

}
