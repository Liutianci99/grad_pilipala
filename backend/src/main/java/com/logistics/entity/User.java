package com.logistics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password; // BCrypt加密存储
    private String role;     // merchant / driver / consumer / admin
    private Integer warehouseId; // 配送员所属仓库（仅driver有值）
}
