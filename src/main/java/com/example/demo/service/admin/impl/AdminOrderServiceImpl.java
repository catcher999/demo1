package com.example.demo.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.BusinessException;
import com.example.demo.dto.admin.OrderAdminVO;
import com.example.demo.entity.admin.OperationLog;
import com.example.demo.entity.recharge.RechargeOrder;
import com.example.demo.mapper.admin.OperationLogMapper;
import com.example.demo.mapper.recharge.RechargeOrderMapper;
import com.example.demo.service.admin.AdminOrderService;
import com.example.demo.service.recharge.RechargeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
public class AdminOrderServiceImpl implements AdminOrderService {

    private final RechargeOrderMapper orderMapper;
    private final RechargeService rechargeService;
    private final OperationLogMapper operationLogMapper;

    public AdminOrderServiceImpl(RechargeOrderMapper orderMapper,
                                  RechargeService rechargeService,
                                  OperationLogMapper operationLogMapper) {
        this.orderMapper = orderMapper;
        this.rechargeService = rechargeService;
        this.operationLogMapper = operationLogMapper;
    }

    // ==================== 分页查询订单 ====================
    @Override
    public IPage<OrderAdminVO> listOrders(int page, int size, String status, String orderNo, Long userId) {
        Page<RechargeOrder> p = new Page<>(page, size);
        LambdaQueryWrapper<RechargeOrder> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(RechargeOrder::getStatus, status);
        }
        if (orderNo != null && !orderNo.isBlank()) {
            wrapper.like(RechargeOrder::getOrderNo, orderNo);
        }
        if (userId != null) {
            wrapper.eq(RechargeOrder::getUserId, userId);
        }
        wrapper.orderByDesc(RechargeOrder::getId);
        IPage<RechargeOrder> result = orderMapper.selectPage(p, wrapper);
        return result.convert(this::toVO);
    }

    // ==================== 订单详情 ====================
    @Override
    public OrderAdminVO getOrder(String orderNo) {
        RechargeOrder order = rechargeService.getByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return toVO(order);
    }

    // ==================== 手动补单 ====================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void compensateOrder(String orderNo, Long adminId, String ip) {
        RechargeOrder order = rechargeService.getByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if ("paid".equals(order.getStatus())) {
            throw new BusinessException("订单已支付，无需补单");
        }
        if ("closed".equals(order.getStatus())) {
            throw new BusinessException("订单已关闭，无法补单");
        }

        // 复用掉单补偿逻辑：查支付宝 + 验签 + 加算力 + 改订单状态
        boolean success = rechargeService.compensateOrder(orderNo);

        writeOperationLog(adminId, "COMPENSATE_ORDER", "ORDER", order.getId(),
                "orderNo=" + orderNo, ip, success,
                success ? null : "补单失败（支付宝未支付或验签失败）");

        if (success) {
            log.info("管理员 {} 手动补单成功 orderNo={}", adminId, orderNo);
            return;
        }
        throw new BusinessException("补单失败：支付宝侧未支付或验签失败");
    }

    // ==================== 私有工具方法 ====================

    private OrderAdminVO toVO(RechargeOrder o) {
        return new OrderAdminVO(
                o.getId(),
                o.getOrderNo(),
                o.getUserId(),
                o.getPackageId(),
                o.getPackageName(),
                o.getPoints(),
                o.getAmount(),
                o.getPayMethod(),
                o.getTradeNo(),
                o.getStatus(),
                o.getCreatedAt(),
                o.getUpdatedAt()
        );
    }

    private void writeOperationLog(Long adminId, String operation, String targetType,
                                   Long targetId, String requestParams,
                                   String ip, boolean success, String errorMsg) {
        OperationLog logEntry = new OperationLog();
        logEntry.setOperatorId(adminId);
        logEntry.setOperation(operation);
        logEntry.setTargetType(targetType);
        logEntry.setTargetId(targetId);
        logEntry.setRequestParams(requestParams);
        logEntry.setIp(ip);
        logEntry.setStatus(success ? 1 : 0);
        logEntry.setErrorMsg(errorMsg);
        logEntry.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(logEntry);
    }
}
