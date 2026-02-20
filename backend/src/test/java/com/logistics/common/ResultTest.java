package com.logistics.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void success_noArgs_shouldReturnSuccessResult() {
        Result<Void> result = Result.success();
        assertTrue(result.isSuccess());
        assertEquals(200, result.getCode());
        assertEquals("操作成功", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void success_withData_shouldReturnDataResult() {
        Result<String> result = Result.success("hello");
        assertTrue(result.isSuccess());
        assertEquals(200, result.getCode());
        assertEquals("hello", result.getData());
    }

    @Test
    void success_withMessageAndData_shouldReturnBoth() {
        Result<Integer> result = Result.success("创建成功", 42);
        assertTrue(result.isSuccess());
        assertEquals("创建成功", result.getMessage());
        assertEquals(42, result.getData());
    }

    @Test
    void error_withMessage_shouldReturnErrorResult() {
        Result<Void> result = Result.error("出错了");
        assertFalse(result.isSuccess());
        assertEquals(500, result.getCode());
        assertEquals("出错了", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void error_withCodeAndMessage_shouldReturnCustomCode() {
        Result<Void> result = Result.error(400, "参数错误");
        assertFalse(result.isSuccess());
        assertEquals(400, result.getCode());
        assertEquals("参数错误", result.getMessage());
    }
}
