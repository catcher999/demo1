package com.example.demo.entity.admin;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 算力流水账实体
 * 每次 points 变动（充值/任务扣/退/签到/管理员调整）都写一条
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("points_log")
public class PointsLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 变动值，正=增加，负=扣减 */
    private Integer delta;

    /** 变动后余额（用于回放还原任意时刻余额） */
    private Integer balanceAfter;

    /** 来源：recharge / task_cost / refund / sign / admin_adj */
    private String source;

    /** 操作人ID（admin_adj 时记录管理员；其他来源为 null） */
    private Long operatorId;

    private String remark;

    private LocalDateTime createdAt;
}
