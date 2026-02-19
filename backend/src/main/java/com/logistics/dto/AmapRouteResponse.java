package com.logistics.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 高德路径规划API响应
 * 使用 @JsonIgnoreProperties 允许忽略未定义的字段，提高容错性
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmapRouteResponse {
    
    private String status;
    
    private String info;
    
    private Route route;
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Route {
        private String origin;
        private String destination;
        
        @JsonProperty("taxi_cost")
        private String taxiCost;
        
        private List<Path> paths;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Path {
        private String distance;  // 总距离（米）
        private String duration;  // 总耗时（秒）
        
        private List<Step> steps;
        
        @JsonProperty("waypoint_order")
        private String waypointOrder;  // 途经点顺序，如"0,1,2"
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Step {
        private String instruction;
        private String orientation;
        private String road;
        private String distance;
        private String duration;
        private String polyline;  // 路径坐标点串
        // 高德返回的 action 可能是字符串或空数组，使用 Object 以兼容两种情况
        private Object action;
        
        // 高德返回的 assistant_action 可能是字符串或空数组，使用 Object 以兼容两种情况
        @JsonProperty("assistant_action")
        private Object assistantAction;
    }
}
