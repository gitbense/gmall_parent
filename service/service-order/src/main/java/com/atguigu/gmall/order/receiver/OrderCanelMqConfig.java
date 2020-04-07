package com.atguigu.gmall.order.receiver;

import com.atguigu.gmall.common.constant.MqConst;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zr
 * @create 2020-03-31 下午 20:33
 */
@Configuration
public class OrderCanelMqConfig {

    // 定义队列
    @Bean
    public Queue dealyQueue() {
        // 订单的队列
        return new Queue(MqConst.QUEUE_ORDER_CANCEL, true);
    }

    // 定义交换机
    @Bean
    public CustomExchange dealyExchange() {
        Map<String, Object> map = new HashMap<>();
        map.put("x-delayed-type", "direct");
        return new CustomExchange(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL, "x-delayed-message", true, false, map);
    }

    @Bean
    public Binding delayBinding() {
        return BindingBuilder.bind(dealyQueue()).to(dealyExchange()).with(MqConst.ROUTING_ORDER_CANCEL).noargs();
    }
}
