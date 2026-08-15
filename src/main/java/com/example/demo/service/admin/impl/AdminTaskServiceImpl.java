package com.example.demo.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.BusinessException;
import com.example.demo.dto.admin.TaskAdminVO;
import com.example.demo.entity.task.Task;
import com.example.demo.mapper.task.TaskMapper;
import com.example.demo.service.admin.AdminTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AdminTaskServiceImpl implements AdminTaskService {

    private final TaskMapper taskMapper;

    public AdminTaskServiceImpl(TaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    // ==================== 分页查询全站任务流水 ====================
    @Override
    public IPage<TaskAdminVO> listTasks(int page, int size, String status, Long userId) {
        Page<Task> p = new Page<>(page, size);
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(Task::getStatus, status);
        }
        if (userId != null) {
            wrapper.eq(Task::getUserId, userId);
        }
        wrapper.orderByDesc(Task::getId);
        IPage<Task> result = taskMapper.selectPage(p, wrapper);
        return result.convert(this::toVO);
    }

    // ==================== 任务详情 ====================
    @Override
    public TaskAdminVO getTask(Long id) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        return toVO(task);
    }

    // ==================== 私有工具方法 ====================

    private TaskAdminVO toVO(Task t) {
        return new TaskAdminVO(
                t.getId(),
                t.getSessionId(),
                t.getUserId(),
                t.getPrompt(),
                t.getDescription(),
                t.getStatus(),
                t.getResultUrl(),
                t.getErrorMessage(),
                t.getArtworkId(),
                t.getPointsCost(),
                t.getModel(),
                t.getQueuedAt(),
                t.getFinishedAt(),
                t.getCreatedAt()
        );
    }
}
