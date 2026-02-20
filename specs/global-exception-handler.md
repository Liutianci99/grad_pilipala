# 规范：全局异常处理器

## 概述
用一个 `@RestControllerAdvice` 统一处理所有异常，替代控制器里散落的 try-catch，返回统一的 `Result<?>` 格式。

## 现状
- 每个控制器方法都有自己的 try-catch
- 错误响应格式不统一（有的返回 Result.error，有的直接抛异常）
- 未处理的异常会把堆栈信息泄露给前端

## 改动

### 1. 新增：`GlobalExceptionHandler.java`
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 业务异常（自定义异常）
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusiness(BusinessException e) {
        return Result.error(e.getMessage());
    }

    // 参数校验异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return Result.error(msg);
    }

    // 404
    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<?> handleNotFound(NoHandlerFoundException e) {
        return Result.error(404, "接口不存在");
    }

    // 兜底
    @ExceptionHandler(Exception.class)
    public Result<?> handleAll(Exception e) {
        log.error("未处理异常", e);
        return Result.error("服务器内部错误");
    }
}
```

### 2. 新增：`BusinessException.java`
简单的运行时异常，供 Service 层抛出：
```java
public class BusinessException extends RuntimeException {
    public BusinessException(String message) { super(message); }
}
```

### 3. 清理控制器
控制器直接调用 Service 方法，不再 try-catch。出错时 Service 抛 `BusinessException`，由全局处理器统一捕获。

## 涉及文件
- 新增：`common/GlobalExceptionHandler.java`
- 新增：`common/BusinessException.java`
- 修改：所有 Controller — 移除 try-catch，改用 BusinessException

## 验收标准
- [ ] 所有控制器方法中没有 try-catch
- [ ] BusinessException → 返回 Result.error + 错误信息
- [ ] 未知异常 → 返回"服务器内部错误"（不泄露堆栈）
- [ ] 参数校验失败 → 返回可读的字段级错误信息
