package com.logistics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.logistics.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 直接用 MyBatis Plus 提供的 selectOne 配合 QueryWrapper/LambdaQueryWrapper 在 Service 中按 username+role 查询即可
}