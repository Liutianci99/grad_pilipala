package com.logistics.service;

import com.logistics.common.Result;
import com.logistics.entity.Address;

import java.util.List;

/**
 * 地址服务接口
 */
public interface AddressService {
    
    /**
     * 获取用户的所有地址
     */
    List<Address> listByUserId(Long userId);
    
    /**
     * 添加地址
     */
    Result<Address> add(Address address);
    
    /**
     * 更新地址
     */
    Result<Address> update(Address address);
    
    /**
     * 删除地址
     */
    Result<Void> delete(Long id, Long userId);
    
    /**
     * 设置默认地址
     */
    Result<Void> setDefault(Long id, Long userId);
}
