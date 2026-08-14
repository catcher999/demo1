package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.common.Result;
import com.example.demo.dto.recharge.CreateOrderRequest;
import com.example.demo.dto.recharge.PaymentResponse;
import com.example.demo.dto.recharge.RechargeOrderVO;
import com.example.demo.dto.recharge.RechargePackageVO;
import com.example.demo.service.recharge.RechargeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 充值模块 Controller
 *
 * 接口列表：
 *   GET  /api/recharge/packages              套餐列表（需登录）
 *   POST /api/recharge/orders                创建订单，返回支付链接（需登录）
 *   GET  /api/recharge/orders/{orderNo}      查询订单详情（需登录）
 *   GET  /api/recharge/orders                我的订单列表（需登录）
 *   POST /api/recharge/notify                支付宝异步回调（放行，无需登录）
 *   GET  /api/recharge/orders/{orderNo}/query 主动查询支付宝（掉单补偿，需登录）
 */
@RestController
@RequestMapping("/api/recharge")
@Slf4j
public class RechargeController {

    private final RechargeService rechargeService;

    public RechargeController(RechargeService rechargeService) {
        this.rechargeService = rechargeService;
    }

    /** 套餐列表 */
    @GetMapping("/packages")
    public ResponseEntity<Result<List<RechargePackageVO>>> listPackages() {
        return ResponseEntity.ok(Result.success(rechargeService.listPackages()));
    }

    /** 创建订单，返回支付表单 HTML */
    @PostMapping("/orders")
    public ResponseEntity<Result<PaymentResponse>> createOrder(
            @RequestAttribute("currentUserId") Long userId,
            @Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(Result.success(rechargeService.createOrder(userId, request)));
    }

    /** 查询订单详情 */
    @GetMapping("/orders/{orderNo}")
    public ResponseEntity<Result<RechargeOrderVO>> getOrder(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable String orderNo) {
        return ResponseEntity.ok(Result.success(rechargeService.getOrder(userId, orderNo)));
    }

    /** 我的订单列表（分页） */
    @GetMapping("/orders")
    public ResponseEntity<Result<IPage<RechargeOrderVO>>> listOrders(
            @RequestAttribute("currentUserId") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(Result.success(rechargeService.listOrders(userId, page, size)));
    }

    /**
     * 支付宝异步回调
     * 支付宝要求返回 "success" 字符串（纯文本，不是 JSON）
     */
    @PostMapping("/notify")
    public String notify(HttpServletRequest request) {
        // 1. 把所有参数转成 Map<String, String>
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                params.put(key, values[0]);
            }
        });

        // 2. 交给 service 处理
        return rechargeService.handleNotify(params);
    }

    /**
     * 支付宝同步返回（用户支付后跳回）
     * 这里只做日志记录，实际状态以异步回调为准
     */
    @GetMapping("/return")
    public ResponseEntity<Result<String>> returnCallback(HttpServletRequest request) {
        String orderNo = request.getParameter("out_trade_no");
        log.info("支付宝同步返回，orderNo={}", orderNo);
        return ResponseEntity.ok(Result.success("支付完成，正在处理中"));
    }

    /**
     * 主动查询支付宝订单状态（掉单补偿，用户也可手动触发）
     */
    @GetMapping("/orders/{orderNo}/query")
    public ResponseEntity<Result<Boolean>> queryOrder(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable String orderNo) {
        // 先校验订单归属权
        rechargeService.getOrder(userId, orderNo);
        boolean compensated = rechargeService.compensateOrder(orderNo);
        return ResponseEntity.ok(Result.success(compensated));
    }
}
