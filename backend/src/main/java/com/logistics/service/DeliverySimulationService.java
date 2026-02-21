package com.logistics.service;

import com.alibaba.fastjson.JSONArray;
import com.logistics.entity.DeliveryBatch;
import com.logistics.entity.DeliveryLocation;
import com.logistics.mapper.DeliveryBatchMapper;
import com.logistics.mapper.DeliveryLocationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 配送模拟服务
 * 模拟配送员沿路线移动，更新 delivery_batches.current_index
 */
@Slf4j
@Service
public class DeliverySimulationService {

    @Autowired
    private DeliveryBatchMapper deliveryBatchMapper;

    @Autowired
    private DeliveryLocationMapper deliveryLocationMapper;

    @Autowired
    private TencentMapService tencentMapService;

    /**
     * 开始配送模拟（异步执行）
     */
    @Async
    public void startSimulation(Integer batchId) {
        log.info("开始配送模拟，批次ID: {}", batchId);

        try {
            DeliveryBatch batch = deliveryBatchMapper.selectById(batchId);
            if (batch == null || batch.getStatus() != 1) {
                log.warn("批次不存在或状态不正确，批次ID: {}", batchId);
                return;
            }

            if (batch.getRouteData() == null || batch.getRouteData().isEmpty()) {
                log.error("路径数据为空，批次ID: {}", batchId);
                return;
            }

            // 解压polyline坐标
            JSONArray pathPoints = tencentMapService.decompressPolyline(batch.getRouteData());
            if (pathPoints.isEmpty()) {
                log.error("路径点为空，批次ID: {}", batchId);
                return;
            }

            int totalPoints = pathPoints.size();
            long intervalMs = (batch.getTotalDuration() * 1000L) / totalPoints;

            log.info("批次ID: {}, 总点数: {}, 间隔时间: {}ms", batchId, totalPoints, intervalMs);

            for (int i = 0; i < totalPoints; i++) {
                // 检查批次状态
                batch = deliveryBatchMapper.selectById(batchId);
                if (batch.getStatus() != 1) {
                    log.info("配送已停止，批次ID: {}", batchId);
                    break;
                }

                JSONArray point = pathPoints.getJSONArray(i);
                double latitude = point.getDoubleValue(0);
                double longitude = point.getDoubleValue(1);

                // 更新当前索引
                batch.setCurrentIndex(i);
                deliveryBatchMapper.updateById(batch);

                // 每10个点记录一次位置
                if (i % 10 == 0 || i == totalPoints - 1) {
                    DeliveryLocation location = new DeliveryLocation();
                    location.setBatchId(batchId);
                    location.setLatitude(new BigDecimal(latitude));
                    location.setLongitude(new BigDecimal(longitude));
                    location.setPathIndex(i);

                    try {
                        String address = tencentMapService.getAddress(latitude, longitude);
                        location.setAddress(address);
                    } catch (Exception e) {
                        log.warn("获取地址失败: {}", e.getMessage());
                        location.setAddress("位置获取中...");
                    }

                    deliveryLocationMapper.insert(location);
                }

                Thread.sleep(intervalMs);
            }

            // 配送完成
            batch.setStatus(2);
            batch.setCompletedAt(LocalDateTime.now());
            deliveryBatchMapper.updateById(batch);

            log.info("配送完成，批次ID: {}", batchId);

        } catch (InterruptedException e) {
            log.error("配送模拟被中断，批次ID: {}", batchId, e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("配送模拟异常，批次ID: {}", batchId, e);
        }
    }

    /**
     * 停止配送模拟
     */
    public void stopSimulation(Integer batchId) {
        DeliveryBatch batch = deliveryBatchMapper.selectById(batchId);
        if (batch != null && batch.getStatus() == 1) {
            batch.setStatus(0); // 回到待出发
            deliveryBatchMapper.updateById(batch);
            log.info("配送已停止，批次ID: {}", batchId);
        }
    }
}
