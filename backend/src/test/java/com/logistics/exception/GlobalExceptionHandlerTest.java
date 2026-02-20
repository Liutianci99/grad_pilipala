package com.logistics.exception;

import com.logistics.common.Result;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBusinessException_shouldReturnErrorResult() {
        BusinessException ex = new BusinessException("用户不存在");
        Result<Void> result = handler.handleBusinessException(ex);
        assertFalse(result.isSuccess());
        assertEquals(500, result.getCode());
        assertEquals("用户不存在", result.getMessage());
    }

    @Test
    void handleBusinessException_withCustomCode() {
        BusinessException ex = new BusinessException(401, "未登录");
        Result<Void> result = handler.handleBusinessException(ex);
        assertFalse(result.isSuccess());
        assertEquals(401, result.getCode());
        assertEquals("未登录", result.getMessage());
    }

    @Test
    void handleRuntimeException_shouldReturnServerError() {
        RuntimeException ex = new RuntimeException("NPE");
        Result<Void> result = handler.handleRuntimeException(ex);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("NPE"));
    }

    @Test
    void handleException_shouldReturnGenericError() {
        Exception ex = new Exception("unknown");
        Result<Void> result = handler.handleException(ex);
        assertFalse(result.isSuccess());
        assertEquals("服务器内部错误", result.getMessage());
    }
}
