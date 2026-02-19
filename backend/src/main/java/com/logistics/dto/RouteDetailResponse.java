package com.logistics.dto;

import com.alibaba.fastjson.JSONArray;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 路线详情响应DTO
 */
@Data
public class RouteDetailResponse {

    /**
     * 路线ID
     */
    private Long routeId;

    /**
     * 压缩的路径坐标（腾讯地图格式）
     */
    private JSONArray polyline;

    /**
     * 总距离（公里）
     */
    private BigDecimal totalDistance;

    /**
     * 总时长（分钟）
     */
    private Integer totalDuration;

    /**
     * 当前状态
     */
    private String status;
}
