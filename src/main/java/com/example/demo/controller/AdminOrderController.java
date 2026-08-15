package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.common.Result;
import com.example.demo.dto.admin.OrderAdminVO;
import com.example.demo.service.admin.AdminOrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端 - 订单管理 Controller
 * 由 AdminInterceptor 拦截 /api/admin/** 校验 role=admin
 *
 * 接口列表：
 *   GET   /api/admin/orders                       订单列表（可按 status/orderNo/userId 过滤）
 *   GET   /api/admin/orders/{orderNo}             订单详情
 *   POST  /api/admin/orders/{orderNo}/compensate  手动补单
 *
 * 注：列表/详情查询不做审计；手动补单写 operation_log
 */
@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    public AdminOrderController(AdminOrderService adminOrderService) {
        this.adminOrderService = adminOrderService;
    }

    /** 订单列表（可按 status/orderNo/userId 过滤） */
    @GetMapping
    public ResponseEntity<Result<IPage<OrderAdminVO>>> listOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long userId
    ) {
        IPage<OrderAdminVO> data = adminOrderService.listOrders(page, size, status, orderNo, userId);
        return ResponseEntity.ok(Result.success("Orders retrieved", data));
    }

    /** 订单详情 */
    @GetMapping("/{orderNo}")
    public ResponseEntity<Result<OrderAdminVO>> getOrder(@PathVariable String orderNo) {
        OrderAdminVO data = adminOrderService.getOrder(orderNo);
        return ResponseEntity.ok(Result.success("Order retrieved", data));
    }

    /** 手动补单（调用支付掉单补偿逻辑，加审计） */
    @PostMapping("/{orderNo}/compensate")
    public ResponseEntity<Result<Void>> compensateOrder(
            @PathVariable String orderNo,
            @RequestAttribute("currentUserId") Long adminId,
            HttpServletRequest httpRequest
    ) {
        String ip = resolveClientIp(httpRequest);
        adminOrderService.compensateOrder(orderNo, adminId, ip);
        return ResponseEntity.ok(Result.success("Order compensated successfully", null));
    }

    /** 从请求头解析客户端真实 IP（兼容反向代理） */
    private String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        ip = request.getHeader("Proxy-Client-IP");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }
}
