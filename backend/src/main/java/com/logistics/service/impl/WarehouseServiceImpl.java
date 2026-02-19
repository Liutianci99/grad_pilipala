package com.logistics.service.impl;

import com.logistics.entity.Warehouse;
import com.logistics.mapper.WarehouseMapper;
import com.logistics.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 仓库服务实现
 */
@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {
    
    private final WarehouseMapper warehouseMapper;
    
    @Override
    public List<Warehouse> listAll() {
        return warehouseMapper.selectList(null);
    }

    @Override
    public Warehouse getById(Integer id) {
        return warehouseMapper.selectById(id);
    }
}
