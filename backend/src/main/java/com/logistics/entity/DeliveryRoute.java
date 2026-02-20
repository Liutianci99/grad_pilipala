package com.logistics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 配送路线实体类
 * 存储路线规划结果和实时配送状态
 */
@Data
@TableName("delivery_route")
public class DeliveryRoute {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的运输批次ID
     */
    private Integer batchId;

    /**
     * 配送批次时间
     */
    private LocalDateTime deliveryTime;

    /**
     * 仓库ID
     */
    private Integer warehouseId;

    /**
     * 路线数据(polyline压缩坐标JSON数组)
     */
    private String routeData;

    /**
     * 总距离(公里)
     */
    private BigDecimal totalDistance;

    /**
     * 总时长(分钟)
     */
    private Integer totalDuration;

    /**
     * 状态: PENDING=待开始, DELIVERING=运输中, COMPLETED=已完成, STOPPED=已停止
     */
    private String status;

    /**
     * 当前路径点索引
     */
    private Integer currentIndex;

    /**
     * 开始时间
     */
    private LocalDateTime startedAt;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
