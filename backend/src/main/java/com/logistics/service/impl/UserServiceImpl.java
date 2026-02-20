package com.logistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.logistics.dto.LoginRequest;
import com.logistics.dto.LoginResponse;
import com.logistics.entity.DeliveryPersonnel;
import com.logistics.entity.User;
import com.logistics.mapper.DeliveryPersonnelMapper;
import com.logistics.mapper.UserMapper;
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
    private final DeliveryPersonnelMapper deliveryPersonnelMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginResponse register(LoginRequest req) {
        // 不允许注册管理员
        if ("admin".equals(req.getRole())) {
            return new LoginResponse(false, "不允许注册管理员账号", null);
        }
        // 检查用户名+角色是否已存在
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
            .eq(User::getUsername, req.getUsername())
            .eq(User::getRole, req.getRole()));
        if (count > 0) {
            return new LoginResponse(false, "该用户名已被注册", null);
        }
        // 创建用户
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(req.getRole());
        userMapper.insert(user);
        // 自动登录，返回 token
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
        
        // 生成 Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        
        // 如果是配送员，查询所属仓库信息
        Integer warehouseId = null;
        String warehouseName = null;
        if ("driver".equals(user.getRole())) {
            DeliveryPersonnel personnel = deliveryPersonnelMapper.selectByUserIdWithWarehouse(Math.toIntExact(user.getId()));
            if (personnel != null) {
                warehouseId = personnel.getWarehouseId();
                warehouseName = personnel.getWarehouseName();
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
