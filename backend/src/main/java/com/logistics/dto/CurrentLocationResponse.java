package com.logistics.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 当前位置响应DTO
 */
@Data
public class CurrentLocationResponse {

    /**
     * 纬度
     */
    private BigDecimal latitude;

    /**
     * 经度
     */
    private BigDecimal longitude;

    /**
     * 地址
     */
    private String address;

    /**
     * 状态: PENDING/DELIVERING/COMPLETED/STOPPED
     */
    private String status;

    /**
     * 进度百分比 (0-100)
     */
    private Integer progress;

    /**
     * 剩余距离(公里)
     */
    private BigDecimal remainingDistance;

    /**
     * 剩余时间(分钟)
     */
    private Integer remainingTime;
}
