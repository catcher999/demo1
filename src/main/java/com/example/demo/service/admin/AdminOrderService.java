package com.example.demo.service.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.dto.admin.OrderAdminVO;

/**
 * 管理端 - 订单服务
 * 查询不做审计；手动补单需写 operation_log
 */
public interface AdminOrderService {

    /**
     * 分页查询订单
     * 可按 status（pending/paid/closed/refunded）、orderNo（模糊）、userId 过滤
     */
    IPage<OrderAdminVO> listOrders(int page, int size, String status, String orderNo, Long userId);

    /** 订单详情（按订单号） */
    OrderAdminVO getOrder(String orderNo);

    /**
     * 手动补单：调用支付掉单补偿逻辑（验签+加算力+改订单状态）
     * 写 operation_log 审计
     */
    void compensateOrder(String orderNo, Long adminId, String ip);
}
