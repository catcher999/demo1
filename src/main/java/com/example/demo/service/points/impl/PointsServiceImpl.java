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

        if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            loadPointsToCache(userId);
        }

        Long result = redisTemplate.execute(
                new DefaultRedisScript<>(DEDUCT_SCRIPT, Long.class),
                Collections.singletonList(key),
                String.valueOf(points)
        );

        if (result == 1L) {
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

        if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            loadPointsToCache(userId);
        }

        redisTemplate.execute(
                new DefaultRedisScript<>(REFUND_SCRIPT, Long.class),
                Collections.singletonList(key),
                String.valueOf(points)
        );

        userMapper.update(null,
                new LambdaUpdateWrapper<User>()
                        .eq(User::getId, userId)
                        .setSql("points = points + " + points));
    }

    @Override
    public void addPoints(Long userId, int points) {
        String key = POINTS_KEY + userId;

        // 如果缓存已加载，原子加算力；否则直接改 DB，下次读会重新加载
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            redisTemplate.opsForValue().increment(key, points);
        }

        userMapper.update(null,
                new LambdaUpdateWrapper<User>()
                        .eq(User::getId, userId)
                        .setSql("points = points + " + points));
    }

    @Override
    public int sign(Long userId) {
        LocalDate today = LocalDate.now();
        String signKey = SIGN_KEY + userId + ":" + today;

        Boolean firstSign = redisTemplate.opsForValue()
                .setIfAbsent(signKey, "1", 25, TimeUnit.HOURS);
        if (Boolean.FALSE.equals(firstSign)) {
            return 0;
        }

        User user = userMapper.selectById(userId);
        int streak = 1;
        int reward = 10;

        if (user.getSignDate() != null && user.getSignDate().equals(today.minusDays(1))) {
            streak = user.getSignStreak() + 1;
            reward = 10 + Math.min(streak - 1, 6) * 5;
        }

        userMapper.update(null,
                new LambdaUpdateWrapper<User>()
                        .eq(User::getId, userId)
                        .set(User::getSignDate, today)
                        .set(User::getSignStreak, streak)
                        .setSql("points = points + " + reward));

        String pointsKey = POINTS_KEY + userId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(pointsKey))) {
            redisTemplate.opsForValue().increment(pointsKey, reward);
        }

        return reward;
    }

    private void loadPointsToCache(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            redisTemplate.opsForValue().set(POINTS_KEY + userId, String.valueOf(user.getPoints()));
        }
    }
}
