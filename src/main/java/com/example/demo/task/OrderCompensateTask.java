package com.example.demo.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

    /** 单页扫描数量，避免一次性加载过多订单导致 OOM */
    private static final int PAGE_SIZE = 200;

    public OrderCompensateTask(RechargeOrderMapper orderMapper, RechargeService rechargeService) {
        this.orderMapper = orderMapper;
        this.rechargeService = rechargeService;
    }

    /**
     * 每 5 分钟执行一次
     * 扫描 30 分钟前创建、仍为 pending 的订单，分页处理
     */
    @Scheduled(fixedRate = 5 * 60 * 1000L)
    public void compensate() {
        Date threshold = new Date(System.currentTimeMillis() - 30 * 60 * 1000L);

        int currentPage = 1;
        int totalCompensated = 0;
        long totalScanned = 0;

        while (true) {
            Page<RechargeOrder> pageParam = new Page<>(currentPage, PAGE_SIZE);
            LambdaQueryWrapper<RechargeOrder> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(RechargeOrder::getStatus, "pending")
                    .lt(RechargeOrder::getCreatedAt, threshold)
                    .orderByAsc(RechargeOrder::getCreatedAt);

            Page<RechargeOrder> pageResult = orderMapper.selectPage(pageParam, wrapper);
            List<RechargeOrder> pendingOrders = pageResult.getRecords();

            if (pendingOrders.isEmpty()) {
                break;
            }

            totalScanned += pendingOrders.size();

            for (RechargeOrder order : pendingOrders) {
                try {
                    boolean success = rechargeService.compensateOrder(order.getOrderNo());
                    if (success) {
                        totalCompensated++;
                    }
                } catch (Exception e) {
                    log.error("掉单补偿异常，orderNo={}", order.getOrderNo(), e);
                }
            }

            // 最后一页不足 PAGE_SIZE，说明已到末尾
            if (pendingOrders.size() < PAGE_SIZE) {
                break;
            }

            // 关键：补偿成功后订单状态变为 paid，下一页会少一条记录
            // 因此保持 currentPage 不变继续查（新的 pending 订单会顶上来）
        }

        if (totalScanned > 0) {
            log.info("掉单补偿任务完成，扫描 {} 笔，补偿 {} 笔", totalScanned, totalCompensated);
        }
    }
}

