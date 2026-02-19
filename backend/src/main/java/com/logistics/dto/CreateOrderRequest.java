package com.logistics.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建订单请求DTO
 */
@Data
public class CreateOrderRequest {
    
    private Integer customerId;
    
    private Integer productId;
    
    private Integer quantity;
    
    private BigDecimal price;
    
    private Integer addressId;
}
