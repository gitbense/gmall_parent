package com.atguigu.gmall.mq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zr
 * @create 2020-03-31 下午 18:05
 */
@Configuration // 变成xml
public class DeadLetterMqConfig {

    // 声明交换机 路由键 队列
    public static final String exchange_dead = "exchange.dead";
    public static final String routing_dead_1 = "routing.dead.1";
    public static final String routing_dead_2 = "routing.dead.2";
    public static final String queue_dead_1 = "queue.dead.1";
    public static final String queue_dead_2 = "queue.dead.2";

    // <bean></bean>
    // 声明一个交换机 注入到spring容器中
    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(exchange_dead, true, false, null);
    }

    // 定义一个队列
    @Bean
    public Queue queue1() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", exchange_dead);
        arguments.put("x-dead-letter-routing-key", routing_dead_2);
        // 方式二：统一延迟时间
        arguments.put("x-message-ttl", 10 * 1000);

        return new Queue(queue_dead_1, true, false, false, arguments);
    }

    // 定义绑定规则
    @Bean
    public Binding binding() {
        return BindingBuilder.bind(queue1()).to(exchange()).with(routing_dead_1);
    }

    // 定义第二个队列
    @Bean
    public Queue queue2() {
        return new Queue(queue_dead_2, true, false, false, null);
    }

    // 定义绑定规则
    @Bean
    public Binding deadBinding() {
        return BindingBuilder.bind(queue2()).to(exchange()).with(routing_dead_2);
    }
}
