package com.logistics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.logistics.entity.Address;
import org.apache.ibatis.annotations.Mapper;

/**
 * 地址 Mapper
 */
@Mapper
public interface AddressMapper extends BaseMapper<Address> {
}
