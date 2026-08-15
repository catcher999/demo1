package com.example.demo.service.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.dto.admin.TaskAdminVO;

/**
 * 管理端 - 任务流水服务
 * 任务管理只做查询（任务流转由 TaskService 内部处理），故无审计写入
 */
public interface AdminTaskService {

    /**
     * 分页查询全站任务流水
     * 可按 status（pending/waiting_confirm/queued/running/succeeded/failed/cancelled）和 userId 过滤
     */
    IPage<TaskAdminVO> listTasks(int page, int size, String status, Long userId);

    /** 任务详情 */
    TaskAdminVO getTask(Long id);
}
