package com.logistics.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.logistics.common.Result;
import com.logistics.dto.CurrentLocationResponse;
import com.logistics.dto.RouteDetailResponse;
import com.logistics.entity.DeliveryBatch;
import com.logistics.entity.DeliveryBatchOrder;
import com.logistics.entity.DeliveryLocation;
import com.logistics.entity.Order;
import com.logistics.entity.Warehouse;
import com.logistics.exception.BusinessException;
import com.logistics.mapper.DeliveryBatchMapper;
import com.logistics.mapper.DeliveryBatchOrderMapper;
import com.logistics.mapper.DeliveryLocationMapper;
import com.logistics.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 配送批次控制器
 * 路线数据现在存储在 delivery_batches 表中（不再有 delivery_route 表）
 */
@Slf4j
@RestController
@RequestMapping("/delivery-batch")
@RequiredArgsConstructor
@Tag(name = "配送管理", description = "配送批次、实时追踪等相关接口")
public class DeliveryBatchController {

    private final DeliverySimulationService deliverySimulationService;
    private final TencentMapService tencentMapService;
    private final OrderService orderService;
    private final WarehouseService warehouseService;
    private final DeliveryLocationMapper deliveryLocationMapper;
    private final DeliveryBatchMapper deliveryBatchMapper;
    private final DeliveryBatchOrderMapper deliveryBatchOrderMapper;

    @Operation(summary = "开始运输批次")
    @PostMapping("/start-batch")
    public Result<Void> startBatch(@Parameter(description = "批次ID") @RequestParam Integer batchId) {
        log.info("开始运输批次: batchId={}", batchId);

        DeliveryBatch batch = deliveryBatchMapper.selectById(batchId);
        if (batch == null) throw new BusinessException("批次不存在");
        if (batch.getStatus() != 0) throw new BusinessException("批次状态不正确，只能开始待运输的批次");

        batch.setStatus(1);
        batch.setStartedAt(LocalDateTime.now());
        deliveryBatchMapper.updateById(batch);

        Warehouse warehouse = warehouseService.getById(batch.getWarehouseId());
        if (warehouse == null) throw new BusinessException("仓库不存在");

        List<Order> orders = orderService.getOrdersByBatchId(batchId);
        orderService.updateOrdersToInTransit(orders);

        if (orders.isEmpty()) {
            log.info("批次 {} 已开始运输（无订单，跳过路线规划）", batchId);
            return Result.success("开始运输成功", null);
        }

        // 调用腾讯地图规划路线
        String from = warehouse.getLatitude() + "," + warehouse.getLongitude();
        List<String> waypoints = orders.stream()
                .filter(order -> order.getAddress() != null)
                .map(order -> order.getAddress().getLatitude() + "," + order.getAddress().getLongitude())
                .collect(Collectors.toList());

        if (!waypoints.isEmpty()) {
            String to = waypoints.get(waypoints.size() - 1);
            String waypointsParam = waypoints.size() > 1 ?
                    String.join(";", waypoints.subList(0, waypoints.size() - 1)) : null;

            JSONObject routeResponse = tencentMapService.planRoute(from, to, waypointsParam);

            if (routeResponse != null) {
                JSONObject result = routeResponse.getJSONObject("result");
                JSONArray routes = result.getJSONArray("routes");
                JSONObject route = routes.getJSONObject(0);

                Integer distance = route.getInteger("distance");
                Integer duration = route.getInteger("duration");
                JSONArray polyline = route.getJSONArray("polyline");

                // 路线数据直接存入 delivery_batches
                batch.setRouteData(polyline.toJSONString());
                batch.setTotalDistance(distance);
                batch.setTotalDuration(duration);
                batch.setCurrentIndex(0);
                deliveryBatchMapper.updateById(batch);

                log.info("路线规划成功，批次ID: {}, 距离: {}m, 时长: {}s", batchId, distance, duration);

                // 启动模拟
                deliverySimulationService.startSimulation(batchId);
            } else {
                log.warn("路线规划失败，批次 {} 已开始但无路线", batchId);
            }
        }

        return Result.success("开始运输成功", null);
    }

    @Operation(summary = "获取批次路线详情")
    @GetMapping("/route-by-batch")
    public Result<RouteDetailResponse> getRouteByBatch(@Parameter(description = "批次ID") @RequestParam Integer batchId) {
        DeliveryBatch batch = deliveryBatchMapper.selectById(batchId);
        if (batch == null || batch.getRouteData() == null) {
            throw new BusinessException("该批次暂无路线数据");
        }
        return Result.success(buildRouteDetailResponse(batch));
    }

    @Operation(summary = "获取批次配送位置")
    @GetMapping("/location-by-batch")
    public Result<CurrentLocationResponse> getLocationByBatch(@Parameter(description = "批次ID") @RequestParam Integer batchId) {
        DeliveryBatch batch = deliveryBatchMapper.selectById(batchId);
        if (batch == null || batch.getRouteData() == null) {
            throw new BusinessException("该批次暂无配送记录");
        }
        return Result.success(buildLocationResponse(batch));
    }

    @Operation(summary = "完成批次配送")
    @PostMapping("/complete-batch")
    public Result<Void> completeBatch(@Parameter(description = "批次ID") @RequestParam Integer batchId) {
        DeliveryBatch batch = deliveryBatchMapper.selectById(batchId);
        if (batch == null) throw new BusinessException("批次不存在");
        if (batch.getStatus() != 1) throw new BusinessException("只能完成配送中的批次");

        batch.setStatus(2);
        batch.setCompletedAt(LocalDateTime.now());
        deliveryBatchMapper.updateById(batch);

        List<Order> orders = orderService.getOrdersByBatchId(batchId);
        List<Integer> orderIds = orders.stream().map(Order::getOrderId).collect(Collectors.toList());
        if (!orderIds.isEmpty()) {
            orderService.completeDelivery(orderIds);
        }

        log.info("批次 {} 配送完成", batchId);
        return Result.success("配送完成", null);
    }

    @Operation(summary = "订单物流追踪")
    @GetMapping("/track-by-order")
    public Result<?> trackByOrder(@Parameter(description = "订单ID") @RequestParam Integer orderId) {
        // 获取最新的批次关联（一个订单可能被重新分配过批次）
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<DeliveryBatchOrder> qw =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        qw.eq("order_id", orderId).orderByDesc("batch_id").last("LIMIT 1");
        DeliveryBatchOrder dbo = deliveryBatchOrderMapper.selectOne(qw);

        if (dbo == null) throw new BusinessException("该订单暂无物流信息");

        Integer batchId = dbo.getBatchId();
        DeliveryBatch batch = deliveryBatchMapper.selectById(batchId);
        if (batch == null) throw new BusinessException("批次信息不存在");

        JSONObject resultObj = new JSONObject();
        resultObj.put("batchId", batchId);
        resultObj.put("batchStatus", batch.getStatus());
        resultObj.put("startedAt", batch.getStartedAt());
        resultObj.put("completedAt", batch.getCompletedAt());

        Warehouse warehouse = warehouseService.getById(batch.getWarehouseId());
        if (warehouse != null) {
            resultObj.put("warehouseName", warehouse.getName());
            resultObj.put("warehouseAddress", warehouse.getAddress());
        }

        // 配送中或已完成 — 返回路线和位置数据
        if (batch.getStatus() >= 1 && batch.getRouteData() != null) {
            JSONArray polyline = JSON.parseArray(batch.getRouteData());
            resultObj.put("totalDistance", batch.getTotalDistance());
            resultObj.put("totalDuration", batch.getTotalDuration());

            // 检测格式: [{lng,lat,name}] (旧假数据) vs [num,num,...] (腾讯地图压缩)
            boolean isWaypointFormat = !polyline.isEmpty() && polyline.get(0) instanceof JSONObject;

            JSONArray pathPoints;
            if (isWaypointFormat) {
                pathPoints = new JSONArray();
                JSONArray frontendPolyline = new JSONArray();
                for (int i = 0; i < polyline.size(); i++) {
                    JSONObject wp = polyline.getJSONObject(i);
                    JSONArray point = new JSONArray();
                    point.add(wp.getDoubleValue("lat"));
                    point.add(wp.getDoubleValue("lng"));
                    pathPoints.add(point);
                    frontendPolyline.add(wp.getDoubleValue("lat"));
                    frontendPolyline.add(wp.getDoubleValue("lng"));
                }
                resultObj.put("polyline", frontendPolyline);
                resultObj.put("waypoints", polyline);
            } else {
                resultObj.put("polyline", polyline);
                pathPoints = tencentMapService.decompressPolyline(batch.getRouteData());
            }

            if (!pathPoints.isEmpty()) {
                int currentIndex = Math.min(
                    batch.getCurrentIndex() != null ? batch.getCurrentIndex() : 0,
                    pathPoints.size() - 1
                );
                JSONArray currentPoint = pathPoints.getJSONArray(currentIndex);

                resultObj.put("currentLat", currentPoint.getDoubleValue(0));
                resultObj.put("currentLng", currentPoint.getDoubleValue(1));

                double progress = pathPoints.size() > 1 ? (double) currentIndex / (pathPoints.size() - 1) * 100 : 0;
                resultObj.put("progress", BigDecimal.valueOf(progress).setScale(1, RoundingMode.HALF_UP));

                double remainingRatio = 1.0 - (double) currentIndex / Math.max(pathPoints.size() - 1, 1);
                resultObj.put("remainingTime", BigDecimal.valueOf(batch.getTotalDuration() * remainingRatio).setScale(0, RoundingMode.HALF_UP));
            }
        }

        return Result.success("查询成功", resultObj);
    }

    @Operation(summary = "停止配送")
    @PostMapping("/stop")
    public Result<Void> stopDelivery(@Parameter(description = "批次ID") @RequestParam Integer batchId) {
        deliverySimulationService.stopSimulation(batchId);
        return Result.success();
    }

    // ── Private helpers ──

    private CurrentLocationResponse buildLocationResponse(DeliveryBatch batch) {
        JSONArray pathPoints = tencentMapService.decompressPolyline(batch.getRouteData());
        if (pathPoints.isEmpty()) throw new BusinessException("路径数据异常");

        int currentIndex = Math.min(
            batch.getCurrentIndex() != null ? batch.getCurrentIndex() : 0,
            pathPoints.size() - 1
        );
        JSONArray currentPoint = pathPoints.getJSONArray(currentIndex);

        DeliveryLocation location = deliveryLocationMapper.selectLatestByBatchId(batch.getId());

        CurrentLocationResponse response = new CurrentLocationResponse();
        response.setLatitude(new BigDecimal(currentPoint.getDoubleValue(0)));
        response.setLongitude(new BigDecimal(currentPoint.getDoubleValue(1)));
        response.setAddress(location != null ? location.getAddress() : "位置获取中...");
        response.setStatus(batch.getStatus() == 1 ? "DELIVERING" : "COMPLETED");
        response.setProgress((currentIndex * 100) / pathPoints.size());

        int remainingPoints = pathPoints.size() - currentIndex;
        BigDecimal totalDistKm = new BigDecimal(batch.getTotalDistance()).divide(new BigDecimal(1000), 2, RoundingMode.HALF_UP);
        response.setRemainingDistance(totalDistKm
                .multiply(new BigDecimal(remainingPoints))
                .divide(new BigDecimal(pathPoints.size()), 2, RoundingMode.HALF_UP));
        response.setRemainingTime((batch.getTotalDuration() * remainingPoints) / pathPoints.size());

        return response;
    }

    private RouteDetailResponse buildRouteDetailResponse(DeliveryBatch batch) {
        RouteDetailResponse response = new RouteDetailResponse();
        response.setRouteId(batch.getId().longValue());
        response.setPolyline(JSON.parseArray(batch.getRouteData()));
        response.setTotalDistance(new BigDecimal(batch.getTotalDistance()).divide(new BigDecimal(1000), 2, RoundingMode.HALF_UP));
        response.setTotalDuration(batch.getTotalDuration());
        response.setStatus(batch.getStatus() == 1 ? "DELIVERING" : batch.getStatus() == 2 ? "COMPLETED" : "PENDING");
        return response;
    }
}
