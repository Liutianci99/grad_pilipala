package com.logistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.logistics.dto.LoginRequest;
import com.logistics.dto.LoginResponse;
import com.logistics.entity.User;
import com.logistics.entity.Warehouse;
import com.logistics.mapper.UserMapper;
import com.logistics.mapper.WarehouseMapper;
import com.logistics.service.UserService;
import com.logistics.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final WarehouseMapper warehouseMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginResponse register(LoginRequest req) {
        if ("admin".equals(req.getRole())) {
            return new LoginResponse(false, "不允许注册管理员账号", null);
        }
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
            .eq(User::getUsername, req.getUsername())
            .eq(User::getRole, req.getRole()));
        if (count > 0) {
            return new LoginResponse(false, "该用户名已被注册", null);
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(req.getRole());
        userMapper.insert(user);
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
            user.getId(), user.getUsername(), user.getRole(), token, null, null
        );
        return new LoginResponse(true, "注册成功", userInfo);
    }

    @Override
    public LoginResponse login(LoginRequest req) {
        log.debug("登录请求: username={}, role={}", req.getUsername(), req.getRole());
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
            .eq(User::getUsername, req.getUsername())
            .eq(User::getRole, req.getRole()));

        if (user == null) {
            return new LoginResponse(false, "用户或角色不存在", null);
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            return new LoginResponse(false, "密码错误", null);
        }
        
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        
        // 配送员：直接从 users.warehouse_id 获取仓库信息
        Integer warehouseId = null;
        String warehouseName = null;
        if ("driver".equals(user.getRole()) && user.getWarehouseId() != null) {
            warehouseId = user.getWarehouseId();
            Warehouse warehouse = warehouseMapper.selectById(warehouseId);
            if (warehouse != null) {
                warehouseName = warehouse.getName();
            }
        }
        
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
            user.getId(), 
            user.getUsername(), 
            user.getRole(),
            token,
            warehouseId,
            warehouseName
        );
        return new LoginResponse(true, "登录成功", userInfo);
    }
}
