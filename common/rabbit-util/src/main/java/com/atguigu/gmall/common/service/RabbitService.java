package com.atguigu.gmall.common.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.entity.GmallCorrelationData;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author zr
 * 操作rabbitMq 工具类，封装了一个公共的发送方法
 * @create 2020-03-30 下午 20:33
 */
@Service
public class RabbitService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

    // 过期时间：分钟
    public static final int OBJECT_TIMEOUT = 10;

    /**
     * 发送普通消息
     *
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param message    消息
     * @return
     */
    public boolean sendMessage(String exchange, String routingKey, Object message) {
        // 发送消息
        // rabbitTemplate.convertAndSend(exchange, routingKey, message);
        // 封装好的数据对象
        GmallCorrelationData gmallCorrelationData = new GmallCorrelationData();
        String correlationDataId = UUID.randomUUID().toString();
        // id
        gmallCorrelationData.setId(correlationDataId);
        // exchange
        gmallCorrelationData.setExchange(exchange);
        // routingKey
        gmallCorrelationData.setRoutingKey(routingKey);
        // message
        gmallCorrelationData.setMessage(message);

        // 为防止消息处理失败，将消息放入缓存
        redisTemplate.opsForValue().set(correlationDataId, JSON.toJSONString(gmallCorrelationData), OBJECT_TIMEOUT, TimeUnit.MINUTES);

        // 发送的消息数据
        rabbitTemplate.convertAndSend(exchange, routingKey, message, gmallCorrelationData);

        // 默认发送成功
        return true;
    }

    /**
     * 发送延迟队列的消息
     *
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param message    消息内容
     * @param delayTime  延迟时间
     * @return
     */
    public boolean sendDelayMessage(String exchange, String routingKey, Object message, int delayTime) {
        // 封装好的数据对象
        GmallCorrelationData gmallCorrelationData = new GmallCorrelationData();
        String correlationDataId = UUID.randomUUID().toString();
        // id
        gmallCorrelationData.setId(correlationDataId);
        // exchange
        gmallCorrelationData.setExchange(exchange);
        // routingKey
        gmallCorrelationData.setRoutingKey(routingKey);
        // message
        gmallCorrelationData.setMessage(message);
        // 设置延迟时间
        gmallCorrelationData.setDelayTime(delayTime);
        // 确定开启延迟
        gmallCorrelationData.setDelay(true);

        // 为防止消息处理失败，将消息存入缓存一份
        redisTemplate.opsForValue().set(correlationDataId, JSON.toJSONString(gmallCorrelationData), OBJECT_TIMEOUT, TimeUnit.MINUTES);

        // 发送延迟消息内容
        rabbitTemplate.convertAndSend(exchange, routingKey, message, new MessagePostProcessor() {
            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                // 设置延迟时间
                message.getMessageProperties().setDelay(delayTime * 1000);

                return message;
            }
        }, gmallCorrelationData);
        // 默认发送成功
        return true;
    }
}
