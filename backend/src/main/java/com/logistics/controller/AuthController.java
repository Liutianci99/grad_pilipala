package com.logistics.controller;

import com.logistics.common.Result;
import com.logistics.dto.LoginRequest;
import com.logistics.dto.LoginResponse;
import com.logistics.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户登录认证相关接口")
public class AuthController {

    private final UserService userService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest req) {
        return userService.login(req);
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public LoginResponse register(@RequestBody LoginRequest req) {
        return userService.register(req);
    }

    @Operation(summary = "健康检查")
    @GetMapping("/test")
    public String test() {
        return "Backend is running!";
    }
}
