package com.logistics.service;

import com.alibaba.fastjson.JSONArray;
import com.logistics.entity.DeliveryLocation;
import com.logistics.entity.DeliveryRoute;
import com.logistics.mapper.DeliveryLocationMapper;
import com.logistics.mapper.DeliveryRouteMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 配送模拟服务
 * 负责模拟配送员沿路线移动
 */
@Slf4j
@Service
public class DeliverySimulationService {

    @Autowired
    private DeliveryRouteMapper deliveryRouteMapper;

    @Autowired
    private DeliveryLocationMapper deliveryLocationMapper;

    @Autowired
    private TencentMapService tencentMapService;

    /**
     * 开始配送模拟（异步执行）
     *
     * @param routeId 路线ID
     */
    @Async
    public void startSimulation(Long routeId) {
        log.info("开始配送模拟，路线ID: {}", routeId);

        try {
            // 查询路线信息
            DeliveryRoute route = deliveryRouteMapper.selectById(routeId);
            if (route == null || !"DELIVERING".equals(route.getStatus())) {
                log.warn("路线不存在或状态不正确，路线ID: {}", routeId);
                return;
            }

            // 解压polyline坐标
            JSONArray pathPoints = tencentMapService.decompressPolyline(route.getRouteData());
            if (pathPoints.isEmpty()) {
                log.error("路径点为空，路线ID: {}", routeId);
                return;
            }

            // 计算每个点的间隔时间（毫秒）
            int totalPoints = pathPoints.size();
            long intervalMs = (route.getTotalDuration() * 60 * 1000L) / totalPoints;

            log.info("路线ID: {}, 总点数: {}, 间隔时间: {}ms", routeId, totalPoints, intervalMs);

            // 模拟移动
            for (int i = 0; i < totalPoints; i++) {
                // 检查路线状态
                route = deliveryRouteMapper.selectById(routeId);
                if (!"DELIVERING".equals(route.getStatus())) {
                    log.info("配送已停止，路线ID: {}", routeId);
                    break;
                }

                // 获取当前坐标点
                JSONArray point = pathPoints.getJSONArray(i);
                double latitude = point.getDoubleValue(0);
                double longitude = point.getDoubleValue(1);

                // 更新当前索引
                route.setCurrentIndex(i);
                deliveryRouteMapper.updateById(route);

                // 每10个点记录一次位置（减少数据库压力）
                if (i % 10 == 0 || i == totalPoints - 1) {
                    DeliveryLocation location = new DeliveryLocation();
                    location.setRouteId(routeId);
                    location.setLatitude(new BigDecimal(latitude));
                    location.setLongitude(new BigDecimal(longitude));
                    location.setPathIndex(i);

                    // 异步获取地址（不阻塞主流程）
                    try {
                        String address = tencentMapService.getAddress(latitude, longitude);
                        location.setAddress(address);
                    } catch (Exception e) {
                        log.warn("获取地址失败: {}", e.getMessage());
                        location.setAddress("位置获取中...");
                    }

                    deliveryLocationMapper.insert(location);
                    log.debug("记录位置: 索引={}, 坐标=[{}, {}]", i, latitude, longitude);
                }

                // 等待间隔时间
                Thread.sleep(intervalMs);
            }

            // 配送完成
            route.setStatus("COMPLETED");
            route.setCompletedAt(LocalDateTime.now());
            deliveryRouteMapper.updateById(route);

            log.info("配送完成，路线ID: {}", routeId);

        } catch (InterruptedException e) {
            log.error("配送模拟被中断，路线ID: {}", routeId, e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("配送模拟异常，路线ID: {}", routeId, e);
        }
    }

    /**
     * 停止配送模拟
     *
     * @param routeId 路线ID
     */
    public void stopSimulation(Long routeId) {
        DeliveryRoute route = deliveryRouteMapper.selectById(routeId);
        if (route != null && "DELIVERING".equals(route.getStatus())) {
            route.setStatus("STOPPED");
            deliveryRouteMapper.updateById(route);
            log.info("配送已停止，路线ID: {}", routeId);
        }
    }
}
