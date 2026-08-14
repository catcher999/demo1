package com.example.demo.service.auth.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.common.BusinessException;
import com.example.demo.common.JwtUtil;
import com.example.demo.dto.auth.LoginResponse;
import com.example.demo.entity.user.User;
import com.example.demo.mapper.user.UserMapper;
import com.example.demo.service.auth.AuthService;
import com.example.demo.service.mail.MailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;
import java.time.Duration;

@Service
public class AuthServiceImpl implements AuthService {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final MailService mailService;
    private final StringRedisTemplate redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${jwt.expire}")
    private long expire;

    /** 验证码在 Redis 中的 key 前缀：verify_code:{email} */
    private static final String CODE_KEY_PREFIX = "verify_code:";

    /** 验证码有效期 5 分钟 */
    private static final Duration CODE_TTL = Duration.ofMinutes(5);

    /** 防刷：发送频率限制 key（60 秒内不能重发） */
    private static final String CODE_LIMIT_KEY = "code_limit:";

    /** 防刷：每日发送次数限制 key（每天最多 5 次） */
    private static final String CODE_DAILY_KEY = "code_daily:";

    private static final Duration CODE_LIMIT_TTL = Duration.ofSeconds(60);
    private static final Duration CODE_DAILY_TTL = Duration.ofHours(24);
    private static final int DAILY_LIMIT = 5;

    public AuthServiceImpl(UserMapper userMapper,
                           JwtUtil jwtUtil,
                           MailService mailService,
                           StringRedisTemplate redisTemplate) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.mailService = mailService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public LoginResponse login(String email, String password) {
        // 1. 按邮箱查询用户
        User user = findByEmail(email);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 2. 用户尚未设置密码（仅通过验证码自动注册的账号）
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            throw new BusinessException("请使用邮箱验证码登录，或先设置密码");
        }

        // 3. BCrypt 校验密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException("密码错误");
        }

        // 4. 生成 token（subject 用 email，name 可能为空）
        return buildLoginResponse(user, email);
    }

    @Override
    public void sendCode(String email) {
        // 0. 防刷：60 秒频率限制
        String limitKey = CODE_LIMIT_KEY + email;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(limitKey))) {
            throw new BusinessException("请求过于频繁，请 60 秒后再试");
        }

        // 0.1 防刷：每日发送次数限制
        String dailyKey = CODE_DAILY_KEY + email;
        String dailyCount = redisTemplate.opsForValue().get(dailyKey);
        if (dailyCount != null && Integer.parseInt(dailyCount) >= DAILY_LIMIT) {
            throw new BusinessException("今日验证码发送次数已达上限（" + DAILY_LIMIT + " 次）");
        }

        // 1. 生成 6 位数字验证码
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));

        // 2. 存入 Redis，5 分钟过期
        redisTemplate.opsForValue().set(CODE_KEY_PREFIX + email, code, CODE_TTL);

        // 3. 发送邮件
        mailService.sendVerificationCode(email, code);

        // 4. 记录防刷标记
        redisTemplate.opsForValue().set(limitKey, "1", CODE_LIMIT_TTL);
        if (dailyCount == null) {
            redisTemplate.opsForValue().set(dailyKey, "1", CODE_DAILY_TTL);
        } else {
            redisTemplate.opsForValue().increment(dailyKey);
        }
    }

    @Override
    public LoginResponse emailLogin(String email, String code) {
        // 1. 从 Redis 读取验证码
        String cached = redisTemplate.opsForValue().get(CODE_KEY_PREFIX + email);
        if (cached == null || !cached.equals(code)) {
            throw new BusinessException("验证码错误或已过期");
        }

        // 2. 校验通过后立即删除，防止重放
        redisTemplate.delete(CODE_KEY_PREFIX + email);

        // 3. 查询用户；不存在则自动注册（name 留空待用户设置）
        User user = findByEmail(email);
        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setName(null);
            user.setPassword(null);
            user.setRole("user");
            userMapper.insert(user);
        }

        // 4. 生成 token
        return buildLoginResponse(user, email);
    }

    @Override
    public void logout() {
        // 暂无黑名单机制
    }

    /** 按 email 查询用户，返回 null 表示不存在 */
    private User findByEmail(String email) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, email);
        return userMapper.selectOne(wrapper);
    }

    /** 组装 LoginResponse，token 的 subject 统一用 email */
    private LoginResponse buildLoginResponse(User user, String email) {
        String token = jwtUtil.generateToken(email, user.getRole(), user.getId());
        return new LoginResponse(
                token,
                user.getId(),
                user.getName(),
                user.getRole(),
                expire
        );
    }
}
