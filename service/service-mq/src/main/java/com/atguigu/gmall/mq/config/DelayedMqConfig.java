package com.atguigu.gmall.mq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zr
 * @create 2020-03-31 下午 19:32
 */
@Configuration
public class DelayedMqConfig {

    public static final String exchange_delay = "exchange.delay";
    public static final String routing_delay = "routing.delay";
    public static final String queue_delay_1 = "queue.delay.1";

    // 声明一个队列
    @Bean
    public Queue delayQueue() {
        return new Queue(queue_delay_1, true);
    }

    // 声明交换机
    @Bean
    public CustomExchange delayExchange() {
        Map<String, Object> map = new HashMap<>();
        // 设置基于插件做延迟队列的交换机
        map.put("x-delayed-type", "direct");
        return new CustomExchange(exchange_delay, "x-delayed-message", true, false, map);
    }

    // 设置绑定
    @Bean
    public Binding delayBinding() {
        return BindingBuilder.bind(delayQueue()).to(delayExchange()).with(routing_delay).noargs();
    }
}
