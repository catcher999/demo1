package com.example.demo.mapper.admin;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.admin.OperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作审计 Mapper（单表 CRUD，使用 MBP BaseMapper，无需 XML）
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
