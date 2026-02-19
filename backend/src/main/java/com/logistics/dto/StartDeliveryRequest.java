package com.logistics.dto;

import lombok.Data;

/**
 * 开始运输请求DTO
 */
@Data
public class StartDeliveryRequest {

    /**
     * 配送批次时间（ISO格式字符串）
     */
    private String deliveryTime;

    /**
     * 仓库ID
     */
    private Integer warehouseId;
}
