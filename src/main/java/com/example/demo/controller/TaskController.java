package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.common.Result;
import com.example.demo.dto.task.CreateTaskRequest;
import com.example.demo.dto.task.PublishTaskRequest;
import com.example.demo.dto.task.UpdatePreferenceRequest;
import com.example.demo.dto.task.SessionVO;
import com.example.demo.dto.task.TaskVO;
import com.example.demo.entity.gallery.Artwork;
import com.example.demo.service.task.AiSessionService;
import com.example.demo.service.task.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final AiSessionService aiSessionService;
    private final TaskService taskService;

    public TaskController(AiSessionService aiSessionService, TaskService taskService) {
        this.aiSessionService = aiSessionService;
        this.taskService = taskService;
    }

    // ==================== 会话相关 ====================

    /** 创建会话 */
    @PostMapping("/sessions")
    public ResponseEntity<Result<SessionVO>> createSession(
            @RequestAttribute("currentUserId") Long userId
    ) {
        SessionVO data = aiSessionService.createSession(userId);
        return ResponseEntity.ok(Result.success("Session created", data));
    }

    /** 我的会话列表 */
    @GetMapping("/sessions")
    public ResponseEntity<Result<IPage<SessionVO>>> listSessions(
            @RequestAttribute("currentUserId") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        IPage<SessionVO> data = aiSessionService.listSessions(userId, page, size);
        return ResponseEntity.ok(Result.success("Sessions retrieved", data));
    }

    /** 更新会话风格偏好 */
    @PutMapping("/sessions/{id}/preference")
    public ResponseEntity<Result<Void>> updatePreference(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody UpdatePreferenceRequest req
    ) {
        aiSessionService.updatePreference(id, userId, req.getPreference());
        return ResponseEntity.ok(Result.success("Preference updated", null));
    }

    // ==================== 任务相关 ====================

    /** 提交生成请求 */
    @PostMapping
    public ResponseEntity<Result<TaskVO>> createTask(
            @RequestAttribute("currentUserId") Long userId,
            @Valid @RequestBody CreateTaskRequest req
    ) {
        TaskVO data = taskService.createTask(userId, req);
        return ResponseEntity.ok(Result.success("Task created", data));
    }

    /** 确认描述，开始生成图片 */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<Result<TaskVO>> confirmTask(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long id
    ) {
        TaskVO data = taskService.confirmTask(id, userId);
        return ResponseEntity.ok(Result.success("Image generated", data));
    }

    /** 取消任务（拒绝描述） */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Result<Void>> cancelTask(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long id
    ) {
        taskService.cancelTask(id, userId);
        return ResponseEntity.ok(Result.success("Task cancelled", null));
    }

    /** 发布任务成果到画廊 */
    @PostMapping("/{id}/publish")
    public ResponseEntity<Result<Artwork>> publishTask(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody PublishTaskRequest req
    ) {
        Artwork data = taskService.publishTask(id, userId, req);
        return ResponseEntity.ok(Result.success("Artwork published", data));
    }

    /** 查询任务详情 */
    @GetMapping("/{id}")
    public ResponseEntity<Result<TaskVO>> getTask(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long id
    ) {
        TaskVO data = taskService.getTask(id, userId);
        return ResponseEntity.ok(Result.success("Task retrieved", data));
    }

    /** 查询任务列表（可按 session 过滤） */
    @GetMapping
    public ResponseEntity<Result<IPage<TaskVO>>> listTasks(
            @RequestAttribute("currentUserId") Long userId,
            @RequestParam(required = false) Long sessionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        IPage<TaskVO> data = taskService.listTasks(userId, sessionId, page, size);
        return ResponseEntity.ok(Result.success("Tasks retrieved", data));
    }
}
