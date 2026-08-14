# Tasks

- [ ] Task 1: 新建 GlobalExceptionHandler
  - [ ] 1.1 创建 `common/GlobalExceptionHandler.java`，标注 `@RestControllerAdvice` + `@Slf4j`
  - [ ] 1.2 `@ExceptionHandler(BusinessException.class)` → `ResponseEntity.ok(Result.error(400, e.getMessage()))`，日志 warn
  - [ ] 1.3 `@ExceptionHandler(RuntimeException.class)` → `ResponseEntity.ok(Result.error(400, e.getMessage()))`，日志 error + 堆栈
  - [ ] 1.4 `@ExceptionHandler(Exception.class)` → `ResponseEntity.ok(Result.error(500, "服务器内部错误"))`，日志 error + 堆栈

- [ ] Task 2: 创建 Mapper XML
  - [ ] 2.1 创建 `src/main/resources/mapper/UserMapper.xml`
  - [ ] 2.2 namespace 为 `com.example.demo.mapper.UserMapper`
  - [ ] 2.3 实现 selectByUsername：`SELECT * FROM user WHERE name = #{username}`

- [ ] Task 3: AuthService 接口返回值改为 LoginResponse
  - [ ] 3.1 `String login(...)` → `LoginResponse login(...)`
  - [ ] 3.2 `String register(...)` → `LoginResponse register(...)`
  - [ ] 3.3 `String logout()` 保持不变
  - [ ] 3.4 添加 LoginResponse import

- [ ] Task 4: AuthServiceImpl 实现组装 LoginResponse
  - [ ] 4.1 login 返回类型改为 LoginResponse，组装 token/userId/username/role/expiresIn 并返回
  - [ ] 4.2 register 返回类型改为 LoginResponse，组装并返回
  - [ ] 4.3 login 和 register 中查询用户改用 `userMapper.selectByUsername(username)` 替代 selectOne + wrapper

- [ ] Task 5: AuthController 移除 try-catch，简化为纯转发
  - [ ] 5.1 login 移除 try-catch，直接 `ResponseEntity.ok(Result.success("Login successful", authService.login(...)))`
  - [ ] 5.2 register 移除 try-catch，直接 `ResponseEntity.ok(Result.success("Register successful", authService.register(...)))`
  - [ ] 5.3 移除 LoginResponse import 和构造逻辑

# Task Dependencies
- [Task 1] 独立，可先行
- [Task 2] 独立，可先行
- [Task 3] 独立，可先行
- [Task 4] 依赖 [Task 2]（改用 selectByUsername）和 [Task 3]（接口改完才改实现）
- [Task 5] 依赖 [Task 4]（Service 返回 LoginResponse 后 Controller 才能简化）
