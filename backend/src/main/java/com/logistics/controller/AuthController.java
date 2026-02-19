package com.logistics.controller;

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
@CrossOrigin
@Tag(name = "认证管理", description = "用户登录认证相关接口")
public class AuthController {

    private final UserService userService;

    @Operation(summary = "用户登录", description = "根据用户名、密码和角色进行登录认证")
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest req) {
        System.out.println("收到登录请求: username=" + req.getUsername() + ", role=" + req.getRole());
        return userService.login(req);
    }
    
    @Operation(summary = "健康检查", description = "检查后端服务是否正常运行")
    @GetMapping("/test")
    public String test() {
        return "Backend is running!";
    }
}