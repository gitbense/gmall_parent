package com.atguigu.gmall.mq.receiver;

import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * @author zr
 * @create 2020-03-30 下午 20:44
 */
@Component
@Configuration
public class ConfirmReceiver {

    // rabbitmq使用注解形式来获取消息队列中的数据
    @SneakyThrows // 忽略异常
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "queue.confirm", autoDelete = "false"),
            exchange = @Exchange(value = "exchange.confirm", autoDelete = "true"),
            key = {"routing.confirm"}
    ))
    public void process(Message message, Channel channel) {
        // 声明一个字符串
        String msg = new String(message.getBody());
        System.out.println("获取到的消息：" + msg);

        try {
            int i = 1/0;
            // 确认消息，第二个参数：false 每次只确认一条消息。true 表示批量确认消息
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            if (message.getMessageProperties().getRedelivered()) {
                System.out.println("消息已经处理过，拒绝再次接收！");
                // 拒绝消息，requeue = false 表示不再重新入队，如果配置了死信队列则进入死信队列
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
            } else {
                System.out.println("消息即将再次返回队列处理！");
                // 参数二：是否批量 参数三：是否重新回到队列 true 重新入列
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            }
        }
    }
}
