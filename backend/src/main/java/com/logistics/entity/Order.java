package com.logistics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体类
 */
@Data
@TableName("orders")
public class Order {
    
    @TableId(type = IdType.AUTO)
    private Integer orderId;
    
    private Integer productId;
    
    private Integer customerId;
    
    private Integer merchantId;
    
    private Integer addressId;
    
    private String productName;
    
    private Integer quantity;
    
    private BigDecimal unitPrice;
    
    private BigDecimal totalAmount;
    
    private String imageUrl;
    
    private Integer warehouseId;
    
    private Integer status;
    
    private LocalDateTime orderTime;
    
    private LocalDateTime shipTime;
    
    private LocalDateTime pickupTime;
    
    private LocalDateTime deliveryTime;
    
    private LocalDateTime receiveTime;
    
    private LocalDateTime reviewTime;
    
    // 非数据库字段，用于显示仓库名称
    @TableField(exist = false)
    private String warehouseName;
    
    // 非数据库字段，用于显示顾客名称
    @TableField(exist = false)
    private String customerName;
    
    // 非数据库字段，用于显示收货地址
    @TableField(exist = false)
    private Address address;
}
