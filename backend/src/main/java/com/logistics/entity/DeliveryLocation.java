package com.logistics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 配送位置记录实体类
 * 记录配送员历史轨迹
 */
@Data
@TableName("delivery_location")
public class DeliveryLocation {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 路线ID
     */
    private Long routeId;

    /**
     * 纬度
     */
    private BigDecimal latitude;

    /**
     * 经度
     */
    private BigDecimal longitude;

    /**
     * 地址（通过逆地理编码获取）
     */
    private String address;

    /**
     * 路径点索引
     */
    private Integer pathIndex;

    /**
     * 记录时间
     */
    private LocalDateTime recordedAt;
}
