package com.atguigu.gmall.activity.receiver;

import com.atguigu.gmall.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.activity.UserRecode;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * @author zr
 * @create 2020-04-06 下午 12:26
 */
@Component
public class SeckillReceiver {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private SeckillGoodsService seckillGoodsService;

    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TASK_1),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_TASK),
            key = {MqConst.ROUTING_TASK_1}
    ))
    public void importGoods(Message message, Channel channel) {
        QueryWrapper<SeckillGoods> seckillGoodsQueryWrapper = new QueryWrapper<>();
        // 审核状态为1，剩余库存数大于0
        seckillGoodsQueryWrapper.eq("status", 1).gt("stock_count", 0);
        // 当前的时间必须为今天的日期，只查询当天要秒杀的商品
        seckillGoodsQueryWrapper.eq("DATE_FORMAT(start_time,'%Y-%m-%d')", DateUtil.formatDate(new Date()));

        // 查询秒杀的商品
        List<SeckillGoods> seckillGoodsList = seckillGoodsMapper.selectList(seckillGoodsQueryWrapper);

        // 将集合数据放入缓存中
        if (seckillGoodsList != null && seckillGoodsList.size() > 0) {
            for (SeckillGoods seckillGoods : seckillGoodsList) {
                // 使用hash来存储 hset(key,field,value)
                // key = seckill:goods field = skuId value = seckillGoods
                // 先判断缓存中是否已经存在当前秒杀的商品，有则不放入，没有则存入缓存
                Boolean flag = redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).hasKey(seckillGoods.getSkuId().toString());
                if (flag) {
                    continue;
                }
                // 将秒杀商品放入缓存
                redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).put(seckillGoods.getSkuId().toString(), seckillGoods);
                // 如何保证商品不超卖！ seckillGoods.getStockCount()数量存储到redis-list队列中
                for (Integer i = 0; i < seckillGoods.getStockCount(); i++) {
                    // key = seckill:stock:skuId
                    // value = skuId
                    redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + seckillGoods.getSkuId()).leftPush(seckillGoods.getSkuId().toString());
                }
                // 发布消息：当前所有商品初始化状态位都是1
                redisTemplate.convertAndSend("seckillpush", seckillGoods.getSkuId() + ":1");
            }
            // 手动确认当前消息已被处理
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }
    }

    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_SECKILL_USER, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_SECKILL_USER),
            key = {MqConst.ROUTING_SECKILL_USER}
    ))
    public void seckillUser(UserRecode userRecode, Message message, Channel channel) {
        if (null != userRecode) {
            // 预下单
            seckillGoodsService.seckillOrder(userRecode.getSkuId(), userRecode.getUserId());

            // 手动确认消息已经处理
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }
    }

    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TASK_18, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_TASK, type = ExchangeTypes.DIRECT, durable = "true"),
            key = {MqConst.ROUTING_TASK_18}
    ))
    public void clearRedis(Message message, Channel channel) {
        QueryWrapper<SeckillGoods> seckillGoodsQueryWrapper = new QueryWrapper<>();
        seckillGoodsQueryWrapper.eq("status", 1).eq("end_time", new Date());
        List<SeckillGoods> list = seckillGoodsMapper.selectList(seckillGoodsQueryWrapper);
        // 清空缓存
        for (SeckillGoods seckillGoods : list) {
            redisTemplate.delete(RedisConst.SECKILL_STOCK_PREFIX + seckillGoods.getSkuId());
        }
        redisTemplate.delete(RedisConst.SECKILL_GOODS);
        redisTemplate.delete(RedisConst.SECKILL_ORDERS);
        redisTemplate.delete(RedisConst.SECKILL_ORDERS_USERS);

        // 将状态更新为结束
        SeckillGoods seckillGoodsUp = new SeckillGoods();
        seckillGoodsUp.setStatus("2");
        seckillGoodsMapper.update(seckillGoodsUp, seckillGoodsQueryWrapper);

        // 手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
