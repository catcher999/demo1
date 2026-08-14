package com.example.demo.service.task;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.dto.task.SessionVO;

public interface AiSessionService {

    /**
     * 创建会话（标题由首个任务生成时回填）
     * @param userId 创建者 ID
     * @return 会话 VO
     */
    SessionVO createSession(Long userId);

    /**
     * 查询我的会话列表（分页）
     */
    IPage<SessionVO> listSessions(Long userId, int page, int size);

    /**
     * 更新会话风格偏好
     * @param sessionId   会话 ID
     * @param userId      创建者 ID（用于鉴权）
     * @param preference  新的偏好
     */
    void updatePreference(Long sessionId, Long userId, String preference);
}
