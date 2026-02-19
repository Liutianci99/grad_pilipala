package com.logistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.logistics.common.Result;
import com.logistics.entity.Address;
import com.logistics.mapper.AddressMapper;
import com.logistics.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 地址服务实现
 */
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {
    
    private final AddressMapper addressMapper;
    
    @Override
    public List<Address> listByUserId(Long userId) {
        return addressMapper.selectList(new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, userId)
                .orderByDesc(Address::getIsDefault)
                .orderByDesc(Address::getId));
    }
    
    @Override
    @Transactional
    public Result<Address> add(Address address) {
        // 如果设置为默认地址，先取消其他默认地址
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            addressMapper.update(null, new LambdaUpdateWrapper<Address>()
                    .eq(Address::getUserId, address.getUserId())
                    .set(Address::getIsDefault, 0));
        } else {
            // 如果是第一个地址，自动设为默认
            long count = addressMapper.selectCount(new LambdaQueryWrapper<Address>()
                    .eq(Address::getUserId, address.getUserId()));
            if (count == 0) {
                address.setIsDefault(1);
            } else {
                address.setIsDefault(0);
            }
        }
        
        addressMapper.insert(address);
        return Result.success(address);
    }
    
    @Override
    @Transactional
    public Result<Address> update(Address address) {
        // 验证地址是否属于该用户
        Address existAddress = addressMapper.selectById(address.getId());
        if (existAddress == null) {
            return Result.error("地址不存在");
        }
        if (!existAddress.getUserId().equals(address.getUserId())) {
            return Result.error("无权限操作此地址");
        }
        
        // 如果设置为默认地址，先取消其他默认地址
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            addressMapper.update(null, new LambdaUpdateWrapper<Address>()
                    .eq(Address::getUserId, address.getUserId())
                    .ne(Address::getId, address.getId())
                    .set(Address::getIsDefault, 0));
        }
        
        addressMapper.updateById(address);
        return Result.success(address);
    }
    
    @Override
    @Transactional
    public Result<Void> delete(Long id, Long userId) {
        // 验证地址是否属于该用户
        Address address = addressMapper.selectById(id);
        if (address == null) {
            return Result.error("地址不存在");
        }
        if (!address.getUserId().equals(userId)) {
            return Result.error("无权限操作此地址");
        }
        
        addressMapper.deleteById(id);
        
        // 如果删除的是默认地址，将第一个地址设为默认
        if (address.getIsDefault() == 1) {
            Address firstAddress = addressMapper.selectOne(new LambdaQueryWrapper<Address>()
                    .eq(Address::getUserId, userId)
                    .last("LIMIT 1"));
            if (firstAddress != null) {
                firstAddress.setIsDefault(1);
                addressMapper.updateById(firstAddress);
            }
        }
        
        return Result.success(null);
    }
    
    @Override
    @Transactional
    public Result<Void> setDefault(Long id, Long userId) {
        // 验证地址是否属于该用户
        Address address = addressMapper.selectById(id);
        if (address == null) {
            return Result.error("地址不存在");
        }
        if (!address.getUserId().equals(userId)) {
            return Result.error("无权限操作此地址");
        }
        
        // 取消其他默认地址
        addressMapper.update(null, new LambdaUpdateWrapper<Address>()
                .eq(Address::getUserId, userId)
                .set(Address::getIsDefault, 0));
        
        // 设置当前地址为默认
        address.setIsDefault(1);
        addressMapper.updateById(address);
        
        return Result.success(null);
    }
}
