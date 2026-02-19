package com.logistics.dto;

import com.logistics.entity.Order;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 运输批次响应DTO
 */
@Data
public class DeliveryBatchResponse {

    /**
     * 批次ID
     */
    private Integer batchId;

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
     * 总距离（米）
     */
    private Integer totalDistance;

    /**
     * 预计总耗时（秒）
     */
    private Integer totalDuration;

    /**
     * 批次包含的订单列表
     */
    private List<Order> orders;
}
