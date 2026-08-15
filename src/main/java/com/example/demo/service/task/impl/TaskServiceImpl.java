package com.example.demo.service.task.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.BusinessException;
import com.example.demo.config.RabbitMQConfig;
import com.example.demo.dto.task.CreateTaskRequest;
import com.example.demo.dto.task.PublishTaskRequest;
import com.example.demo.dto.task.TaskMessage;
import com.example.demo.dto.task.TaskVO;
import com.example.demo.entity.gallery.Artwork;
import com.example.demo.entity.task.AiModel;
import com.example.demo.entity.task.AiSession;
import com.example.demo.entity.task.Task;
import com.example.demo.mapper.gallery.ArtworkMapper;
import com.example.demo.mapper.task.AiModelMapper;
import com.example.demo.mapper.task.TaskMapper;
import com.example.demo.service.points.PointsService;
import com.example.demo.service.task.AiSessionService;
import com.example.demo.service.task.LlmService;
import com.example.demo.service.task.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class TaskServiceImpl implements TaskService {

    private final TaskMapper taskMapper;
    private final ArtworkMapper artworkMapper;
    private final AiSessionService aiSessionService;
    private final LlmService llmService;
    private final StringRedisTemplate redisTemplate;
    private final PointsService pointsService;
    private final AiModelMapper aiModelMapper;
    private final RabbitTemplate rabbitTemplate;

    /** 图片生成模型标识（当前 Mock，后续可换 SD/Pollinations） */
    private static final String IMAGE_MODEL = "mock-image";

    /** 任务提交限流 key：task_limit:ip:{ip}，同一 IP 10 秒内只能提交一次 */
    private static final String TASK_LIMIT_KEY = "task_limit:ip:";
    private static final Duration TASK_LIMIT_TTL = Duration.ofSeconds(10);

    public TaskServiceImpl(TaskMapper taskMapper,
                           ArtworkMapper artworkMapper,
                           AiSessionService aiSessionService,
                           LlmService llmService,
                           StringRedisTemplate redisTemplate,
                           PointsService pointsService,
                           AiModelMapper aiModelMapper,
                           RabbitTemplate rabbitTemplate) {
        this.taskMapper = taskMapper;
        this.artworkMapper = artworkMapper;
        this.aiSessionService = aiSessionService;
        this.llmService = llmService;
        this.redisTemplate = redisTemplate;
        this.pointsService = pointsService;
        this.aiModelMapper = aiModelMapper;
        this.rabbitTemplate = rabbitTemplate;
    }

    // ==================== 提交生成请求 ====================
    @Override
    public TaskVO createTask(Long userId, String ip, CreateTaskRequest request) {
        // 0. 接口防刷：同一 IP 10 秒内只能提交一次
        String limitKey = TASK_LIMIT_KEY + ip;
        if (redisTemplate.hasKey(limitKey)) {
            throw new BusinessException("请求过于频繁，请 10 秒后再试");
        }

        // 1. 校验会话归属权
        AiSession session = aiSessionService.getByIdAndUserId(request.getSessionId(), userId);

        // 2. 并发控制：同会话下不能有 waiting_confirm 的任务
        checkNoWaitingConfirm(request.getSessionId(), userId);

        // 3. 创建任务（pending）
        Task task = new Task();
        task.setSessionId(request.getSessionId());
        task.setUserId(userId);
        task.setPrompt(request.getPrompt());
        task.setStatus("pending");
        taskMapper.insert(task);

        // 3.1 设置防刷标记（10 秒）
        redisTemplate.opsForValue().set(limitKey, "1", TASK_LIMIT_TTL);

        // 4. 组装上下文历史（同会话下 succeeded 的任务，按时间顺序）
        List<String> history = buildHistory(request.getSessionId());

        // 5. 调 LLM 生成描述
        String aiResponse;
        try {
            aiResponse = llmService.generateDescription(
                    request.getPrompt(),
                    session.getPreference(),
                    history
            );
        } catch (BusinessException e) {
            // AI 调用失败，标记任务为 failed
            task.setStatus("failed");
            task.setErrorMessage(e.getMessage());
            taskMapper.updateById(task);
            throw e;
        }

        // 6. 解析 AI 返回：拆出标题和描述
        String[] parts = parseAiResponse(aiResponse);
        String title = parts[0];
        String description = parts[1];

        // 7. 更新任务
        task.setDescription(description);
        task.setStatus("waiting_confirm");
        taskMapper.updateById(task);

        // 8. 回填会话标题（若为空）
        if (session.getTitle() == null || session.getTitle().isBlank()) {
            session.setTitle(title);
            aiSessionService.updateSessionTitle(session);
        }

        return toVO(task);
    }

    // ==================== 确认生成图片（异步推 MQ） ====================
    @Override
    public TaskVO confirmTask(Long taskId, Long userId) {
        Task task = getByIdAndUserId(taskId, userId);

        // 1. 校验状态
        if (!"waiting_confirm".equals(task.getStatus())) {
            throw new BusinessException("当前任务状态不允许确认，状态：" + task.getStatus());
        }

        // 2. 查询模型算力消耗
        AiModel aiModel = getEnabledModel(IMAGE_MODEL);
        int pointsCost = aiModel.getPointsCost();

        // 3. 扣算力（Lua 原子操作，余额不足抛异常）
        boolean ok = pointsService.deductPoints(userId, pointsCost);
        if (!ok) {
            throw new BusinessException("算力不足，需要 " + pointsCost + " 点，请充值");
        }

        // 4. 更新任务：queued 状态 + 记录 points_cost / model / queued_at
        Date now = new Date();
        task.setStatus("queued");
        task.setPointsCost(pointsCost);
        task.setModel(IMAGE_MODEL);
        task.setQueuedAt(now);
        taskMapper.updateById(task);

        // 5. 推消息到 task.exchange（异步生成）
        TaskMessage message = new TaskMessage(
                task.getId(),
                userId,
                task.getDescription(),
                IMAGE_MODEL
        );
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TASK_EXCHANGE,
                RabbitMQConfig.TASK_ROUTING_KEY,
                message
        );
        log.info("任务已推入队列，taskId={}, userId={}, model={}", taskId, userId, IMAGE_MODEL);

        return toVO(task);
    }

    // ==================== 取消任务 ====================
    @Override
    public void cancelTask(Long taskId, Long userId) {
        Task task = getByIdAndUserId(taskId, userId);
        String status = task.getStatus();

        // waiting_confirm 或 queued 都可取消
        if (!"waiting_confirm".equals(status) && !"queued".equals(status)) {
            throw new BusinessException("当前任务状态不允许取消，状态：" + status);
        }

        // 如果是 queued 状态取消，说明已扣算力但消费者未消费，退还款项
        if ("queued".equals(status) && task.getPointsCost() != null && task.getPointsCost() > 0) {
            pointsService.refundPoints(userId, task.getPointsCost());
            log.info("queued 任务取消，退还算力 {} 点，taskId={}", task.getPointsCost(), taskId);
        }

        task.setStatus("cancelled");
        taskMapper.updateById(task);
    }

    // ==================== 发布到画廊 ====================
    @Override
    public Artwork publishTask(Long taskId, Long userId, PublishTaskRequest request) {
        Task task = getByIdAndUserId(taskId, userId);

        // 1. 校验状态
        if (!"succeeded".equals(task.getStatus())) {
            throw new BusinessException("只有成功的任务才能发布，当前状态：" + task.getStatus());
        }
        if (task.getArtworkId() != null) {
            throw new BusinessException("该任务已发布，不能重复发布");
        }

        // 2. 组装 Artwork
        Artwork artwork = new Artwork();
        artwork.setTitle(request.getTitle());
        artwork.setDescription(task.getDescription());
        artwork.setImageUrl(task.getResultUrl());
        artwork.setUserId(task.getUserId());
        artwork.setHeatScore(0);
        artwork.setDate(new Date());
        artwork.setCategoryId(request.getCategoryId());
        // isPublic 默认 false（除非用户明确指定）
        artwork.setIsPublic(Boolean.TRUE.equals(request.getIsPublic()));

        // 3. 插入 artwork 表
        artworkMapper.insert(artwork);

        // 4. 回填 task.artworkId
        task.setArtworkId(artwork.getId());
        taskMapper.updateById(task);

        return artwork;
    }

    // ==================== 查询任务详情 ====================
    @Override
    public TaskVO getTask(Long taskId, Long userId) {
        Task task = getByIdAndUserId(taskId, userId);
        return toVO(task);
    }

    // ==================== 查询任务列表 ====================
    @Override
    public IPage<TaskVO> listTasks(Long userId, Long sessionId, int page, int size) {
        Page<Task> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getUserId, userId);
        if (sessionId != null) {
            wrapper.eq(Task::getSessionId, sessionId);
        }
        wrapper.orderByDesc(Task::getCreatedAt);

        IPage<Task> result = taskMapper.selectPage(pageObj, wrapper);

        List<TaskVO> voList = result.getRecords().stream()
                .map(this::toVO)
                .toList();
        Page<TaskVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    // ==================== 内部方法 ====================

    /** 并发控制：检查同会话下是否有 waiting_confirm 的任务 */
    private void checkNoWaitingConfirm(Long sessionId, Long userId) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getSessionId, sessionId)
                .eq(Task::getUserId, userId)
                .eq(Task::getStatus, "waiting_confirm");
        Long count = taskMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException("当前有未确认的任务，请先确认或取消");
        }
    }

    /** 组装上下文历史：同会话下 succeeded 的任务，按时间顺序，返回 [u1, a1, u2, a2, ...] */
    private List<String> buildHistory(Long sessionId) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getSessionId, sessionId)
                .eq(Task::getStatus, "succeeded")
                .orderByAsc(Task::getCreatedAt);
        List<Task> succeededTasks = taskMapper.selectList(wrapper);

        List<String> history = new ArrayList<>();
        for (Task t : succeededTasks) {
            history.add(t.getPrompt());         // user
            history.add(t.getDescription());    // assistant
        }
        return history;
    }

    /** 解析 AI 返回，拆出标题和描述。格式：【标题】xxx\n---\n描述正文 */
    private String[] parseAiResponse(String aiResponse) {
        String[] result = new String[2];
        String title;
        String description;

        int separatorIndex = aiResponse.indexOf("---");
        if (separatorIndex > 0) {
            String titlePart = aiResponse.substring(0, separatorIndex).trim();
            // 去掉【标题】前缀
            if (titlePart.startsWith("【标题】")) {
                title = titlePart.substring(4).trim();
            } else {
                title = titlePart;
            }
            description = aiResponse.substring(separatorIndex + 3).trim();
        } else {
            // AI 没按格式返回，兜底处理
            title = aiResponse.length() > 10 ? aiResponse.substring(0, 10) : aiResponse;
            description = aiResponse;
        }

        result[0] = title;
        result[1] = description;
        return result;
    }

    /** 查询任务并校验归属权 */
    private Task getByIdAndUserId(Long taskId, Long userId) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getId, taskId)
                .eq(Task::getUserId, userId);
        Task task = taskMapper.selectOne(wrapper);
        if (task == null) {
            throw new BusinessException("任务不存在或无权访问");
        }
        return task;
    }

    /** 查询启用的 AI 模型配置 */
    private AiModel getEnabledModel(String modelName) {
        LambdaQueryWrapper<AiModel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiModel::getName, modelName)
                .eq(AiModel::getStatus, 1);
        AiModel model = aiModelMapper.selectOne(wrapper);
        if (model == null) {
            throw new BusinessException("模型未启用或不存在：" + modelName);
        }
        return model;
    }

    private TaskVO toVO(Task task) {
        return new TaskVO(
                task.getId(),
                task.getSessionId(),
                task.getPrompt(),
                task.getDescription(),
                task.getStatus(),
                task.getResultUrl(),
                task.getErrorMessage(),
                task.getArtworkId(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
