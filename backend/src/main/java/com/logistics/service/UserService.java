package com.logistics.service;

import com.logistics.dto.LoginRequest;
import com.logistics.dto.LoginResponse;

public interface UserService {
    LoginResponse login(LoginRequest req);
}