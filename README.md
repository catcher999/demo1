# AI 创作平台

基于 Spring Boot 3 + MyBatis-Plus 的 AI 内容创作平台，提供用户端 AI 创作 + 算力充值闭环，以及管理端运营管控能力。

## 技术栈

| 层 | 技术 |
|----|------|
| 框架 | Spring Boot 3.x / MyBatis-Plus |
| 数据库 | MySQL 8.x |
| 缓存 | Redis 7.x（Lettuce 客户端） |
| 消息队列 | RabbitMQ |
| 鉴权 | JWT + Spring 拦截器 |
| 密码 | BCryptPasswordEncoder |
| 支付 | Alipay SDK（沙箱） |
| LLM | DeepSeek（策略模式，可切换 OpenAI） |
| 构建 | Maven |

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.x
- Redis 7.x（推荐 Redis Cloud 免费版）
- RabbitMQ 3.x（本地 Docker 容器即可）

### 配置准备

参照 `application.properties` 中注释的 `local` 配置项，在项目根目录创建 `application-local.properties`，填入本机敏感配置：

- DB 密码
- Redis 密码
- JWT 密钥（32 字节 Base64）
- QQ 邮箱账号 + 授权码
- DeepSeek API Key
- 支付宝沙箱私钥

该文件已被 `.gitignore` 忽略，不会提交到 git。

### 数据库初始化

在 IDEA Database 面板或 MySQL 客户端执行：

```sql
CREATE DATABASE IF NOT EXISTS ai_creation DEFAULT CHARSET utf8mb4;
USE ai_creation;

-- 然后按 设计4.md 中"数据库表结构"小节顺序建表
-- 共 10 张表：user / category / artwork / ai_session / ai_task / ai_model
--           recharge_package / recharge_order / points_log / operation_log
```

### 启动

```bash
mvn spring-boot:run
```

启动成功后访问：
- API 文档：http://localhost:8080/swagger-ui.html
- 健康检查：http://localhost:8080/api/gallery/artworks（公开接口）

## 项目结构

```
src/main/java/com/example/demo/
├── controller/        接口层（10 个 Controller，平铺）
├── service/            业务层（8 个模块子包，每个含接口+impl）
├── consumer/           MQ 消费者（TaskConsumer / OrderCloseConsumer）
├── task/               定时任务（OrderCompensateTask 掉单补偿）
├── mapper/             持久层（5 个模块子包）
├── entity/             实体（5 个模块子包）
├── dto/                DTO（6 个模块子包，含 admin/）
├── config/             配置类（7 个）
└── common/             通用层（Result/Jwt/Interceptor/Exception）
```

## 核心模块

### 用户端

| 模块 | 主要接口 |
|------|---------|
| 认证 | 密码登录 / 邮箱验证码登录 / 登出 |
| 用户 | 个人信息 / 修改密码 / 每日签到 |
| 画廊 | 公开作品分页 / 作品详情 / 点赞（Redis 防重复） |
| 任务 | AI 会话 / 提交任务 / 异步生成 / 发布作品 |
| 充值 | 套餐列表 / 创建订单 / 我的订单 / 支付宝回调 |

### 管理端

| 模块 | 主要接口 |
|------|---------|
| 用户管理 | 列表 / 详情 / 修改 / 启禁用 / 调整算力 |
| 套餐管理 | 列表 / 添加 / 修改 / 上下架 |
| 模型管理 | 列表 / 添加 / 修改（含算力单价） / 启禁用 |
| 任务流水 | 全站任务列表 / 详情 |
| 订单管理 | 列表 / 详情 / 手动补单 |

## 鉴权分层

- **用户端**：JwtInterceptor 拦截 `/api/**`，白名单：登录/发验证码/画廊列表/支付回调
- **管理端**：AdminInterceptor 拦截 `/api/admin/**`，校验 JWT 中 `role=admin`
- 复用同一套 JWT 体系，role 字段决定访问权限

## 关键设计

| 设计点 | 方案 |
|--------|------|
| 算力安全 | Lua 原子扣减/退还 + points_log 流水账追溯 |
| 异步任务 | RabbitMQ + 手动 ACK + 失败退算力 + 死信队列 |
| 支付闭环 | 创建订单 → 支付宝二维码 → 异步回调 → 30 分钟延迟关单 → 定时掉单补偿 |
| 限流防刷 | 验证码 60秒/日 5次；任务提交按 IP 10秒 1次 |
| 操作审计 | 高危操作（算力调整/启禁用/补单）写 operation_log 表 |
| LLM 切换 | 策略模式 + @ConditionalOnProperty（llm.provider 控制） |

## 设计文档

- [设计4.md](设计4.md) — 当前版本，详细目录结构 / 数据库表 / API 接口 / 状态机 / 审计规则
- [项目总结报告.md](项目总结报告.md) — 项目完成度统计 / 文件数 / 批次开发回顾

## 工程约定

- Controller 依赖 Service 接口，不依赖实现类
- 异常统一由 GlobalExceptionHandler 处理，返回 Result JSON
- Controller 方法返回 `ResponseEntity.ok()` + Result body，用 Result.code 区分成功失败
- 简单 CRUD 用 MyBatis-Plus Wrapper，复杂 JOIN 用 XML（ArtworkMapper.xml）
- 敏感配置全部存 `application-local.properties`，已加入 `.gitignore`
- 算力变动必须先写 points_log 流水，再改 user.points（同一事务）
- 管理端高危操作必须写 operation_log 审计表

## 待办

- [ ] 联调测试：应用启动 + 接口连通性验证
- [ ] SSE 实时通知：替代前端轮询
- [ ] 算力对账任务：定时 SUM(delta) == user.points 校验

## 备注

- 项目记忆与决策已沉淀至 `project_memory.md`，后续迭代可参考
- IDEA SqlResolve 误报通过 `<!--suppress SqlNoDataSourceInspection, SqlResolve -->` 抑制
