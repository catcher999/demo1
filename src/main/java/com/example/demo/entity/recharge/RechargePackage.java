package com.example.demo.entity.recharge;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 充值套餐表
 * 对应数据库 recharge_package 表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("recharge_package")
public class RechargePackage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 套餐名称（如：100算力包、500算力包、月卡） */
    private String name;

    /** 赠送算力点数 */
    private Integer points;

    /** 价格（单位：元） */
    private BigDecimal price;

    /** 套餐类型：once 一次性 / monthly 月卡 */
    private String type;

    /** 状态：1 启用 / 0 下架 */
    private Integer status;

    private Date createdAt;

    private Date updatedAt;
}
