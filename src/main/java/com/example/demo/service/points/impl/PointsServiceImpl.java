package com.example.demo.service.points.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.demo.entity.user.User;
import com.example.demo.mapper.user.UserMapper;
import com.example.demo.service.points.PointsService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Service
public class PointsServiceImpl implements PointsService {

    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;

    public PointsServiceImpl(UserMapper userMapper, StringRedisTemplate redisTemplate) {
        this.userMapper = userMapper;
        this.redisTemplate = redisTemplate;
    }

    /** Redis Key：用户算力余额 */
    private static final String POINTS_KEY = "user:points:";

    /** Redis Key：签到缓存（当日有效，防并发重复签到） */
    private static final String SIGN_KEY = "user:sign:";

    /**
     * Lua 脚本：原子性检查+扣减
     * 返回 1=成功 0=余额不足 -1=key不存在（需从DB加载）
     */
    private static final String DEDUCT_SCRIPT =
            "local current = redis.call('get', KEYS[1]) " +
            "if current == nil then return -1 end " +
            "if tonumber(current) < tonumber(ARGV[1]) then return 0 end " +
            "redis.call('decrby', KEYS[1], ARGV[1]) " +
            "return 1";

    /**
     * Lua 脚本：原子性退还（INCRBY）
     */
    private static final String REFUND_SCRIPT =
            "local current = redis.call('get', KEYS[1]) " +
            "if current == nil then return -1 end " +
            "redis.call('incrby', KEYS[1], ARGV[1]) " +
            "return 1";

    @Override
    public int getPoints(Long userId) {
        String key = POINTS_KEY + userId;
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            // 缓存未加载，从 DB 读取并缓存
            User user = userMapper.selectById(userId);
            if (user == null) {
                return 0;
            }
            redisTemplate.opsForValue().set(key, String.valueOf(user.getPoints()));
            return user.getPoints();
        }
        return Integer.parseInt(value);
    }

    @Override
    public boolean deductPoints(Long userId, int points) {
        String key = POINTS_KEY + userId;

        // 第一次调用时，缓存可能未加载，先预热
        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            loadPointsToCache(userId);
        }

        Long result = redisTemplate.execute(
                new DefaultRedisScript<>(DEDUCT_SCRIPT, Long.class),
                Collections.singletonList(key),
                String.valueOf(points)
        );

        if (result != null && result == 1L) {
            // 同步扣减 DB
            new LambdaUpdateWrapper<User>()
                    .eq(User::getId, userId)
                    .setSql("points = points - " + points);
            userMapper.update(null,
                    new LambdaUpdateWrapper<User>()
                            .eq(User::getId, userId)
                            .setSql("points = points - " + points));
            return true;
        }
        return false;
    }

    @Override
    public void refundPoints(Long userId, int points) {
        String key = POINTS_KEY + userId;

        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            loadPointsToCache(userId);
        }

        redisTemplate.execute(
                new DefaultRedisScript<>(REFUND_SCRIPT, Long.class),
                Collections.singletonList(key),
                String.valueOf(points)
        );

        // 同步退还 DB
        userMapper.update(null,
                new LambdaUpdateWrapper<User>()
                        .eq(User::getId, userId)
                        .setSql("points = points + " + points));
    }

    @Override
    public int sign(Long userId) {
        LocalDate today = LocalDate.now();
        String signKey = SIGN_KEY + userId + ":" + today;

        // 用 setIfAbsent 保证当天只能签一次（防并发重复签到）
        Boolean firstSign = redisTemplate.opsForValue()
                .setIfAbsent(signKey, "1", 25, TimeUnit.HOURS);
        if (Boolean.FALSE.equals(firstSign)) {
            return 0; // 今天已签到
        }

        // 查用户上次签到信息
        User user = userMapper.selectById(userId);
        int streak = 1;
        int reward = 10; // 基础奖励

        // 连续签到加成：昨天签了 → 连续 +1；否则重置为 1
        if (user.getSignDate() != null && user.getSignDate().equals(today.minusDays(1))) {
            streak = user.getSignStreak() + 1;
            reward = 10 + Math.min(streak - 1, 6) * 5; // 第2天+5, 第3天+10... 最高 +30
        }

        // 更新用户：签到日期、连续天数、算力
        userMapper.update(null,
                new LambdaUpdateWrapper<User>()
                        .eq(User::getId, userId)
                        .set(User::getSignDate, today)
                        .set(User::getSignStreak, streak)
                        .setSql("points = points + " + reward));

        // 同步更新 Redis 余额缓存
        String pointsKey = POINTS_KEY + userId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(pointsKey))) {
            redisTemplate.opsForValue().increment(pointsKey, reward);
        }

        return reward;
    }

    /** 从 DB 加载算力余额到 Redis 缓存 */
    private void loadPointsToCache(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            redisTemplate.opsForValue().set(POINTS_KEY + userId, String.valueOf(user.getPoints()));
        }
    }
}
