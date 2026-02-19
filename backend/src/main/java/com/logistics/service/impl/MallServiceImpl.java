package com.logistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.logistics.dto.ProductListingRequest;
import com.logistics.entity.Inventory;
import com.logistics.entity.Mall;
import com.logistics.mapper.InventoryMapper;
import com.logistics.mapper.MallMapper;
import com.logistics.service.MallService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商城服务实现类
 */
@Service
@RequiredArgsConstructor
public class MallServiceImpl implements MallService {
    
    private final InventoryMapper inventoryMapper;
    private final MallMapper mallMapper;
    
    @Override
    public List<Inventory> getOfflineProducts(Integer userId) {
        QueryWrapper<Inventory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                    .eq("is_published", 0)
                    .orderByDesc("stock_in_date");
        return inventoryMapper.selectList(queryWrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Mall publishProduct(ProductListingRequest request) {
        // 查询库存商品
        Inventory inventory = inventoryMapper.selectById(request.getProductId());
        if (inventory == null) {
            throw new RuntimeException("商品不存在");
        }
        
        if (inventory.getIsPublished() == 1) {
            throw new RuntimeException("商品已上架");
        }
        
        if (request.getQuantity() > inventory.getQuantity()) {
            throw new RuntimeException("上架数量不能超过库存数量");
        }
        
        // 创建商城商品
        Mall mall = new Mall();
        mall.setProductId(inventory.getProductId());
        mall.setMerchantId(inventory.getUserId()); // 商户ID来自库存的user_id
        mall.setWarehouseId(inventory.getWarehouseId()); // 仓库ID
        mall.setProductName(inventory.getProductName());
        mall.setDescription(request.getDescription());
        mall.setAvailableQuantity(request.getQuantity());
        mall.setPrice(request.getPrice());
        mall.setIsPublished(1);
        mall.setPublishTime(LocalDateTime.now());
        mall.setImageUrl(inventory.getImageUrl());
        
        // 插入商城表
        mallMapper.insert(mall);
        
        // 更新库存表的上架状态
        inventory.setIsPublished(1);
        inventoryMapper.updateById(inventory);
        
        return mall;
    }
    
    @Override
    public List<Mall> getMallProducts() {
        QueryWrapper<Mall> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_published", 1)
                    .orderByDesc("publish_time");
        return mallMapper.selectList(queryWrapper);
    }
}
