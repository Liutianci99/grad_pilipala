package com.logistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.logistics.dto.InventoryQueryRequest;
import com.logistics.dto.StockInRequest;
import com.logistics.entity.Inventory;
import com.logistics.mapper.InventoryMapper;
import com.logistics.service.InventoryService;
import com.logistics.service.R2StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 库存服务实现类
 */
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {
    
    private final InventoryMapper inventoryMapper;
    private final R2StorageService r2StorageService;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Inventory stockIn(StockInRequest request, MultipartFile imageFile) {
        // 上传图片到Cloudflare R2并获取URL
        String imageUrl = r2StorageService.uploadFile(imageFile);
        
        // 创建库存记录
        Inventory inventory = new Inventory();
        inventory.setUserId(request.getUserId());
        inventory.setWarehouseId(request.getWarehouseId());
        inventory.setProductName(request.getProductName());
        inventory.setQuantity(request.getQuantity());
        inventory.setStockInDate(request.getStockInDate());
        inventory.setImageUrl(imageUrl);
        inventory.setIsPublished(0); // 默认未上架
        
        // 保存到数据库
        inventoryMapper.insert(inventory);
        
        return inventory;
    }
    
    @Override
    public List<Inventory> queryInventory(InventoryQueryRequest request) {
        // 使用新的方法查询库存并关联仓库信息
        return inventoryMapper.selectWithWarehouseByConditions(
            request.getUserId(),
            request.getProductName(),
            request.getMinStock(),
            request.getMaxStock(),
            request.getIsPublished()
        );
    }
}
