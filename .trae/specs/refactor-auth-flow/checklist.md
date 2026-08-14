# Checklist

- [ ] GlobalExceptionHandler 已创建，标注 @RestControllerAdvice + @Slf4j
- [ ] GlobalExceptionHandler 包含 BusinessException / RuntimeException / Exception 三层处理
- [ ] GlobalExceptionHandler 所有返回均使用 ResponseEntity.ok(...)，HTTP 恒为 200
- [ ] GlobalExceptionHandler 的 Exception 兜底处理对外只返回"服务器内部错误"，不暴露堆栈
- [ ] AuthController login / register 方法已移除 try-catch
- [ ] AuthController 不再自行构造 LoginResponse
- [ ] AuthController login 方法体仅为一行 service 调用 + 一行包装返回
- [ ] AuthService 接口 login / register 返回类型为 LoginResponse
- [ ] AuthServiceImpl.login 组装 LoginResponse 时 userId 来自数据库查询结果（非 null）
- [ ] AuthServiceImpl.login 组装 LoginResponse 时 expiresIn 来自 jwt.expire 配置值（非硬编码）
- [ ] AuthServiceImpl.register 组装 LoginResponse 时 userId 来自插入后的实体（非 null）
- [ ] AuthServiceImpl 的 login 和 register 中查询用户改用 userMapper.selectByUsername()
- [ ] resources/mapper/UserMapper.xml 已创建，namespace 正确
- [ ] UserMapper.xml 中 selectByUsername 的 SQL 为 SELECT * FROM user WHERE name = #{username}
