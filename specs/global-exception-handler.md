# 规范：全局异常处理器

## 概述
用一个 `@RestControllerAdvice` 统一处理所有异常，替代控制器里散落的 try-catch，返回统一的 `Result<?>` 格式。

## 现状分析
- 每个控制器方法都有自己的 try-catch
- 错误响应格式不统一（有的返回 Result.error，有的直接抛异常）
- 未处理的异常会把堆栈信息泄露给前端（安全风险）

## 改动详情

### 1. 新增：`BusinessException.java`

位置：`com.logistics.common.BusinessException`

```java
/**
 * 业务异常 — 用于 Service 层抛出可预期的错误
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
```

### 2. 新增：`GlobalExceptionHandler.java`

位置：`com.logistics.common.GlobalExceptionHandler`

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常 → 返回错误信息给前端
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusiness(BusinessException e) {
        return Result.error(e.getMessage());
    }

    /**
     * 参数校验异常 → 返回字段级错误信息
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return Result.error(msg);
    }

    /**
     * 兜底 → 隐藏堆栈，返回通用错误
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleAll(Exception e) {
        log.error("未处理异常", e);
        return Result.error("服务器内部错误");
    }
}
```

### 3. 清理所有 Controller

改动原则：
- 移除所有 try-catch 包裹
- Controller 直接调用 Service/Mapper 方法
- 需要抛错的地方用 `throw new BusinessException("错误信息")`

改动前：
```java
@GetMapping("/list")
public Result<?> list() {
    try {
        List<Item> items = mapper.selectList(null);
        return Result.success(items);
    } catch (Exception e) {
        return Result.error("查询失败: " + e.getMessage());
    }
}
```

改动后：
```java
@GetMapping("/list")
public Result<?> list() {
    List<Item> items = mapper.selectList(null);
    return Result.success(items);
}
```

需要业务校验时：
```java
@DeleteMapping("/{id}")
public Result<?> delete(@PathVariable Long id) {
    User user = userMapper.selectById(id);
    if (user == null) {
        throw new BusinessException("用户不存在");
    }
    userMapper.deleteById(id);
    return Result.success("删除成功");
}
```

## 涉及文件

| 文件 | 改动 |
|------|------|
| `common/BusinessException.java` | 新增 |
| `common/GlobalExceptionHandler.java` | 新增 |
| `controller/AdminController.java` | 移除 try-catch |
| `controller/OrderController.java` | 移除 try-catch |
| `controller/AuthController.java` | 移除 try-catch |
| `controller/MallController.java` | 移除 try-catch |
| `controller/WarehouseController.java` | 移除 try-catch |
| `controller/AddressController.java` | 移除 try-catch |
| `controller/InventoryController.java` | 移除 try-catch |
| `controller/DeliveryBatchController.java` | 移除 try-catch |

## 验收标准
- [ ] 所有 Controller 方法中没有 try-catch
- [ ] `BusinessException` → 返回 `Result.error` + 业务错误信息（如"用户不存在"）
- [ ] 未知异常 → 返回 `Result.error("服务器内部错误")`，不泄露堆栈
- [ ] 参数校验失败 → 返回可读的字段级错误信息
- [ ] 日志中记录未处理异常的完整堆栈（`log.error`）
- [ ] 前端现有功能不受影响（返回格式不变，仍是 `{ code, message, data }`）
