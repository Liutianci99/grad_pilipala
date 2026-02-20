# 功能规范：全局异常处理器

**分支**: `feat/global-exception-handler`
**创建日期**: 2026-02-20
**状态**: 审核中

## 用户场景

### 用户故事 1 — 业务错误返回友好提示（优先级：P1）

用户操作触发业务错误（如删除不存在的用户、用户名重复），前端收到统一格式的错误信息并显示给用户。

**为什么这个优先级**: 这是最常见的错误场景，直接影响用户体验。

**验收场景**:
1. **当** 管理员删除不存在的用户，**则** 返回 `{ code: 500, message: "用户不存在", data: null }`
2. **当** 管理员创建重复用户名，**则** 返回 `{ code: 500, message: "用户名已存在", data: null }`
3. **当** 前端收到错误，**则** 用 ElMessage 显示 message 内容

---

### 用户故事 2 — 未知异常不泄露堆栈（优先级：P1）

系统发生意外错误（如数据库连接断开、空指针），前端只看到"服务器内部错误"，不会看到 Java 堆栈信息。

**为什么这个优先级**: 安全需求，堆栈泄露可能暴露系统架构和漏洞。

**验收场景**:
1. **当** 后端发生 NullPointerException，**则** 前端收到 `{ code: 500, message: "服务器内部错误", data: null }`
2. **当** 后端发生未知异常，**则** 服务器日志记录完整堆栈（`log.error`）

---

### 用户故事 3 — Controller 代码简洁化（优先级：P2）

开发者（我）写 Controller 时不需要写 try-catch，直接调用 Service/Mapper，代码更简洁可读。

**为什么这个优先级**: 代码质量改进，不影响用户功能。

**验收场景**:
1. **当** 查看任意 Controller 方法，**则** 没有 try-catch 包裹
2. **当** 需要抛业务错误，**则** 用 `throw new BusinessException("错误信息")`

---

### 边界情况
- 参数校验失败（`@Valid` 注解）→ 应返回字段级错误信息
- 并发请求同时触发异常 → 每个请求独立处理，互不影响
- 异常处理器本身出错 → Spring 兜底返回 500

## 需求

### 功能需求
- **FR-001**: 新增 `BusinessException` 运行时异常类
- **FR-002**: 新增 `GlobalExceptionHandler`（`@RestControllerAdvice`）
- **FR-003**: 处理三类异常：`BusinessException`、`MethodArgumentNotValidException`、`Exception`（兜底）
- **FR-004**: 所有 Controller 移除 try-catch，改用 BusinessException

### 关键实体
- **BusinessException**: 继承 RuntimeException，只有一个 message 字段
- **GlobalExceptionHandler**: 三个 `@ExceptionHandler` 方法

## 涉及文件

| 文件 | 改动 |
|------|------|
| `common/BusinessException.java` | 新增 |
| `common/GlobalExceptionHandler.java` | 新增 |
| `controller/AdminController.java` | 修改：移除 try-catch |
| `controller/OrderController.java` | 修改：移除 try-catch |
| `controller/AuthController.java` | 修改：移除 try-catch |
| `controller/MallController.java` | 修改：移除 try-catch |
| `controller/WarehouseController.java` | 修改：移除 try-catch |
| `controller/AddressController.java` | 修改：移除 try-catch |
| `controller/InventoryController.java` | 修改：移除 try-catch |
| `controller/DeliveryBatchController.java` | 修改：移除 try-catch |

## 验收标准
- [ ] 所有 Controller 方法中没有 try-catch
- [ ] BusinessException → 返回错误信息（如"用户不存在"）
- [ ] 未知异常 → 返回"服务器内部错误"，不泄露堆栈
- [ ] 参数校验失败 → 返回字段级错误信息
- [ ] 服务器日志记录未处理异常的完整堆栈
- [ ] 前端现有功能不受影响（返回格式不变）

## 审核清单
- [x] 没有 `[待确认]` 标记残留
- [x] 需求可测试、无歧义
- [x] 符合项目宪法
- [x] 验收标准完整
