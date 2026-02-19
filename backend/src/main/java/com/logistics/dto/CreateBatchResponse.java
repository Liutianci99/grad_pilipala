package com.logistics.dto;

import lombok.Data;

import java.util.List;

/**
 * 创建送货批次响应
 */
@Data
public class CreateBatchResponse {
    private Integer batchId;
    private Integer totalDistance; // 米
    private Integer totalDuration; // 秒
    private Integer orderCount;
    /**
     * 停靠顺序，元素为 orderId，顺序为 1..N
     */
    private List<Integer> stopOrder;
}
