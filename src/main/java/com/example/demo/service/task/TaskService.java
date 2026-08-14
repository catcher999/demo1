package com.example.demo.service.task;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.dto.task.CreateTaskRequest;
import com.example.demo.dto.task.PublishTaskRequest;
import com.example.demo.dto.task.TaskVO;
import com.example.demo.entity.gallery.Artwork;

public interface TaskService {

    /**
     * 提交生成请求：
     * 1. 校验同 session 下无 waiting_confirm 任务（并发控制）
     * 2. 调 DeepSeek 生成结构化描述
     * 3. 回填会话标题（若为空）
     * @param userId  当前用户 ID
     * @param request {sessionId, prompt}
     * @return 任务 VO（含 description，状态 waiting_confirm）
     */
    TaskVO createTask(Long userId, CreateTaskRequest request);

    /**
     * 确认描述，开始生成图片
     * @param taskId 任务 ID
     * @param userId 当前用户 ID（用于鉴权）
     * @return 任务 VO（状态 running → succeeded/failed）
     */
    TaskVO confirmTask(Long taskId, Long userId);

    /**
     * 取消任务（拒绝描述）
     * @param taskId 任务 ID
     * @param userId 当前用户 ID（用于鉴权）
     */
    void cancelTask(Long taskId, Long userId);

    /**
     * 发布任务成果到画廊
     * @param taskId  任务 ID
     * @param userId  当前用户 ID（用于鉴权）
     * @param request {title, categoryId?, isPublic?}
     * @return 新建的 Artwork
     */
    Artwork publishTask(Long taskId, Long userId, PublishTaskRequest request);

    /**
     * 查询单个任务详情
     * @param taskId 任务 ID
     * @param userId 当前用户 ID（用于鉴权）
     */
    TaskVO getTask(Long taskId, Long userId);

    /**
     * 查询任务列表（可按 session 过滤）
     * @param userId    当前用户 ID
     * @param sessionId 可选，传则只查该会话的任务
     * @param page      页码
     * @param size      每页大小
     */
    IPage<TaskVO> listTasks(Long userId, Long sessionId, int page, int size);
}
