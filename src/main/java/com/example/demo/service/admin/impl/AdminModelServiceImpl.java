package com.example.demo.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.BusinessException;
import com.example.demo.dto.admin.CreateModelRequest;
import com.example.demo.dto.admin.ModelAdminVO;
import com.example.demo.dto.admin.UpdateModelRequest;
import com.example.demo.dto.admin.UpdateUserStatusRequest;
import com.example.demo.entity.admin.OperationLog;
import com.example.demo.entity.task.AiModel;
import com.example.demo.mapper.admin.OperationLogMapper;
import com.example.demo.mapper.task.AiModelMapper;
import com.example.demo.service.admin.AdminModelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
public class AdminModelServiceImpl implements AdminModelService {

    private final AiModelMapper aiModelMapper;
    private final OperationLogMapper operationLogMapper;

    public AdminModelServiceImpl(AiModelMapper aiModelMapper,
                                 OperationLogMapper operationLogMapper) {
        this.aiModelMapper = aiModelMapper;
        this.operationLogMapper = operationLogMapper;
    }

    // ==================== 分页查询模型 ====================
    @Override
    public IPage<ModelAdminVO> listModels(int page, int size, Integer status) {
        Page<AiModel> p = new Page<>(page, size);
        LambdaQueryWrapper<AiModel> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(AiModel::getStatus, status);
        }
        wrapper.orderByDesc(AiModel::getId);
        IPage<AiModel> result = aiModelMapper.selectPage(p, wrapper);
        return result.convert(this::toVO);
    }

    // ==================== 添加模型 ====================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelAdminVO createModel(CreateModelRequest request, Long adminId, String ip) {
        AiModel model = new AiModel();
        model.setName(request.getName());
        model.setDisplayName(request.getDisplayName());
        model.setPointsCost(request.getPointsCost());
        model.setStatus(0); // 默认禁用，需调启用接口才生效
        aiModelMapper.insert(model);

        writeOperationLog(adminId, "CREATE_MODEL", "MODEL", model.getId(),
                "name=" + request.getName() + ",displayName=" + request.getDisplayName()
                        + ",pointsCost=" + request.getPointsCost(),
                ip, true, null);

        log.info("管理员 {} 添加模型 {}（id={}, status=0 禁用）", adminId, model.getName(), model.getId());
        return toVO(model);
    }

    // ==================== 修改模型（含算力单价） ====================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelAdminVO updateModel(Long id, UpdateModelRequest request, Long adminId, String ip) {
        AiModel model = aiModelMapper.selectById(id);
        if (model == null) {
            throw new BusinessException("模型不存在");
        }

        boolean changed = false;
        if (request.getName() != null) {
            model.setName(request.getName());
            changed = true;
        }
        if (request.getDisplayName() != null) {
            model.setDisplayName(request.getDisplayName());
            changed = true;
        }
        if (request.getPointsCost() != null) {
            model.setPointsCost(request.getPointsCost());
            changed = true;
        }
        if (changed) {
            aiModelMapper.updateById(model);
        }

        writeOperationLog(adminId, "UPDATE_MODEL", "MODEL", id,
                "name=" + request.getName() + ",displayName=" + request.getDisplayName()
                        + ",pointsCost=" + request.getPointsCost(),
                ip, true, null);

        log.info("管理员 {} 修改模型 {}（含算力单价调整）", adminId, id);
        return toVO(model);
    }

    // ==================== 启用/禁用模型 ====================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateModelStatus(Long id, UpdateUserStatusRequest request, Long adminId, String ip) {
        if (!request.isValidStatus()) {
            throw new BusinessException("status 只能是 0 或 1");
        }
        AiModel model = aiModelMapper.selectById(id);
        if (model == null) {
            throw new BusinessException("模型不存在");
        }

        model.setStatus(request.getStatus());
        aiModelMapper.updateById(model);

        String operation = request.getStatus() == 1 ? "ENABLE_MODEL" : "DISABLE_MODEL";
        writeOperationLog(adminId, operation, "MODEL", id,
                "status=" + request.getStatus(), ip, true, null);

        log.info("管理员 {} {} 模型 {}", adminId, operation, id);
    }

    // ==================== 私有工具方法 ====================

    private ModelAdminVO toVO(AiModel model) {
        return new ModelAdminVO(
                model.getId(),
                model.getName(),
                model.getDisplayName(),
                model.getPointsCost(),
                model.getStatus()
        );
    }

    private void writeOperationLog(Long adminId, String operation, String targetType,
                                   Long targetId, String requestParams,
                                   String ip, boolean success, String errorMsg) {
        OperationLog logEntry = new OperationLog();
        logEntry.setOperatorId(adminId);
        logEntry.setOperation(operation);
        logEntry.setTargetType(targetType);
        logEntry.setTargetId(targetId);
        logEntry.setRequestParams(requestParams);
        logEntry.setIp(ip);
        logEntry.setStatus(success ? 1 : 0);
        logEntry.setErrorMsg(errorMsg);
        logEntry.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(logEntry);
    }
}
