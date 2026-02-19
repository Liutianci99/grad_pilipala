package com.logistics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 配送员实体类
 */
@Data
@TableName("delivery_personnel")
public class DeliveryPersonnel {
    
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    private Integer userId;
    
    private Integer warehouseId;
    
    // 非数据库字段，用于显示仓库名称
    @TableField(exist = false)
    private String warehouseName;
}
