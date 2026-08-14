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
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
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

    /**
     * Lua 脚本：原子性检查每日发送次数 + 自增
     * 返回 1=允许发送 0=已达上限
     * key 不存在时初始化为 1 并设置 TTL
     */
    private static final String DAILY_COUNT_SCRIPT =
            "local current = redis.call('get', KEYS[1]) " +
            "if current == nil then " +
            "  redis.call('set', KEYS[1], '1', 'EX', ARGV[2]) " +
            "  return 1 " +
            "end " +
            "if tonumber(current) >= tonumber(ARGV[1]) then return 0 end " +
            "redis.call('incr', KEYS[1]) " +
            "return 1";

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
        User user = findByEmail(email);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            throw new BusinessException("请使用邮箱验证码登录，或先设置密码");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException("密码错误");
        }

        return buildLoginResponse(user, email);
    }

    @Override
    public void sendCode(String email) {
        // 1. 防刷：60 秒频率限制
        String limitKey = CODE_LIMIT_KEY + email;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(limitKey))) {
            throw new BusinessException("请求过于频繁，请 60 秒后再试");
        }

        // 2. 防刷：每日发送次数限制（Lua 原子操作，防并发突破上限）
        String dailyKey = CODE_DAILY_KEY + email;
        Long allowed = redisTemplate.execute(
                new DefaultRedisScript<>(DAILY_COUNT_SCRIPT, Long.class),
                Collections.singletonList(dailyKey),
                String.valueOf(DAILY_LIMIT),
                String.valueOf(CODE_DAILY_TTL.toSeconds())
        );
        // Lua 脚本始终返回数值（0 或 1），不会为 null
        if (allowed != null && allowed == 0L) {
            throw new BusinessException("今日验证码发送次数已达上限（" + DAILY_LIMIT + " 次）");
        }

        // 3. 生成 6 位数字验证码
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));

        // 4. 先发邮件（失败则抛异常，不写 Redis，用户可立即重试）
        mailService.sendVerificationCode(email, code);

        // 5. 邮件发送成功后，存验证码到 Redis
        redisTemplate.opsForValue().set(CODE_KEY_PREFIX + email, code, CODE_TTL);

        // 6. 记录 60 秒防刷标记
        redisTemplate.opsForValue().set(limitKey, "1", CODE_LIMIT_TTL);
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
