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
 * 充值订单表
 * 对应数据库 recharge_order 表
 *
 * 状态流转：pending → paid / closed / refunded
 * pending：已创建未支付
 * paid：支付成功
 * closed：30分钟未支付自动关闭
 * refunded：已退款
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("recharge_order")
public class RechargeOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号（唯一，业务生成） */
    private String orderNo;

    /** 用户ID */
    private Long userId;

    /** 套餐ID */
    private Long packageId;

    /** 套餐名称（冗余字段，避免套餐改名影响历史订单） */
    private String packageName;

    /** 购买的算力点数 */
    private Integer points;

    /** 实际支付金额（元） */
    private BigDecimal amount;

    /** 支付方式：alipay / wechat（第一版只支持 alipay） */
    private String payMethod;

    /** 支付宝交易号（回调后回填） */
    private String tradeNo;

    /** 订单状态：pending / paid / closed / refunded */
    private String status;

    private Date createdAt;

    private Date updatedAt;
}
