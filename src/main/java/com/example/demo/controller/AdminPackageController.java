package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.common.Result;
import com.example.demo.dto.admin.CreatePackageRequest;
import com.example.demo.dto.admin.PackageAdminVO;
import com.example.demo.dto.admin.UpdatePackageRequest;
import com.example.demo.dto.admin.UpdateUserStatusRequest;
import com.example.demo.service.admin.AdminPackageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端 - 充值套餐管理 Controller
 * 由 AdminInterceptor 拦截 /api/admin/** 校验 role=admin
 *
 * 接口列表：
 *   GET    /api/admin/packages              套餐列表（含下架的，可按 status 过滤）
 *   POST   /api/admin/packages              添加套餐（status 默认 0=下架）
 *   PUT    /api/admin/packages/{id}         修改套餐（按需传字段）
 *   PUT    /api/admin/packages/{id}/status   上架/下架套餐
 */
@RestController
@RequestMapping("/api/admin/packages")
public class AdminPackageController {

    private final AdminPackageService adminPackageService;

    public AdminPackageController(AdminPackageService adminPackageService) {
        this.adminPackageService = adminPackageService;
    }

    /** 套餐列表（含下架的，可按 status 过滤） */
    @GetMapping
    public ResponseEntity<Result<IPage<PackageAdminVO>>> listPackages(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status
    ) {
        IPage<PackageAdminVO> data = adminPackageService.listPackages(page, size, status);
        return ResponseEntity.ok(Result.success("Packages retrieved", data));
    }

    /** 添加套餐（添加不等于上架，status 默认 0=下架，需调上架接口才生效） */
    @PostMapping
    public ResponseEntity<Result<PackageAdminVO>> createPackage(
            @Valid @RequestBody CreatePackageRequest request,
            @RequestAttribute("currentUserId") Long adminId,
            HttpServletRequest httpRequest
    ) {
        String ip = resolveClientIp(httpRequest);
        PackageAdminVO data = adminPackageService.createPackage(request, adminId, ip);
        return ResponseEntity.ok(Result.success("Package created", data));
    }

    /** 修改套餐（按需传字段，未传的不动） */
    @PutMapping("/{id}")
    public ResponseEntity<Result<PackageAdminVO>> updatePackage(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePackageRequest request,
            @RequestAttribute("currentUserId") Long adminId,
            HttpServletRequest httpRequest
    ) {
        String ip = resolveClientIp(httpRequest);
        PackageAdminVO data = adminPackageService.updatePackage(id, request, adminId, ip);
        return ResponseEntity.ok(Result.success("Package updated", data));
    }

    /** 上架/下架套餐 */
    @PutMapping("/{id}/status")
    public ResponseEntity<Result<Void>> updatePackageStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request,
            @RequestAttribute("currentUserId") Long adminId,
            HttpServletRequest httpRequest
    ) {
        String ip = resolveClientIp(httpRequest);
        adminPackageService.updatePackageStatus(id, request, adminId, ip);
        return ResponseEntity.ok(Result.success("Package status updated", null));
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
