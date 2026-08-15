package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.common.Result;
import com.example.demo.dto.admin.TaskAdminVO;
import com.example.demo.service.admin.AdminTaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端 - 任务流水管理 Controller
 * 由 AdminInterceptor 拦截 /api/admin/** 校验 role=admin
 *
 * 接口列表：
 *   GET  /api/admin/tasks            任务列表（可按 status/userId 过滤）
 *   GET  /api/admin/tasks/{id}       任务详情
 *
 * 注：任务流水只做查询，任务状态流转由 TaskService 内部处理
 */
@RestController
@RequestMapping("/api/admin/tasks")
public class AdminTaskController {

    private final AdminTaskService adminTaskService;

    public AdminTaskController(AdminTaskService adminTaskService) {
        this.adminTaskService = adminTaskService;
    }

    /** 全站任务流水列表（可按 status/userId 过滤） */
    @GetMapping
    public ResponseEntity<Result<IPage<TaskAdminVO>>> listTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userId
    ) {
        IPage<TaskAdminVO> data = adminTaskService.listTasks(page, size, status, userId);
        return ResponseEntity.ok(Result.success("Tasks retrieved", data));
    }

    /** 任务详情 */
    @GetMapping("/{id}")
    public ResponseEntity<Result<TaskAdminVO>> getTask(@PathVariable Long id) {
        TaskAdminVO data = adminTaskService.getTask(id);
        return ResponseEntity.ok(Result.success("Task retrieved", data));
    }
}
