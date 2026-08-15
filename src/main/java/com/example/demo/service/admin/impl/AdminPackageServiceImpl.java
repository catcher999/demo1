package com.example.demo.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.BusinessException;
import com.example.demo.dto.admin.CreatePackageRequest;
import com.example.demo.dto.admin.PackageAdminVO;
import com.example.demo.dto.admin.UpdatePackageRequest;
import com.example.demo.dto.admin.UpdateUserStatusRequest;
import com.example.demo.entity.admin.OperationLog;
import com.example.demo.entity.recharge.RechargePackage;
import com.example.demo.mapper.admin.OperationLogMapper;
import com.example.demo.mapper.recharge.RechargePackageMapper;
import com.example.demo.service.admin.AdminPackageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;

@Service
@Slf4j
public class AdminPackageServiceImpl implements AdminPackageService {

    private final RechargePackageMapper packageMapper;
    private final OperationLogMapper operationLogMapper;

    public AdminPackageServiceImpl(RechargePackageMapper packageMapper,
                                   OperationLogMapper operationLogMapper) {
        this.packageMapper = packageMapper;
        this.operationLogMapper = operationLogMapper;
    }

    // ==================== 分页查询套餐 ====================
    @Override
    public IPage<PackageAdminVO> listPackages(int page, int size, Integer status) {
        Page<RechargePackage> p = new Page<>(page, size);
        LambdaQueryWrapper<RechargePackage> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(RechargePackage::getStatus, status);
        }
        wrapper.orderByDesc(RechargePackage::getId);
        IPage<RechargePackage> result = packageMapper.selectPage(p, wrapper);
        return result.convert(this::toVO);
    }

    // ==================== 添加套餐 ====================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PackageAdminVO createPackage(CreatePackageRequest request, Long adminId, String ip) {
        RechargePackage pkg = new RechargePackage();
        pkg.setName(request.getName());
        pkg.setPoints(request.getPoints());
        pkg.setPrice(request.getPrice());
        pkg.setType(request.getType() == null ? "once" : request.getType());
        pkg.setStatus(0); // 添加不等于上架，默认下架
        pkg.setCreatedAt(new Date());
        pkg.setUpdatedAt(new Date());
        packageMapper.insert(pkg);

        writeOperationLog(adminId, "CREATE_PACKAGE", "PACKAGE", pkg.getId(),
                "name=" + request.getName() + ",points=" + request.getPoints()
                        + ",price=" + request.getPrice() + ",type=" + request.getType(),
                ip, true, null);

        log.info("管理员 {} 添加套餐 {}（id={}, status=0 下架）", adminId, pkg.getName(), pkg.getId());
        return toVO(pkg);
    }

    // ==================== 修改套餐 ====================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PackageAdminVO updatePackage(Long id, UpdatePackageRequest request, Long adminId, String ip) {
        RechargePackage pkg = packageMapper.selectById(id);
        if (pkg == null) {
            throw new BusinessException("套餐不存在");
        }

        boolean changed = false;
        if (request.getName() != null) {
            pkg.setName(request.getName());
            changed = true;
        }
        if (request.getPoints() != null) {
            pkg.setPoints(request.getPoints());
            changed = true;
        }
        if (request.getPrice() != null) {
            pkg.setPrice(request.getPrice());
            changed = true;
        }
        if (request.getType() != null) {
            pkg.setType(request.getType());
            changed = true;
        }
        if (changed) {
            pkg.setUpdatedAt(new Date());
            packageMapper.updateById(pkg);
        }

        writeOperationLog(adminId, "UPDATE_PACKAGE", "PACKAGE", id,
                "name=" + request.getName() + ",points=" + request.getPoints()
                        + ",price=" + request.getPrice() + ",type=" + request.getType(),
                ip, true, null);

        log.info("管理员 {} 修改套餐 {} 信息", adminId, id);
        return toVO(pkg);
    }

    // ==================== 上架/下架 ====================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePackageStatus(Long id, UpdateUserStatusRequest request, Long adminId, String ip) {
        if (!request.isValidStatus()) {
            throw new BusinessException("status 只能是 0 或 1");
        }
        RechargePackage pkg = packageMapper.selectById(id);
        if (pkg == null) {
            throw new BusinessException("套餐不存在");
        }

        pkg.setStatus(request.getStatus());
        pkg.setUpdatedAt(new Date());
        packageMapper.updateById(pkg);

        String operation = request.getStatus() == 1 ? "LIST_PACKAGE" : "DELIST_PACKAGE";
        writeOperationLog(adminId, operation, "PACKAGE", id,
                "status=" + request.getStatus(), ip, true, null);

        log.info("管理员 {} {} 套餐 {}", adminId, operation, id);
    }

    // ==================== 私有工具方法 ====================

    private PackageAdminVO toVO(RechargePackage pkg) {
        return new PackageAdminVO(
                pkg.getId(),
                pkg.getName(),
                pkg.getPoints(),
                pkg.getPrice(),
                pkg.getType(),
                pkg.getStatus(),
                pkg.getCreatedAt(),
                pkg.getUpdatedAt()
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
