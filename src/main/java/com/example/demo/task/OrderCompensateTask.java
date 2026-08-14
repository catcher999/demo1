package com.example.demo.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.entity.recharge.RechargeOrder;
import com.example.demo.mapper.recharge.RechargeOrderMapper;
import com.example.demo.service.recharge.RechargeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 掉单补偿定时任务
 *
 * 场景：用户已支付，但支付宝异步回调丢失（网络问题等）
 * 处理：每 5 分钟扫描 30 分钟前创建的 pending 订单，调支付宝接口查询
 *       若已支付则补偿更新状态 + 加算力
 */
@Component
@Slf4j
public class OrderCompensateTask {

    private final RechargeOrderMapper orderMapper;
    private final RechargeService rechargeService;

    public OrderCompensateTask(RechargeOrderMapper orderMapper, RechargeService rechargeService) {
        this.orderMapper = orderMapper;
        this.rechargeService = rechargeService;
    }

    /**
     * 每 5 分钟执行一次
     * 扫描 30 分钟前创建、仍为 pending 的订单
     */
    @Scheduled(fixedRate = 5 * 60 * 1000L)
    public void compensate() {
        // 30 分钟前的时间点
        Date threshold = new Date(System.currentTimeMillis() - 30 * 60 * 1000L);

        LambdaQueryWrapper<RechargeOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeOrder::getStatus, "pending")
                .lt(RechargeOrder::getCreatedAt, threshold);

        List<RechargeOrder> pendingOrders = orderMapper.selectList(wrapper);

        if (pendingOrders.isEmpty()) {
            return;
        }

        log.info("掉单补偿任务启动，待检查订单数：{}", pendingOrders.size());

        int compensatedCount = 0;
        for (RechargeOrder order : pendingOrders) {
            try {
                boolean success = rechargeService.compensateOrder(order.getOrderNo());
                if (success) {
                    compensatedCount++;
                }
            } catch (Exception e) {
                log.error("掉单补偿异常，orderNo={}", order.getOrderNo(), e);
            }
        }

        log.info("掉单补偿任务完成，共补偿 {} 笔订单", compensatedCount);
    }
}
