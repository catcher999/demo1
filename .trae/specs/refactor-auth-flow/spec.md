# 认证流程重构 Spec

## Why
当前认证流程存在三个结构问题：没有 GlobalExceptionHandler 导致 BusinessException 抛出后返回 HTML、Mapper XML 目录缺失导致 selectByUsername 无法运行、Service 只返回 token 导致 Controller 承担了数据组装职责。

## 问题解答

### 问题 1：BusinessException 作为异常类正常吗？
**正常。** IntelliJ IDEA 显示"异常类"只是一个分类标签，表示该类继承了 `RuntimeException`，和 `String` 显示 "final 类" 一样，不是错误。`extends RuntimeException` 是 Spring 体系的标准做法，用于区分"业务可预期的错误"和"系统级故障"。BusinessException 类本身不需要修改。

### 问题 2：GlobalExceptionHandler 方案
当前 JwtInterceptor 抛出 BusinessException 后没有任何地方捕获，Spring 默认返回 HTML 错误页。

方案：创建 `@RestControllerAdvice` 类，按异常类型分层捕获：
- `BusinessException` → 业务错误，`Result.error(400, message)`，日志 warn
- `RuntimeException` → 意料外的运行时错误，`Result.error(400, message)`，日志 error + 堆栈
- `Exception` → 兜底，`Result.error(500, "服务器内部错误")`，日志 error + 堆栈

所有返回均用 `ResponseEntity.ok(...)`，HTTP 恒为 200，业务成败由 `Result.code` 区分。

有了 GlobalExceptionHandler 后，Controller 不再需要 try-catch，异常自动被拦截并格式化。

### 问题 3：缺少 Mapper XML 目录
application.properties 配置了 `mapper-locations=classpath*:mapper/**/*.xml`，但 `resources/mapper/` 目录不存在。UserMapper 声明了自定义方法 `selectByUsername`，该方法不在 BaseMapper 中，必须有 XML 实现。

方案：创建 `src/main/resources/mapper/UserMapper.xml`，namespace 指向 UserMapper，实现 `selectByUsername` 的 SQL。

### 问题 4：Service 返回 LoginResponse
你的理解完全正确。当前流程：
```
Service 返回 token(String) → Controller 自己组装 LoginResponse（硬编码 role、expiresIn）
```

修正后：
```
Service 组装 LoginResponse → Controller 只做 Result + ResponseEntity 包装
```

这样 Controller 只负责 HTTP 层面的包装，业务数据组装归 Service。login 和 register 都改为返回 LoginResponse。

## What Changes
- 新增 `GlobalExceptionHandler`（`@RestControllerAdvice`），捕获 BusinessException / RuntimeException / Exception
- AuthController 移除 try-catch，异常交给 GlobalExceptionHandler
- AuthService 接口 login / register 返回值由 `String` 改为 `LoginResponse`
- AuthServiceImpl 负责组装 LoginResponse（token, userId, username, role, expiresIn）
- AuthController 不再构造 LoginResponse
- 创建 `src/main/resources/mapper/UserMapper.xml`，实现 selectByUsername
- AuthServiceImpl 的 login / register 查询改用 `selectByUsername` 替代 selectOne + wrapper

## Impact
- Affected code: AuthServiceImpl, AuthService, AuthController, UserMapper
- New files: GlobalExceptionHandler.java, resources/mapper/UserMapper.xml

---

## ADDED Requirements

### Requirement: GlobalExceptionHandler
系统 SHALL 提供全局异常处理器，统一捕获所有异常并转为 `Result` JSON 格式返回。

#### Scenario: BusinessException 被捕获
- **WHEN** 任意层抛出 BusinessException
- **THEN** 返回 `ResponseEntity.ok(Result.error(400, message))`，HTTP 200

#### Scenario: 未知 Exception 被捕获
- **WHEN** 抛出非 BusinessException 的 Exception
- **THEN** 返回 `ResponseEntity.ok(Result.error(500, "服务器内部错误"))`，HTTP 200
- **AND** 日志记录完整堆栈，但不暴露给前端

### Requirement: Mapper XML
系统 SHALL 提供 UserMapper.xml，实现 selectByUsername 方法。

#### Scenario: selectByUsername 调用
- **WHEN** AuthServiceImpl 调用 `userMapper.selectByUsername(username)`
- **THEN** 执行 `SELECT * FROM user WHERE name = #{username}`，返回 User 实体

---

## MODIFIED Requirements

### Requirement: Service 层返回 LoginResponse
AuthService.login 和 register 的返回值由 `String`（token）改为 `LoginResponse`。Service 负责组装完整响应对象，Controller 只做包装。

#### Scenario: 登录成功
- **WHEN** 用户登录成功
- **THEN** AuthService.login 返回 LoginResponse（含 token, userId, username, role, expiresIn）
- **AND** Controller 包装为 `ResponseEntity.ok(Result.success("Login successful", loginResponse))`

#### Scenario: 注册成功
- **WHEN** 用户注册成功
- **THEN** AuthService.register 返回 LoginResponse
- **AND** Controller 包装为 `ResponseEntity.ok(Result.success("Register successful", loginResponse))`

#### Scenario: Controller 无 try-catch
- **WHEN** Service 抛出异常
- **THEN** GlobalExceptionHandler 自动捕获，Controller 方法体内无 try-catch
