package com.example.demo.mapper.admin;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.admin.PointsLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 算力流水 Mapper（单表 CRUD，使用 MBP BaseMapper，无需 XML）
 */
@Mapper
public interface PointsLogMapper extends BaseMapper<PointsLog> {
}
