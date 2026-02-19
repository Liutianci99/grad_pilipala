package com.logistics.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商城商品实体类
 */
@Data
@TableName("mall")
public class Mall {
    
    @TableId
    private Integer productId;
    
    private Integer merchantId;
    
    private Integer warehouseId;
    
    private String productName;
    
    private String description;
    
    private Integer availableQuantity;
    
    private BigDecimal price;
    
    private Integer isPublished;
    
    private LocalDateTime publishTime;
    
    private String imageUrl;
}
