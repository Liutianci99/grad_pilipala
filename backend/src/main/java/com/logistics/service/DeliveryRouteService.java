package com.logistics.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.logistics.entity.DeliveryRoute;
import com.logistics.mapper.DeliveryRouteMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 配送路线服务
 */
@Service
public class DeliveryRouteService extends ServiceImpl<DeliveryRouteMapper, DeliveryRoute> implements IService<DeliveryRoute> {

    /**
     * 根据配送时间和仓库ID查询路线
     */
    public DeliveryRoute getByDeliveryTimeAndWarehouse(LocalDateTime deliveryTime, Integer warehouseId) {
        return baseMapper.selectByDeliveryTimeAndWarehouse(deliveryTime, warehouseId);
    }

    /**
     * 根据批次ID查询路线
     */
    public DeliveryRoute getByBatchId(Integer batchId) {
        return baseMapper.selectByBatchId(batchId);
    }
}
