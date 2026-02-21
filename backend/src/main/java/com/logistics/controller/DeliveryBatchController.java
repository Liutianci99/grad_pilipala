package com.logistics.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.logistics.common.Result;
import com.logistics.dto.CurrentLocationResponse;
import com.logistics.dto.RouteDetailResponse;
import com.logistics.dto.StartDeliveryRequest;
import com.logistics.entity.DeliveryBatch;
import com.logistics.entity.DeliveryBatchOrder;
import com.logistics.entity.DeliveryLocation;
import com.logistics.entity.DeliveryRoute;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 配送批次控制器
 */
@Slf4j
@RestController
@RequestMapping("/delivery-batch")
@RequiredArgsConstructor
@Tag(name = "配送管理", description = "配送批次、实时追踪等相关接口")
public class DeliveryBatchController {

    private final DeliveryRouteService deliveryRouteService;
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
        if (batch == null) {
            throw new BusinessException("批次不存在");
        }
        if (batch.getStatus() != 0) {
            throw new BusinessException("批次状态不正确，只能开始待运输的批次");
        }

        batch.setStatus(1);
        batch.setStartedAt(LocalDateTime.now());
        deliveryBatchMapper.updateById(batch);

        Warehouse warehouse = warehouseService.getById(batch.getWarehouseId());
        if (warehouse == null) {
            throw new BusinessException("仓库不存在");
        }

        List<Order> orders = orderService.getOrdersByBatchId(batchId);
        // 更新批次内所有订单状态为运输中(3)
        orderService.updateOrdersToInTransit(orders);

        if (orders.isEmpty()) {
            log.info("批次 {} 已开始运输（无订单，跳过路线规划）", batchId);
            return Result.success("开始运输成功", null);
        }

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

                DeliveryRoute deliveryRoute = new DeliveryRoute();
                deliveryRoute.setBatchId(batchId);
                deliveryRoute.setDeliveryTime(batch.getStartedAt());
                deliveryRoute.setWarehouseId(batch.getWarehouseId());
                deliveryRoute.setRouteData(polyline.toJSONString());
                deliveryRoute.setTotalDistance(
                        new BigDecimal(distance).divide(new BigDecimal(1000), 2, RoundingMode.HALF_UP)
                );
                deliveryRoute.setTotalDuration(duration);
                deliveryRoute.setStatus("DELIVERING");
                deliveryRoute.setCurrentIndex(0);
                deliveryRoute.setStartedAt(LocalDateTime.now());

                deliveryRouteService.save(deliveryRoute);

                batch.setTotalDistance(distance);
                batch.setTotalDuration(duration);
                deliveryBatchMapper.updateById(batch);

                log.info("路线规划成功，路线ID: {}, 距离: {}m, 时长: {}min", deliveryRoute.getId(), distance, duration);

                deliverySimulationService.startSimulation(deliveryRoute.getId());
            } else {
                log.warn("路线规划失败，批次 {} 已开始但无路线", batchId);
            }
        }

        log.info("批次 {} 已开始运输", batchId);
        return Result.success("开始运输成功", null);
    }

    @Operation(summary = "开始配送")
    @PostMapping("/start")
    public Result<Long> startDelivery(@Parameter(description = "配送开始请求信息") @RequestBody StartDeliveryRequest request) {
        log.info("开始运输请求: deliveryTime={}, warehouseId={}", request.getDeliveryTime(), request.getWarehouseId());

        LocalDateTime deliveryTime = LocalDateTime.parse(request.getDeliveryTime(), DateTimeFormatter.ISO_DATE_TIME);

        DeliveryRoute existingRoute = deliveryRouteService.getByDeliveryTimeAndWarehouse(deliveryTime, request.getWarehouseId());
        if (existingRoute != null && "DELIVERING".equals(existingRoute.getStatus())) {
            throw new BusinessException("该批次已在运输中");
        }

        List<Order> orders = orderService.getDeliveryBatchOrders(deliveryTime, request.getWarehouseId());
        if (orders.isEmpty()) {
            throw new BusinessException("该批次没有订单");
        }

        log.info("获取到 {} 个订单", orders.size());

        Warehouse warehouse = warehouseService.getById(request.getWarehouseId());
        if (warehouse == null) {
            throw new BusinessException("仓库不存在");
        }

        String from = warehouse.getLatitude() + "," + warehouse.getLongitude();
        List<String> waypoints = orders.stream()
                .filter(order -> order.getAddress() != null)
                .map(order -> order.getAddress().getLatitude() + "," + order.getAddress().getLongitude())
                .collect(Collectors.toList());

        if (waypoints.isEmpty()) {
            throw new BusinessException("没有有效的配送地址");
        }

        String to = waypoints.get(waypoints.size() - 1);
        String waypointsParam = waypoints.size() > 1 ?
                String.join(";", waypoints.subList(0, waypoints.size() - 1)) : null;

        JSONObject routeResponse = tencentMapService.planRoute(from, to, waypointsParam);
        if (routeResponse == null) {
            throw new BusinessException("路线规划失败");
        }

        JSONObject result = routeResponse.getJSONObject("result");
        JSONArray routesArr = result.getJSONArray("routes");
        JSONObject route = routesArr.getJSONObject(0);

        Integer distance = route.getInteger("distance");
        Integer duration = route.getInteger("duration");
        JSONArray polyline = route.getJSONArray("polyline");

        DeliveryRoute deliveryRoute = new DeliveryRoute();
        deliveryRoute.setDeliveryTime(deliveryTime);
        deliveryRoute.setWarehouseId(request.getWarehouseId());
        deliveryRoute.setRouteData(polyline.toJSONString());
        deliveryRoute.setTotalDistance(new BigDecimal(distance).divide(new BigDecimal(1000), 2, RoundingMode.HALF_UP));
        deliveryRoute.setTotalDuration(duration);
        deliveryRoute.setStatus("DELIVERING");
        deliveryRoute.setCurrentIndex(0);
        deliveryRoute.setStartedAt(LocalDateTime.now());

        deliveryRouteService.save(deliveryRoute);

        log.info("路线保存成功，ID: {}, 距离: {}km, 时长: {}分钟",
                deliveryRoute.getId(), deliveryRoute.getTotalDistance(), deliveryRoute.getTotalDuration());

        deliverySimulationService.startSimulation(deliveryRoute.getId());

        return Result.success(deliveryRoute.getId());
    }

    @Operation(summary = "获取配送实时位置")
    @GetMapping("/current-location")
    public Result<CurrentLocationResponse> getCurrentLocation(
            @Parameter(description = "配送时间") @RequestParam String deliveryTime,
            @Parameter(description = "仓库ID") @RequestParam Integer warehouseId) {

        LocalDateTime time = LocalDateTime.parse(deliveryTime, DateTimeFormatter.ISO_DATE_TIME);
        DeliveryRoute route = deliveryRouteService.getByDeliveryTimeAndWarehouse(time, warehouseId);
        if (route == null) {
            throw new BusinessException("未找到配送路线");
        }

        return Result.success(buildLocationResponse(route));
    }

    @Operation(summary = "获取路线详情")
    @GetMapping("/route-detail")
    public Result<RouteDetailResponse> getRouteDetail(
            @Parameter(description = "配送时间") @RequestParam String deliveryTime,
            @Parameter(description = "仓库ID") @RequestParam Integer warehouseId) {

        LocalDateTime time = LocalDateTime.parse(deliveryTime, DateTimeFormatter.ISO_DATE_TIME);
        DeliveryRoute route = deliveryRouteService.getByDeliveryTimeAndWarehouse(time, warehouseId);
        if (route == null) {
            throw new BusinessException("未找到路线数据");
        }

        return Result.success(buildRouteDetailResponse(route));
    }

    @Operation(summary = "获取批次路线详情")
    @GetMapping("/route-by-batch")
    public Result<RouteDetailResponse> getRouteByBatch(@Parameter(description = "批次ID") @RequestParam Integer batchId) {
        DeliveryRoute route = deliveryRouteService.getByBatchId(batchId);
        if (route == null) {
            throw new BusinessException("该批次暂无路线数据");
        }
        return Result.success(buildRouteDetailResponse(route));
    }

    @Operation(summary = "获取批次配送位置")
    @GetMapping("/location-by-batch")
    public Result<CurrentLocationResponse> getLocationByBatch(@Parameter(description = "批次ID") @RequestParam Integer batchId) {
        DeliveryRoute route = deliveryRouteService.getByBatchId(batchId);
        if (route == null) {
            throw new BusinessException("该批次暂无配送记录");
        }
        return Result.success(buildLocationResponse(route));
    }

    @Operation(summary = "完成批次配送")
    @PostMapping("/complete-batch")
    public Result<Void> completeBatch(@Parameter(description = "批次ID") @RequestParam Integer batchId) {
        DeliveryBatch batch = deliveryBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException("批次不存在");
        }
        if (batch.getStatus() != 1) {
            throw new BusinessException("只能完成配送中的批次");
        }

        batch.setStatus(2);
        batch.setCompletedAt(LocalDateTime.now());
        deliveryBatchMapper.updateById(batch);

        DeliveryRoute route = deliveryRouteService.getByBatchId(batchId);
        if (route != null && "DELIVERING".equals(route.getStatus())) {
            route.setStatus("COMPLETED");
            route.setCompletedAt(LocalDateTime.now());
            deliveryRouteService.updateById(route);
        }

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
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<DeliveryBatchOrder> qw =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        qw.eq("order_id", orderId);
        DeliveryBatchOrder dbo = deliveryBatchOrderMapper.selectOne(qw);

        if (dbo == null) {
            throw new BusinessException("该订单暂无物流信息");
        }

        Integer batchId = dbo.getBatchId();
        DeliveryBatch batch = deliveryBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException("批次信息不存在");
        }

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

        if (batch.getStatus() == 1) {
            DeliveryRoute route = deliveryRouteService.getByBatchId(batchId);
            if (route != null) {
                JSONArray polyline = JSON.parseArray(route.getRouteData());
                resultObj.put("totalDistance", route.getTotalDistance());
                resultObj.put("totalDuration", route.getTotalDuration());

                // Detect format: [{lng,lat,name}] (fake/waypoint) vs [num,num,...] (compressed)
                boolean isWaypointFormat = !polyline.isEmpty() && polyline.get(0) instanceof JSONObject;

                JSONArray pathPoints;
                if (isWaypointFormat) {
                    // Waypoint objects — convert to [[lat,lng], ...] for consistency
                    pathPoints = new JSONArray();
                    JSONArray frontendPolyline = new JSONArray();
                    for (int i = 0; i < polyline.size(); i++) {
                        JSONObject wp = polyline.getJSONObject(i);
                        JSONArray point = new JSONArray();
                        point.add(wp.getDoubleValue("lat"));
                        point.add(wp.getDoubleValue("lng"));
                        pathPoints.add(point);
                        // Build flat array for frontend map: [lat1,lng1,lat2,lng2,...]
                        frontendPolyline.add(wp.getDoubleValue("lat"));
                        frontendPolyline.add(wp.getDoubleValue("lng"));
                    }
                    resultObj.put("polyline", frontendPolyline);
                    resultObj.put("waypoints", polyline);
                } else {
                    // Compressed polyline from Tencent Maps API
                    resultObj.put("polyline", polyline);
                    pathPoints = tencentMapService.decompressPolyline(route.getRouteData());
                }

                if (!pathPoints.isEmpty()) {
                    int currentIndex = Math.min(route.getCurrentIndex(), pathPoints.size() - 1);
                    JSONArray currentPoint = pathPoints.getJSONArray(currentIndex);

                    resultObj.put("currentLat", currentPoint.getDoubleValue(0));
                    resultObj.put("currentLng", currentPoint.getDoubleValue(1));

                    double progress = pathPoints.size() > 1 ? (double) currentIndex / (pathPoints.size() - 1) * 100 : 0;
                    resultObj.put("progress", BigDecimal.valueOf(progress).setScale(1, RoundingMode.HALF_UP));

                    double remainingRatio = 1.0 - (double) currentIndex / Math.max(pathPoints.size() - 1, 1);
                    resultObj.put("remainingTime", BigDecimal.valueOf(route.getTotalDuration() * remainingRatio).setScale(0, RoundingMode.HALF_UP));
                }
            }
        }

        return Result.success("查询成功", resultObj);
    }

    @Operation(summary = "停止配送")
    @PostMapping("/stop")
    public Result<Void> stopDelivery(@Parameter(description = "路线ID") @RequestParam Long routeId) {
        deliverySimulationService.stopSimulation(routeId);
        return Result.success();
    }

    // ── Private helpers ──

    private CurrentLocationResponse buildLocationResponse(DeliveryRoute route) {
        JSONArray pathPoints = tencentMapService.decompressPolyline(route.getRouteData());
        if (pathPoints.isEmpty()) {
            throw new BusinessException("路径数据异常");
        }

        int currentIndex = Math.min(route.getCurrentIndex(), pathPoints.size() - 1);
        JSONArray currentPoint = pathPoints.getJSONArray(currentIndex);

        DeliveryLocation location = deliveryLocationMapper.selectLatestByRouteId(route.getId());

        CurrentLocationResponse response = new CurrentLocationResponse();
        response.setLatitude(new BigDecimal(currentPoint.getDoubleValue(0)));
        response.setLongitude(new BigDecimal(currentPoint.getDoubleValue(1)));
        response.setAddress(location != null ? location.getAddress() : "位置获取中...");
        response.setStatus(route.getStatus());
        response.setProgress((currentIndex * 100) / pathPoints.size());

        int remainingPoints = pathPoints.size() - currentIndex;
        response.setRemainingDistance(route.getTotalDistance()
                .multiply(new BigDecimal(remainingPoints))
                .divide(new BigDecimal(pathPoints.size()), 2, RoundingMode.HALF_UP));
        response.setRemainingTime((route.getTotalDuration() * remainingPoints) / pathPoints.size());

        return response;
    }

    private RouteDetailResponse buildRouteDetailResponse(DeliveryRoute route) {
        RouteDetailResponse response = new RouteDetailResponse();
        response.setRouteId(route.getId());
        response.setPolyline(JSON.parseArray(route.getRouteData()));
        response.setTotalDistance(route.getTotalDistance());
        response.setTotalDuration(route.getTotalDuration());
        response.setStatus(route.getStatus());
        return response;
    }
}
