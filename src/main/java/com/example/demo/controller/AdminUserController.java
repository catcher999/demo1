package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.common.Result;
import com.example.demo.dto.admin.AdjustPointsRequest;
import com.example.demo.dto.admin.UpdateUserRequest;
import com.example.demo.dto.admin.UpdateUserStatusRequest;
import com.example.demo.dto.admin.UserAdminVO;
import com.example.demo.service.admin.AdminUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理端 - 用户管理 Controller
 * 由 AdminInterceptor 拦截 /api/admin/** 校验 role=admin
 *
 * 接口列表：
 *   GET    /api/admin/users              分页查询用户（可按 name/email 模糊搜索）
 *   GET    /api/admin/users/{id}         用户详情
 *   PUT    /api/admin/users/{id}         修改用户信息（合并字段，按需传）
 *   PUT    /api/admin/users/{id}/status  启用/禁用用户
 *   POST   /api/admin/users/{id}/points  手动调整算力（delta 可正可负）
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /** 分页查询用户（可按 name/email 模糊搜索） */
    @GetMapping
    public ResponseEntity<Result<IPage<UserAdminVO>>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword
    ) {
        IPage<UserAdminVO> data = adminUserService.listUsers(page, size, keyword);
        return ResponseEntity.ok(Result.success("Users retrieved", data));
    }

    /** 用户详情 */
    @GetMapping("/{id}")
    public ResponseEntity<Result<UserAdminVO>> getUserDetail(@PathVariable Long id) {
        UserAdminVO data = adminUserService.getUserDetail(id);
        return ResponseEntity.ok(Result.success("User detail", data));
    }

    /** 修改用户信息（合并字段，按需传 name/email/role/status） */
    @PutMapping("/{id}")
    public ResponseEntity<Result<UserAdminVO>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            @RequestAttribute("currentUserId") Long adminId,
            HttpServletRequest httpRequest
    ) {
        String ip = resolveClientIp(httpRequest);
        UserAdminVO data = adminUserService.updateUser(id, request, adminId, ip);
        return ResponseEntity.ok(Result.success("User updated", data));
    }

    /** 启用/禁用用户 */
    @PutMapping("/{id}/status")
    public ResponseEntity<Result<Void>> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request,
            @RequestAttribute("currentUserId") Long adminId,
            HttpServletRequest httpRequest
    ) {
        String ip = resolveClientIp(httpRequest);
        adminUserService.updateUserStatus(id, request, adminId, ip);
        return ResponseEntity.ok(Result.success("Status updated", null));
    }

    /** 手动调整算力（delta 可正可负，正=补偿，负=扣除） */
    @PostMapping("/{id}/points")
    public ResponseEntity<Result<Map<String, Integer>>> adjustPoints(
            @PathVariable Long id,
            @Valid @RequestBody AdjustPointsRequest request,
            @RequestAttribute("currentUserId") Long adminId,
            HttpServletRequest httpRequest
    ) {
        String ip = resolveClientIp(httpRequest);
        int balance = adminUserService.adjustPoints(id, request, adminId, ip);
        return ResponseEntity.ok(Result.success("Points adjusted",
                Map.of("balance", balance)));
    }

    /**
     * 从请求头解析客户端真实 IP（兼容反向代理）
     * 与 TaskController 逻辑一致，不抽公共类避免过度设计
     */
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
