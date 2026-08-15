package com.example.demo.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.common.BusinessException;
import com.example.demo.dto.admin.AdjustPointsRequest;
import com.example.demo.dto.admin.UpdateUserRequest;
import com.example.demo.dto.admin.UpdateUserStatusRequest;
import com.example.demo.dto.admin.UserAdminVO;
import com.example.demo.entity.admin.OperationLog;
import com.example.demo.entity.user.User;
import com.example.demo.mapper.admin.OperationLogMapper;
import com.example.demo.mapper.user.UserMapper;
import com.example.demo.service.admin.AdminUserService;
import com.example.demo.service.points.PointsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
public class AdminUserServiceImpl implements AdminUserService {

    private final UserMapper userMapper;
    private final OperationLogMapper operationLogMapper;
    private final PointsService pointsService;

    public AdminUserServiceImpl(UserMapper userMapper,
                                OperationLogMapper operationLogMapper,
                                PointsService pointsService) {
        this.userMapper = userMapper;
        this.operationLogMapper = operationLogMapper;
        this.pointsService = pointsService;
    }

    // ==================== 分页查询用户 ====================
    @Override
    public IPage<UserAdminVO> listUsers(int page, int size, String keyword) {
        Page<User> p = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            // 按 name 或 email 模糊搜索
            wrapper.like(User::getName, keyword)
                    .or()
                    .like(User::getEmail, keyword);
        }
        wrapper.orderByDesc(User::getId); // 按注册顺序倒序
        IPage<User> result = userMapper.selectPage(p, wrapper);
        // 转换为 VO
        return result.convert(this::toVO);
    }

    // ==================== 用户详情 ====================
    @Override
    public UserAdminVO getUserDetail(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return toVO(user);
    }

    // ==================== 修改用户信息 ====================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAdminVO updateUser(Long userId, UpdateUserRequest request, Long adminId, String ip) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 按需更新字段（仅更新非 null 字段）
        boolean changed = false;
        if (request.getName() != null) {
            user.setName(request.getName());
            changed = true;
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
            changed = true;
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
            changed = true;
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
            changed = true;
        }
        if (changed) {
            userMapper.updateById(user);
        }

        // 写审计日志
        writeOperationLog(adminId, "UPDATE_USER", "USER", userId,
                "name=" + request.getName() + ",email=" + request.getEmail()
                        + ",role=" + request.getRole() + ",status=" + request.getStatus(),
                ip, true, null);

        log.info("管理员 {} 更新用户 {} 信息", adminId, userId);
        return toVO(user);
    }

    // ==================== 启用/禁用用户 ====================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long userId, UpdateUserStatusRequest request, Long adminId, String ip) {
        if (!request.isValidStatus()) {
            throw new BusinessException("status 只能是 0 或 1");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        user.setStatus(request.getStatus());
        userMapper.updateById(user);

        // 写审计日志
        String operation = request.getStatus() == 1 ? "ENABLE_USER" : "DISABLE_USER";
        writeOperationLog(adminId, operation, "USER", userId,
                "status=" + request.getStatus(), ip, true, null);

        log.info("管理员 {} {} 用户 {}", adminId, operation, userId);
    }

    // ==================== 调整算力 ====================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int adjustPoints(Long userId, AdjustPointsRequest request, Long adminId, String ip) {
        // 委托 PointsService 写流水 + 改余额（已含事务）
        int balanceAfter = pointsService.adminAdjustPoints(
                userId, request.getDelta(), adminId, request.getRemark());

        // 写审计日志
        writeOperationLog(adminId, "ADJUST_POINTS", "POINTS", userId,
                "delta=" + request.getDelta() + ",remark=" + request.getRemark(),
                ip, true, null);

        log.info("管理员 {} 调整用户 {} 算力 delta={}, 余额={}",
                adminId, userId, request.getDelta(), balanceAfter);
        return balanceAfter;
    }

    // ==================== 私有工具方法 ====================

    /** User → UserAdminVO */
    private UserAdminVO toVO(User user) {
        return new UserAdminVO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getPoints(),
                user.getStatus(),
                user.getSignDate(),
                user.getSignStreak()
        );
    }

    /** 写操作审计日志（同步写，管理员操作低频可接受） */
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
