package com.example.demo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 配置
 *
 * 任务队列（Direct Exchange）：
 *   task.exchange → task.queue (routing key: task)
 *   task.queue 消费失败 → task.dlx.exchange → task.dlx.queue (routing key: task.dlx)
 *
 * 订单延迟队列（TTL + DLX 实现 30 分钟自动关单）：
 *   order.exchange → order.delay.queue (routing key: order.delay, TTL 30min)
 *   order.delay.queue 消息过期 → order.dlx.exchange → order.close.queue (routing key: order.close)
 */
@Configuration
@Slf4j
public class RabbitMQConfig {

    // ==================== 任务队列 ====================
    public static final String TASK_EXCHANGE = "task.exchange";
    public static final String TASK_QUEUE = "task.queue";
    public static final String TASK_ROUTING_KEY = "task";

    public static final String TASK_DLX_EXCHANGE = "task.dlx.exchange";
    public static final String TASK_DLX_QUEUE = "task.dlx.queue";
    public static final String TASK_DLX_ROUTING_KEY = "task.dlx";

    // ==================== 订单延迟队列 ====================
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String ORDER_DELAY_QUEUE = "order.delay.queue";
    public static final String ORDER_DELAY_ROUTING_KEY = "order.delay";

    public static final String ORDER_DLX_EXCHANGE = "order.dlx.exchange";
    public static final String ORDER_CLOSE_QUEUE = "order.close.queue";
    public static final String ORDER_CLOSE_ROUTING_KEY = "order.close";

    /** 订单延迟关闭时间：30 分钟（毫秒） */
    private static final long ORDER_DELAY_TTL = 30 * 60 * 1000L;

    // ---------- 任务队列定义 ----------
    @Bean
    public DirectExchange taskExchange() {
        return new DirectExchange(TASK_EXCHANGE, true, false);
    }

    @Bean
    public Queue taskQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", TASK_DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", TASK_DLX_ROUTING_KEY);
        return new Queue(TASK_QUEUE, true, false, false, args);
    }

    @Bean
    public Binding taskBinding() {
        return BindingBuilder.bind(taskQueue())
                .to(taskExchange())
                .with(TASK_ROUTING_KEY);
    }

    // ---------- 任务死信队列定义 ----------
    @Bean
    public DirectExchange taskDlxExchange() {
        return new DirectExchange(TASK_DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue taskDlxQueue() {
        return new Queue(TASK_DLX_QUEUE, true);
    }

    @Bean
    public Binding taskDlxBinding() {
        return BindingBuilder.bind(taskDlxQueue())
                .to(taskDlxExchange())
                .with(TASK_DLX_ROUTING_KEY);
    }

    // ---------- 订单延迟队列定义 ----------
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", ORDER_DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", ORDER_CLOSE_ROUTING_KEY);
        args.put("x-message-ttl", ORDER_DELAY_TTL);
        return new Queue(ORDER_DELAY_QUEUE, true, false, false, args);
    }

    @Bean
    public Binding orderDelayBinding() {
        return BindingBuilder.bind(orderDelayQueue())
                .to(orderExchange())
                .with(ORDER_DELAY_ROUTING_KEY);
    }

    // ---------- 订单关闭队列定义 ----------
    @Bean
    public DirectExchange orderDlxExchange() {
        return new DirectExchange(ORDER_DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderCloseQueue() {
        return new Queue(ORDER_CLOSE_QUEUE, true);
    }

    @Bean
    public Binding orderCloseBinding() {
        return BindingBuilder.bind(orderCloseQueue())
                .to(orderDlxExchange())
                .with(ORDER_CLOSE_ROUTING_KEY);
    }

    // ---------- 消息序列化（JSON） ----------
    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // ---------- 启动时主动验证 RabbitMQ 连接 ----------
    @Bean
    public ApplicationRunner rabbitMqConnectionVerifier(ConnectionFactory connectionFactory) {
        return (ApplicationArguments args) -> {
            try (Connection connection = connectionFactory.createConnection()) {
                log.info("✅ RabbitMQ 连接成功");
                log.info("   交换机声明: task.exchange / task.dlx.exchange / order.exchange / order.dlx.exchange");
                log.info("   队列声明: task.queue / task.dlx.queue / order.delay.queue / order.close.queue");
            } catch (Exception e) {
                log.warn("⚠️  RabbitMQ 连接失败（不影响应用启动）: {}", e.getMessage());
                log.warn("   请确认 RabbitMQ 服务已启动: docker start rabbitmq");
            }
        };
    }
}
