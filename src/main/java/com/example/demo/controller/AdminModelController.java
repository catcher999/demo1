package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.common.Result;
import com.example.demo.dto.admin.CreateModelRequest;
import com.example.demo.dto.admin.ModelAdminVO;
import com.example.demo.dto.admin.UpdateModelRequest;
import com.example.demo.dto.admin.UpdateUserStatusRequest;
import com.example.demo.service.admin.AdminModelService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端 - AI 模型管理 Controller
 * 由 AdminInterceptor 拦截 /api/admin/** 校验 role=admin
 *
 * 接口列表：
 *   GET    /api/admin/models              模型列表（可按 status 过滤）
 *   POST   /api/admin/models              添加模型（status 默认 0=禁用）
 *   PUT    /api/admin/models/{id}         修改模型（含算力单价调整 pointsCost）
 *   PUT    /api/admin/models/{id}/status  启用/禁用模型
 */
@RestController
@RequestMapping("/api/admin/models")
public class AdminModelController {

    private final AdminModelService adminModelService;

    public AdminModelController(AdminModelService adminModelService) {
        this.adminModelService = adminModelService;
    }

    /** 模型列表（可按 status 过滤） */
    @GetMapping
    public ResponseEntity<Result<IPage<ModelAdminVO>>> listModels(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status
    ) {
        IPage<ModelAdminVO> data = adminModelService.listModels(page, size, status);
        return ResponseEntity.ok(Result.success("Models retrieved", data));
    }

    /** 添加模型（status 默认 0=禁用） */
    @PostMapping
    public ResponseEntity<Result<ModelAdminVO>> createModel(
            @Valid @RequestBody CreateModelRequest request,
            @RequestAttribute("currentUserId") Long adminId,
            HttpServletRequest httpRequest
    ) {
        String ip = resolveClientIp(httpRequest);
        ModelAdminVO data = adminModelService.createModel(request, adminId, ip);
        return ResponseEntity.ok(Result.success("Model created", data));
    }

    /** 修改模型（含算力单价 pointsCost 调整） */
    @PutMapping("/{id}")
    public ResponseEntity<Result<ModelAdminVO>> updateModel(
            @PathVariable Long id,
            @Valid @RequestBody UpdateModelRequest request,
            @RequestAttribute("currentUserId") Long adminId,
            HttpServletRequest httpRequest
    ) {
        String ip = resolveClientIp(httpRequest);
        ModelAdminVO data = adminModelService.updateModel(id, request, adminId, ip);
        return ResponseEntity.ok(Result.success("Model updated", data));
    }

    /** 启用/禁用模型 */
    @PutMapping("/{id}/status")
    public ResponseEntity<Result<Void>> updateModelStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request,
            @RequestAttribute("currentUserId") Long adminId,
            HttpServletRequest httpRequest
    ) {
        String ip = resolveClientIp(httpRequest);
        adminModelService.updateModelStatus(id, request, adminId, ip);
        return ResponseEntity.ok(Result.success("Model status updated", null));
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
