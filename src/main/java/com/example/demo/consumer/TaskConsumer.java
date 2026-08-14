package com.example.demo.consumer;

import com.example.demo.config.RabbitMQConfig;
import com.example.demo.dto.task.TaskMessage;
import com.example.demo.entity.task.Task;
import com.example.demo.entity.user.User;
import com.example.demo.mapper.task.TaskMapper;
import com.example.demo.mapper.user.UserMapper;
import com.example.demo.service.task.ImageGenerationService;
import com.example.demo.service.mail.MailService;
import com.example.demo.service.points.PointsService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 任务消费者
 * 1. 收到消息 → 状态改 running
 * 2. 调图片生成
 * 3. 成功 → succeeded + finished_at + 发邮件 + ACK
 * 4. 失败 → failed + finished_at + 退算力 + 发邮件 + NACK（消息转 task.dlx.queue）
 */
@Component
@Slf4j
public class TaskConsumer {

    private final TaskMapper taskMapper;
    private final UserMapper userMapper;
    private final ImageGenerationService imageGenerationService;
    private final PointsService pointsService;
    private final MailService mailService;

    public TaskConsumer(TaskMapper taskMapper,
                        UserMapper userMapper,
                        ImageGenerationService imageGenerationService,
                        PointsService pointsService,
                        MailService mailService) {
        this.taskMapper = taskMapper;
        this.userMapper = userMapper;
        this.imageGenerationService = imageGenerationService;
        this.pointsService = pointsService;
        this.mailService = mailService;
    }

    @RabbitListener(queues = RabbitMQConfig.TASK_QUEUE)
    public void onMessage(TaskMessage taskMessage, Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        Long taskId = taskMessage.getTaskId();
        log.info("收到任务消息，taskId={}, userId={}, model={}",
                taskId, taskMessage.getUserId(), taskMessage.getModel());

        try {
            Task task = taskMapper.selectById(taskId);
            if (task == null) {
                log.warn("任务不存在，直接 ACK 丢弃，taskId={}", taskId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 幂等：只处理 queued 状态的任务（可能已被取消）
            if (!"queued".equals(task.getStatus())) {
                log.warn("任务状态非 queued，跳过处理，taskId={}, status={}", taskId, task.getStatus());
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 1. 改为 running
            task.setStatus("running");
            taskMapper.updateById(task);

            // 2. 调图片生成
            String imageUrl = imageGenerationService.generate(task.getDescription());
            task.setResultUrl(imageUrl);
            task.setStatus("succeeded");
            task.setFinishedAt(new Date());
            taskMapper.updateById(task);
            log.info("图片生成成功，taskId={}, url={}", taskId, imageUrl);

            // 3. 发邮件通知
            sendNotify(task, "succeeded", imageUrl);

            // 4. ACK
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("图片生成失败，taskId={}", taskId, e);

            // 更新任务状态为 failed
            Task task = taskMapper.selectById(taskId);
            if (task != null) {
                task.setStatus("failed");
                task.setErrorMessage("图片生成失败：" + e.getMessage());
                task.setFinishedAt(new Date());
                taskMapper.updateById(task);

                // 退还算力
                if (task.getPointsCost() != null && task.getPointsCost() > 0) {
                    pointsService.refundPoints(task.getUserId(), task.getPointsCost());
                    log.info("已退还算力 {} 点，taskId={}", task.getPointsCost(), taskId);
                }

                // 发邮件通知
                sendNotify(task, "failed", null);
            }

            // NACK + 不重新入队 → 消息转 task.dlx.queue
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /** 发送任务结果邮件通知 */
    private void sendNotify(Task task, String status, String imageUrl) {
        try {
            User user = userMapper.selectById(task.getUserId());
            if (user != null && user.getEmail() != null) {
                mailService.sendTaskResultNotify(user.getEmail(), task.getId(), status, imageUrl);
            }
        } catch (Exception e) {
            // 邮件发送失败不影响主流程
            log.warn("邮件通知发送失败，taskId={}, error={}", task.getId(), e.getMessage());
        }
    }
}
