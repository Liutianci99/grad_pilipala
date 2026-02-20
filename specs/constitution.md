# 项目宪法：电商物流管理系统

## 核心原则

### I. 简单优先
- 不过度设计，先让功能跑起来再优化
- YAGNI（You Aren't Gonna Need It）— 不写用不到的代码
- 毕设项目，够用就好，不追求企业级架构

### II. 统一风格
- 后端：Spring Boot 3.2 + MyBatis-Plus，统一返回 `Result<T>`
- 前端：Vue 3 组合式 API（`<script setup>`），统一用 `design.css` 共享样式
- 异常处理：`GlobalExceptionHandler` 统一捕获，Controller 不写 try-catch
- 请求：前端统一走 `@/utils/request.js`（axios 封装 + JWT 拦截）

### III. 规范先行
- 新功能必须先写规范（spec），Boss 审核通过后再写代码
- 规范聚焦 WHAT 和 WHY，不纠结 HOW
- 用 `[待确认]` 标记不确定的地方，不要猜

### IV. 分支管理
- 每个功能一个分支：`feat/xxx`、`fix/xxx`
- 只有 `main` 触发 CI/CD（GitHub Actions → GHCR → 服务器自动部署）
- 合并用 `--no-ff`，保留合并记录

### V. 安全底线
- 密码必须 BCrypt 加密
- 敏感配置走环境变量，不硬编码
- JWT token 验证，401 自动登出
- 不泄露堆栈信息给前端

## 技术栈

| 层 | 技术 | 版本 |
|---|------|------|
| 后端 | Spring Boot + MyBatis-Plus | 3.2 |
| 前端 | Vue 3 + Vite | 3.x |
| 数据库 | MySQL | 8.0 |
| 部署 | Docker + GitHub Actions + GHCR | - |
| 服务器 | Ubuntu VM（腾讯云） | - |

## 治理
- 本宪法优先于所有其他约定
- 修改宪法需要 Boss 同意
- 所有规范和代码必须符合本宪法

**版本**: 1.0 | **创建**: 2026-02-20
