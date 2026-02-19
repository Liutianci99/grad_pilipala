package com.logistics.dto;

import lombok.Data;

/**
 * 库存查询请求DTO
 */
@Data
public class InventoryQueryRequest {
    
    private String productName;  // 商品名称关键字
    
    private Integer minStock;    // 最小库存
    
    private Integer maxStock;    // 最大库存
    
    private Integer isPublished; // 上架状态：null-全部, 0-未上架, 1-已上架
    
    private Integer userId;      // 用户ID（商家查询自己的库存）
}
