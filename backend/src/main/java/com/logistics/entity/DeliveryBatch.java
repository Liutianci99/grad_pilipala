package com.logistics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 运输批次实体类
 */
@Data
@TableName("delivery_batches")
public class DeliveryBatch implements Serializable {
    
    /**
     * 运输批次ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    /**
     * 配送员ID
     */
    private Integer driverId;
    
    /**
     * 起始仓库ID
     */
    private Integer warehouseId;
    
    /**
     * 批次状态：0=待出发, 1=配送中, 2=已完成
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 实际出发时间
     */
    private LocalDateTime startedAt;
    
    /**
     * 完成时间
     */
    private LocalDateTime completedAt;
    
    /**
     * 高德返回的编码路线字符串（用于前端绘图）
     */
    private String routePolyline;
    
    /**
     * 预计总耗时（秒）
     */
    private Integer totalDuration;
    
    /**
     * 总距离（米）
     */
    private Integer totalDistance;
}
