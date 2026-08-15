package com.example.demo.service.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.dto.admin.CreateModelRequest;
import com.example.demo.dto.admin.ModelAdminVO;
import com.example.demo.dto.admin.UpdateModelRequest;
import com.example.demo.dto.admin.UpdateUserStatusRequest;

/**
 * 管理端 - AI 模型服务
 * 所有写操作同步写 operation_log 审计
 */
public interface AdminModelService {

    /** 分页查询所有模型 */
    IPage<ModelAdminVO> listModels(int page, int size, Integer status);

    /** 添加模型 */
    ModelAdminVO createModel(CreateModelRequest request, Long adminId, String ip);

    /** 修改模型（含算力单价调整 pointsCost） */
    ModelAdminVO updateModel(Long id, UpdateModelRequest request, Long adminId, String ip);

    /** 启用/禁用模型 */
    void updateModelStatus(Long id, UpdateUserStatusRequest request, Long adminId, String ip);
}
