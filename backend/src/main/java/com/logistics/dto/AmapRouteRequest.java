package com.logistics.dto;

import lombok.Data;

import java.util.List;

/**
 * 高德路径规划API请求参数
 */
@Data
public class AmapRouteRequest {
    
    /**
     * 高德API Key
     */
    private String key;
    
    /**
     * 起点坐标：经度,纬度
     */
    private String origin;
    
    /**
     * 终点坐标：经度,纬度
     */
    private String destination;
    
    /**
     * 途经点坐标：经度,纬度|经度,纬度...（最多16个）
     */
    private String waypoints;
    
    /**
     * 策略：0=速度优先（时间），1=费用优先（不走收费路段的最快道路），2=距离优先，3=不走快速路
     */
    private Integer strategy;
}
