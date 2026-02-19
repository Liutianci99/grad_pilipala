package com.logistics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存实体类
 */
@Data
@TableName("inventory")
public class Inventory {
    
    @TableId(type = IdType.AUTO)
    private Integer productId;
    
    private Integer userId;
    
    private String productName;
    
    private Integer quantity;
    
    private LocalDateTime stockInDate;
    
    private String imageUrl;
    
    private Integer isPublished; // 0-未上架, 1-已上架
    
    private Integer warehouseId; // 所属仓库ID
    
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String warehouseName; // 仓库名称（非数据库字段）
}
