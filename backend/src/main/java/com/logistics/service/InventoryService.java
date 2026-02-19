package com.logistics.service;

import com.logistics.dto.InventoryQueryRequest;
import com.logistics.dto.StockInRequest;
import com.logistics.entity.Inventory;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 库存服务接口
 */
public interface InventoryService {
    
    /**
     * 商品入库
     * @param request 入库请求数据
     * @param imageFile 商品图片文件
     * @return 创建的库存记录
     */
    Inventory stockIn(StockInRequest request, MultipartFile imageFile);
    
    /**
     * 查询库存列表
     * @param request 查询条件
     * @return 库存列表
     */
    List<Inventory> queryInventory(InventoryQueryRequest request);
}
