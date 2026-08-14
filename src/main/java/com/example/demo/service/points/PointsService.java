package com.example.demo.service.points;

/**
 * 算力点数服务
 * 负责：扣减、退还、查询余额、签到
 */
public interface PointsService {

    /**
     * 查询用户算力余额
     */
    int getPoints(Long userId);

    /**
     * 扣减算力（Redis Lua 原子操作，防超扣）
     * @return true=扣减成功 false=余额不足
     */
    boolean deductPoints(Long userId, int points);

    /**
     * 退还算力（任务失败时调用）
     */
    void refundPoints(Long userId, int points);

    /**
     * 充值加算力（支付成功后调用，语义不同于 refund）
     */
    void addPoints(Long userId, int points);

    /**
     * 每日签到，领取算力
     * @return 本次签到获得的点数（0 表示今天已签到）
     */
    int sign(Long userId);
}
