package com.logistics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品上架请求DTO
 */
@Data
public class ProductListingRequest {
    
    @NotNull(message = "商品ID不能为空")
    private Integer productId;
    
    private String description;  // 商品介绍
    
    @NotNull(message = "上架数量不能为空")
    @Positive(message = "上架数量必须大于0")
    private Integer quantity;
    
    @NotNull(message = "售价不能为空")
    @Positive(message = "售价必须大于0")
    private BigDecimal price;
}
