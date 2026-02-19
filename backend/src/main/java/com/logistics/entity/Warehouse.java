package com.logistics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 仓库实体类
 */
@Data
@TableName("warehouse")
public class Warehouse implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    /**
     * 仓库名称
     */
    private String name;
    
    /**
     * 所在城市
     */
    private String city;
    
    /**
     * 详细地址
     */
    private String address;
    
    /**
     * 经度
     */
    private BigDecimal longitude;
    
    /**
     * 纬度
     */
    private BigDecimal latitude;
}
