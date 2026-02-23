package com.logistics.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
    private String role; // merchant / driver / consumer / admin
    private Integer warehouseId; // for driver registration
}