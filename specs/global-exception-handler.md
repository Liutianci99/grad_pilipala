# Spec: Global Exception Handler

## Summary
Replace scattered try-catch blocks in controllers with a single `@RestControllerAdvice` that catches all exceptions and returns consistent `Result<?>` responses.

## Current State
- Every controller method has its own try-catch
- Error responses are inconsistent (some return Result.error, some throw raw)
- Stack traces leak to frontend on unhandled exceptions

## Changes

### 1. New class: `GlobalExceptionHandler.java`
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Business logic errors (custom exception)
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusiness(BusinessException e) {
        return Result.error(e.getMessage());
    }

    // Validation errors
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

    // Catch-all
    @ExceptionHandler(Exception.class)
    public Result<?> handleAll(Exception e) {
        log.error("Unhandled exception", e);
        return Result.error("服务器内部错误");
    }
}
```

### 2. New class: `BusinessException.java`
Simple runtime exception for throwing from services:
```java
public class BusinessException extends RuntimeException {
    public BusinessException(String message) { super(message); }
}
```

### 3. Remove try-catch from controllers
Controllers just call service methods directly. If something goes wrong, the service throws `BusinessException` and the handler catches it.

## Files Changed
- New: `common/GlobalExceptionHandler.java`
- New: `common/BusinessException.java`
- Modified: All controllers — remove try-catch wrappers, throw BusinessException where needed

## Acceptance Criteria
- [ ] No try-catch in any controller method
- [ ] BusinessException → 200 with error message in Result
- [ ] Unknown exception → 200 with "服务器内部错误" (no stack trace leaked)
- [ ] Validation errors → readable field-level messages
