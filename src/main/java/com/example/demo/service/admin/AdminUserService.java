package com.example.demo.service.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.dto.admin.AdjustPointsRequest;
import com.example.demo.dto.admin.UpdateUserRequest;
import com.example.demo.dto.admin.UpdateUserStatusRequest;
import com.example.demo.dto.admin.UserAdminVO;

/**
 * 管理端用户服务
 * 所有写操作记录 operation_log 审计
 */
public interface AdminUserService {

    /**
     * 分页查询用户（可按 name/email 模糊搜索）
     */
    IPage<UserAdminVO> listUsers(int page, int size, String keyword);

    /**
     * 查询用户详情
     */
    UserAdminVO getUserDetail(Long userId);

    /**
     * 修改用户信息（name/email/role/status 按需传）
     * 写 operation_log
     */
    UserAdminVO updateUser(Long userId, UpdateUserRequest request, Long adminId, String ip);

    /**
     * 启用/禁用用户
     * 写 operation_log
     */
    void updateUserStatus(Long userId, UpdateUserStatusRequest request, Long adminId, String ip);

    /**
     * 管理员手动调整算力（委托 PointsService.adminAdjustPoints）
     * 写 operation_log
     * @return 调整后余额
     */
    int adjustPoints(Long userId, AdjustPointsRequest request, Long adminId, String ip);
}
