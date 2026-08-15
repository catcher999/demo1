package com.example.demo.service.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.dto.admin.CreatePackageRequest;
import com.example.demo.dto.admin.PackageAdminVO;
import com.example.demo.dto.admin.UpdatePackageRequest;
import com.example.demo.dto.admin.UpdateUserStatusRequest;

/**
 * 管理端 - 充值套餐服务
 * 所有写操作同步写 operation_log 审计
 */
public interface AdminPackageService {

    /** 分页查询所有套餐（含下架的） */
    IPage<PackageAdminVO> listPackages(int page, int size, Integer status);

    /** 添加套餐（status 默认 0=下架，需调 updateStatus 上架） */
    PackageAdminVO createPackage(CreatePackageRequest request, Long adminId, String ip);

    /** 修改套餐（按需传字段，未传的不动） */
    PackageAdminVO updatePackage(Long id, UpdatePackageRequest request, Long adminId, String ip);

    /** 上架/下架套餐 */
    void updatePackageStatus(Long id, UpdateUserStatusRequest request, Long adminId, String ip);
}
