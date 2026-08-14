package com.example.demo.service.task.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.BusinessException;
import com.example.demo.dto.task.SessionVO;
import com.example.demo.entity.task.AiSession;
import com.example.demo.mapper.task.AiSessionMapper;
import com.example.demo.service.task.AiSessionService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiSessionServiceImpl implements AiSessionService {

    private final AiSessionMapper aiSessionMapper;

    public AiSessionServiceImpl(AiSessionMapper aiSessionMapper) {
        this.aiSessionMapper = aiSessionMapper;
    }

    @Override
    public SessionVO createSession(Long userId) {
        AiSession session = new AiSession();
        session.setUserId(userId);
        // 标题留空，等首个任务生成时由 AI 回填
        aiSessionMapper.insert(session);
        return toVO(session);
    }

    @Override
    public IPage<SessionVO> listSessions(Long userId, int page, int size) {
        Page<AiSession> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<AiSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiSession::getUserId, userId)
                .orderByDesc(AiSession::getUpdatedAt);

        IPage<AiSession> result = aiSessionMapper.selectPage(pageObj, wrapper);

        // 转成 VO
        List<SessionVO> voList = result.getRecords().stream()
                .map(this::toVO)
                .toList();
        Page<SessionVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public void updatePreference(Long sessionId, Long userId, String preference) {
        AiSession session = getByIdAndUserId(sessionId, userId);
        session.setPreference(preference);
        aiSessionMapper.updateById(session);
    }

    /** 内部方法：更新会话标题（由 TaskService 在首个任务生成后调用） */
    @Override
    public void updateSessionTitle(AiSession session) {
        aiSessionMapper.updateById(session);
    }

    /** 查询会话并校验归属权，不存在或不属于该用户则抛异常 */
    @Override
    public AiSession getByIdAndUserId(Long sessionId, Long userId) {
        LambdaQueryWrapper<AiSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiSession::getId, sessionId)
                .eq(AiSession::getUserId, userId);
        AiSession session = aiSessionMapper.selectOne(wrapper);
        if (session == null) {
            throw new BusinessException("会话不存在或无权访问");
        }
        return session;
    }

    private SessionVO toVO(AiSession session) {
        return new SessionVO(
                session.getId(),
                session.getTitle(),
                session.getPreference(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}
