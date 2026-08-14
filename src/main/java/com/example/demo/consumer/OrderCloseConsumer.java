package com.example.demo.consumer;

import com.example.demo.config.RabbitMQConfig;
import com.example.demo.service.recharge.RechargeService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 订单关闭消费者
 * 消费 order.close.queue，关闭 30 分钟未支付的订单
 *
 * 流程：
 *   order.delay.queue（TTL 30min）→ 消息过期 → order.dlx.exchange → order.close.queue
 */
@Component
@Slf4j
public class OrderCloseConsumer {

    private final RechargeService rechargeService;

    public OrderCloseConsumer(RechargeService rechargeService) {
        this.rechargeService = rechargeService;
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_CLOSE_QUEUE)
    public void handleCloseOrder(String orderNo, Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            log.info("收到订单关闭消息，orderNo={}", orderNo);
            boolean closed = rechargeService.closeExpiredOrder(orderNo);

            if (closed) {
                log.info("✅ 订单已关闭，orderNo={}", orderNo);
            } else {
                log.info("订单无需关闭（可能已支付或不存在），orderNo={}", orderNo);
            }

            // 手动 ACK
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("处理订单关闭消息失败，orderNo={}", orderNo, e);
            // 拒绝消息，不重新入队（避免死循环）
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
