package com.logistics.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.logistics.common.Result;
import com.logistics.dto.CurrentLocationResponse;
import com.logistics.dto.RouteDetailResponse;
import com.logistics.dto.StartDeliveryRequest;
import com.logistics.entity.DeliveryLocation;
import com.logistics.entity.DeliveryRoute;
import com.logistics.entity.Order;
import com.logistics.entity.Warehouse;
import com.logistics.mapper.DeliveryLocationMapper;
import com.logistics.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 配送批次控制器
 * 处理配送员实时追踪相关请求
 */
@Slf4j
@RestController
@RequestMapping("/delivery-batch")
@Tag(name = "配送管理", description = "配送批次、实时追踪等相关接口")
public class DeliveryBatchController {

    @Autowired
    private DeliveryRouteService deliveryRouteService;

    @Autowired
    private DeliverySimulationService deliverySimulationService;

    @Autowired
    private TencentMapService tencentMapService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private DeliveryLocationMapper deliveryLocationMapper;

    @Autowired
    private com.logistics.mapper.DeliveryBatchMapper deliveryBatchMapper;

    @Autowired
    private com.logistics.mapper.DeliveryBatchOrderMapper deliveryBatchOrderMapper;

    /**
     * 开始运输批次（更新批次状态 + 规划路线 + 启动模拟）
     *
     * @param batchId 批次ID
     * @return 成功/失败
     */
    @Operation(summary = "开始运输批次", description = "配送员开始运输，规划路线并启动配送模拟")
    @PostMapping("/start-batch")
    public Result<Void> startBatch(@Parameter(description = "批次ID") @RequestParam Integer batchId) {
        try {
            log.info("开始运输批次: batchId={}", batchId);

            // 1. 查询批次
            com.logistics.entity.DeliveryBatch batch = deliveryBatchMapper.selectById(batchId);
            if (batch == null) {
                return Result.error("批次不存在");
            }

            if (batch.getStatus() != 0) {
                return Result.error("批次状态不正确，只能开始待运输的批次");
            }

            // 2. 更新批次状态为运输中
            batch.setStatus(1);
            batch.setStartedAt(LocalDateTime.now());
            deliveryBatchMapper.updateById(batch);

            // 3. Get orders for this batch
            Warehouse warehouse = warehouseService.getById(batch.getWarehouseId());
            if (warehouse == null) {
                return Result.error("仓库不存在");
            }

            List<Order> orders = orderService.getOrdersByBatchId(batchId);
            if (orders.isEmpty()) {
                log.info("批次 {} 已开始运输（无订单，跳过路线规划）", batchId);
                return Result.success("开始运输成功", null);
            }

            // 4. 规划路线
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

                    // 5. 保存路线
                    DeliveryRoute deliveryRoute = new DeliveryRoute();
                    deliveryRoute.setBatchId(batchId);
                    deliveryRoute.setDeliveryTime(batch.getStartedAt());
                    deliveryRoute.setWarehouseId(batch.getWarehouseId());
                    deliveryRoute.setRouteData(polyline.toJSONString());
                    deliveryRoute.setTotalDistance(
                            new java.math.BigDecimal(distance).divide(new java.math.BigDecimal(1000), 2, java.math.RoundingMode.HALF_UP)
                    );
                    deliveryRoute.setTotalDuration(duration);
                    deliveryRoute.setStatus("DELIVERING");
                    deliveryRoute.setCurrentIndex(0);
                    deliveryRoute.setStartedAt(LocalDateTime.now());

                    deliveryRouteService.save(deliveryRoute);

                    // 6. 更新批次的距离和时长
                    batch.setTotalDistance(distance);
                    batch.setTotalDuration(duration);
                    deliveryBatchMapper.updateById(batch);

                    log.info("路线规划成功，路线ID: {}, 距离: {}m, 时长: {}min", deliveryRoute.getId(), distance, duration);

                    // 7. 启动配送模拟
                    deliverySimulationService.startSimulation(deliveryRoute.getId());
                } else {
                    log.warn("路线规划失败，批次 {} 已开始但无路线", batchId);
                }
            }

            log.info("批次 {} 已开始运输", batchId);
            return Result.success("开始运输成功", null);

        } catch (Exception e) {
            log.error("开始运输批次失败", e);
            return Result.error("开始运输失败: " + e.getMessage());
        }
    }

    /**
     * 开始运输
     *
     * @param request 开始运输请求
     * @return 路线ID
     */
    @Operation(summary = "开始配送", description = "配送员开始配送，系统调用地图服务规划最优路线")
    @PostMapping("/start")
    public Result<Long> startDelivery(@Parameter(description = "配送开始请求信息") @RequestBody StartDeliveryRequest request) {
        try {
            log.info("开始运输请求: deliveryTime={}, warehouseId={}",
                    request.getDeliveryTime(), request.getWarehouseId());

            // 1. 解析配送时间
            LocalDateTime deliveryTime = LocalDateTime.parse(
                    request.getDeliveryTime(),
                    DateTimeFormatter.ISO_DATE_TIME
            );

            // 2. 检查是否已存在路线
            DeliveryRoute existingRoute = deliveryRouteService.getByDeliveryTimeAndWarehouse(
                    deliveryTime, request.getWarehouseId()
            );

            if (existingRoute != null && "DELIVERING".equals(existingRoute.getStatus())) {
                return Result.error("该批次已在运输中");
            }

            // 3. 获取批次订单
            List<Order> orders = orderService.getDeliveryBatchOrders(
                    deliveryTime, request.getWarehouseId()
            );

            if (orders.isEmpty()) {
                return Result.error("该批次没有订单");
            }

            log.info("获取到 {} 个订单", orders.size());

            // 4. 获取仓库信息
            Warehouse warehouse = warehouseService.getById(request.getWarehouseId());
            if (warehouse == null) {
                return Result.error("仓库不存在");
            }

            // 5. 构建路线规划参数
            String from = warehouse.getLatitude() + "," + warehouse.getLongitude();

            // 获取所有配送地址
            List<String> waypoints = orders.stream()
                    .filter(order -> order.getAddress() != null)
                    .map(order -> order.getAddress().getLatitude() + "," +
                                 order.getAddress().getLongitude())
                    .collect(Collectors.toList());

            if (waypoints.isEmpty()) {
                return Result.error("没有有效的配送地址");
            }

            String to = waypoints.get(waypoints.size() - 1);
            String waypointsParam = waypoints.size() > 1 ?
                    String.join(";", waypoints.subList(0, waypoints.size() - 1)) : null;

            // 6. 调用腾讯地图API规划路线
            JSONObject routeResponse = tencentMapService.planRoute(from, to, waypointsParam);

            if (routeResponse == null) {
                return Result.error("路线规划失败");
            }

            // 7. 解析路线数据
            JSONObject result = routeResponse.getJSONObject("result");
            JSONArray routes = result.getJSONArray("routes");
            JSONObject route = routes.getJSONObject(0);

            Integer distance = route.getInteger("distance");  // 米
            Integer duration = route.getInteger("duration");  // 分钟
            JSONArray polyline = route.getJSONArray("polyline");

            // 8. 保存路线数据
            DeliveryRoute deliveryRoute = new DeliveryRoute();
            deliveryRoute.setDeliveryTime(deliveryTime);
            deliveryRoute.setWarehouseId(request.getWarehouseId());
            deliveryRoute.setRouteData(polyline.toJSONString());
            deliveryRoute.setTotalDistance(
                    new BigDecimal(distance).divide(new BigDecimal(1000), 2, RoundingMode.HALF_UP)
            );
            deliveryRoute.setTotalDuration(duration);
            deliveryRoute.setStatus("DELIVERING");
            deliveryRoute.setCurrentIndex(0);
            deliveryRoute.setStartedAt(LocalDateTime.now());

            deliveryRouteService.save(deliveryRoute);

            log.info("路线保存成功，ID: {}, 距离: {}km, 时长: {}分钟",
                    deliveryRoute.getId(), deliveryRoute.getTotalDistance(), deliveryRoute.getTotalDuration());

            // 9. 启动配送模拟
            deliverySimulationService.startSimulation(deliveryRoute.getId());

            return Result.success(deliveryRoute.getId());

        } catch (Exception e) {
            log.error("开始运输失败", e);
            return Result.error("开始运输失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前位置
     *
     * @param deliveryTime 配送时间
     * @param warehouseId  仓库ID
     * @return 当前位置信息
     */
    @Operation(summary = "获取配送实时位置", description = "获取当前配送批次的实时位置、进度和剩余时间")
    @GetMapping("/current-location")
    public Result<CurrentLocationResponse> getCurrentLocation(
            @Parameter(description = "配送时间") @RequestParam String deliveryTime,
            @Parameter(description = "仓库ID") @RequestParam Integer warehouseId) {

        try {
            // 解析时间
            LocalDateTime time = LocalDateTime.parse(deliveryTime, DateTimeFormatter.ISO_DATE_TIME);

            // 查询路线
            DeliveryRoute route = deliveryRouteService.getByDeliveryTimeAndWarehouse(time, warehouseId);

            if (route == null) {
                return Result.error("未找到配送路线");
            }

            // 解压坐标
            JSONArray pathPoints = tencentMapService.decompressPolyline(route.getRouteData());

            if (pathPoints.isEmpty()) {
                return Result.error("路径数据异常");
            }

            // 获取当前位置
            int currentIndex = route.getCurrentIndex();
            if (currentIndex >= pathPoints.size()) {
                currentIndex = pathPoints.size() - 1;
            }

            JSONArray currentPoint = pathPoints.getJSONArray(currentIndex);
            double latitude = currentPoint.getDoubleValue(0);
            double longitude = currentPoint.getDoubleValue(1);

            // 查询最近的位置记录（包含地址）
            DeliveryLocation location = deliveryLocationMapper.selectLatestByRouteId(route.getId());

            // 构建响应
            CurrentLocationResponse response = new CurrentLocationResponse();
            response.setLatitude(new BigDecimal(latitude));
            response.setLongitude(new BigDecimal(longitude));
            response.setAddress(location != null ? location.getAddress() : "位置获取中...");
            response.setStatus(route.getStatus());

            // 计算进度
            int progress = (currentIndex * 100) / pathPoints.size();
            response.setProgress(progress);

            // 计算剩余距离和时间
            int remainingPoints = pathPoints.size() - currentIndex;
            BigDecimal remainingDistance = route.getTotalDistance()
                    .multiply(new BigDecimal(remainingPoints))
                    .divide(new BigDecimal(pathPoints.size()), 2, RoundingMode.HALF_UP);
            response.setRemainingDistance(remainingDistance);

            int remainingTime = (route.getTotalDuration() * remainingPoints) / pathPoints.size();
            response.setRemainingTime(remainingTime);

            return Result.success(response);

        } catch (Exception e) {
            log.error("获取当前位置失败", e);
            return Result.error("获取当前位置失败: " + e.getMessage());
        }
    }

    /**
     * 获取路线详情
     *
     * @param deliveryTime 配送时间
     * @param warehouseId  仓库ID
     * @return 路线详情
     */
    @Operation(summary = "获取路线详情", description = "获取路线规划数据，包括polyline坐标")
    @GetMapping("/route-detail")
    public Result<RouteDetailResponse> getRouteDetail(
            @Parameter(description = "配送时间") @RequestParam String deliveryTime,
            @Parameter(description = "仓库ID") @RequestParam Integer warehouseId) {
        try {
            // 解析时间
            LocalDateTime time = LocalDateTime.parse(deliveryTime, DateTimeFormatter.ISO_DATE_TIME);

            // 查询路线
            DeliveryRoute route = deliveryRouteService.getByDeliveryTimeAndWarehouse(time, warehouseId);

            if (route == null) {
                return Result.error("未找到路线数据");
            }

            // 构建响应
            RouteDetailResponse response = new RouteDetailResponse();
            response.setRouteId(route.getId());
            response.setPolyline(JSON.parseArray(route.getRouteData()));
            response.setTotalDistance(route.getTotalDistance());
            response.setTotalDuration(route.getTotalDuration());
            response.setStatus(route.getStatus());

            return Result.success(response);

        } catch (Exception e) {
            log.error("获取路线详情失败", e);
            return Result.error("获取路线详情失败: " + e.getMessage());
        }
    }

    /**
     * 根据批次ID获取路线详情
     */
    @Operation(summary = "获取批次路线详情", description = "根据批次ID获取路线规划数据")
    @GetMapping("/route-by-batch")
    public Result<RouteDetailResponse> getRouteByBatch(@Parameter(description = "批次ID") @RequestParam Integer batchId) {
        try {
            DeliveryRoute route = deliveryRouteService.getByBatchId(batchId);
            if (route == null) {
                return Result.error("该批次暂无路线数据");
            }

            RouteDetailResponse response = new RouteDetailResponse();
            response.setRouteId(route.getId());
            response.setPolyline(JSON.parseArray(route.getRouteData()));
            response.setTotalDistance(route.getTotalDistance());
            response.setTotalDuration(route.getTotalDuration());
            response.setStatus(route.getStatus());
            return Result.success(response);
        } catch (Exception e) {
            log.error("获取批次路线失败", e);
            return Result.error("获取路线失败: " + e.getMessage());
        }
    }

    /**
     * 根据批次ID获取当前位置
     */
    @Operation(summary = "获取批次配送位置", description = "根据批次ID获取实时配送位置")
    @GetMapping("/location-by-batch")
    public Result<CurrentLocationResponse> getLocationByBatch(@Parameter(description = "批次ID") @RequestParam Integer batchId) {
        try {
            DeliveryRoute route = deliveryRouteService.getByBatchId(batchId);
            if (route == null) {
                return Result.error("该批次暂无配送记录");
            }

            JSONArray pathPoints = tencentMapService.decompressPolyline(route.getRouteData());
            if (pathPoints.isEmpty()) {
                return Result.error("路径数据异常");
            }

            int currentIndex = route.getCurrentIndex();
            if (currentIndex >= pathPoints.size()) {
                currentIndex = pathPoints.size() - 1;
            }

            JSONArray currentPoint = pathPoints.getJSONArray(currentIndex);
            double latitude = currentPoint.getDoubleValue(0);
            double longitude = currentPoint.getDoubleValue(1);

            DeliveryLocation location = deliveryLocationMapper.selectLatestByRouteId(route.getId());

            CurrentLocationResponse response = new CurrentLocationResponse();
            response.setLatitude(new BigDecimal(latitude));
            response.setLongitude(new BigDecimal(longitude));
            response.setAddress(location != null ? location.getAddress() : "位置获取中...");
            response.setStatus(route.getStatus());

            int progress = (currentIndex * 100) / pathPoints.size();
            response.setProgress(progress);

            int remainingPoints = pathPoints.size() - currentIndex;
            BigDecimal remainingDistance = route.getTotalDistance()
                    .multiply(new BigDecimal(remainingPoints))
                    .divide(new BigDecimal(pathPoints.size()), 2, RoundingMode.HALF_UP);
            response.setRemainingDistance(remainingDistance);

            int remainingTime = (route.getTotalDuration() * remainingPoints) / pathPoints.size();
            response.setRemainingTime(remainingTime);

            return Result.success(response);
        } catch (Exception e) {
            log.error("获取批次位置失败", e);
            return Result.error("获取位置失败: " + e.getMessage());
        }
    }

    /**
     * 完成批次配送
     */
    @Operation(summary = "完成批次配送", description = "配送员确认完成整个批次的配送")
    @PostMapping("/complete-batch")
    public Result<Void> completeBatch(@Parameter(description = "批次ID") @RequestParam Integer batchId) {
        try {
            com.logistics.entity.DeliveryBatch batch = deliveryBatchMapper.selectById(batchId);
            if (batch == null) {
                return Result.error("批次不存在");
            }
            if (batch.getStatus() != 1) {
                return Result.error("只能完成配送中的批次");
            }

            // 更新批次状态
            batch.setStatus(2);
            batch.setCompletedAt(LocalDateTime.now());
            deliveryBatchMapper.updateById(batch);

            // 停止路线模拟
            DeliveryRoute route = deliveryRouteService.getByBatchId(batchId);
            if (route != null && "DELIVERING".equals(route.getStatus())) {
                route.setStatus("COMPLETED");
                route.setCompletedAt(LocalDateTime.now());
                deliveryRouteService.updateById(route);
            }

            // 更新所有订单状态为已到达(4)
            List<Order> orders = orderService.getOrdersByBatchId(batchId);
            List<Integer> orderIds = orders.stream().map(Order::getOrderId).collect(Collectors.toList());
            if (!orderIds.isEmpty()) {
                orderService.completeDelivery(orderIds);
            }

            log.info("批次 {} 配送完成", batchId);
            return Result.success("配送完成", null);
        } catch (Exception e) {
            log.error("完成批次配送失败", e);
            return Result.error("完成配送失败: " + e.getMessage());
        }
    }

    /**
     * 停止配送
     *
     * @param routeId 路线ID
     * @return 成功/失败
     */
    /**
     * 根据订单ID查询物流追踪信息（顾客用）
     */
    @Operation(summary = "订单物流追踪", description = "根据订单ID查询所属批次的物流信息，包括路线和实时位置")
    @GetMapping("/track-by-order")
    public Result<?> trackByOrder(@Parameter(description = "订单ID") @RequestParam Integer orderId) {
        try {
            // 1. Find which batch this order belongs to
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.logistics.entity.DeliveryBatchOrder> qw =
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            qw.eq("order_id", orderId);
            com.logistics.entity.DeliveryBatchOrder dbo = deliveryBatchOrderMapper.selectOne(qw);

            if (dbo == null) {
                return Result.error("该订单暂无物流信息");
            }

            Integer batchId = dbo.getBatchId();

            // 2. Get batch info
            com.logistics.entity.DeliveryBatch batch = deliveryBatchMapper.selectById(batchId);
            if (batch == null) {
                return Result.error("批次信息不存在");
            }

            // 3. Build response
            JSONObject result = new JSONObject();
            result.put("batchId", batchId);
            result.put("batchStatus", batch.getStatus()); // 0=待出发, 1=配送中, 2=已完成
            result.put("startedAt", batch.getStartedAt());
            result.put("completedAt", batch.getCompletedAt());

            // 4. Get warehouse info
            Warehouse warehouse = warehouseService.getById(batch.getWarehouseId());
            if (warehouse != null) {
                result.put("warehouseName", warehouse.getName());
                result.put("warehouseAddress", warehouse.getAddress());
            }

            // 5. If delivering, get route + location
            if (batch.getStatus() == 1) {
                DeliveryRoute route = deliveryRouteService.getByBatchId(batchId);
                if (route != null) {
                    // Route polyline
                    JSONArray polyline = JSON.parseArray(route.getRouteData());
                    result.put("polyline", polyline);
                    result.put("totalDistance", route.getTotalDistance());
                    result.put("totalDuration", route.getTotalDuration());

                    // Current location
                    JSONArray pathPoints = tencentMapService.decompressPolyline(route.getRouteData());
                    if (!pathPoints.isEmpty()) {
                        int currentIndex = route.getCurrentIndex();
                        if (currentIndex >= pathPoints.size()) currentIndex = pathPoints.size() - 1;
                        JSONArray currentPoint = pathPoints.getJSONArray(currentIndex);
                        double lat = currentPoint.getDoubleValue(0);
                        double lng = currentPoint.getDoubleValue(1);

                        result.put("currentLat", lat);
                        result.put("currentLng", lng);

                        // Progress
                        double progress = pathPoints.size() > 1 ? (double) currentIndex / (pathPoints.size() - 1) * 100 : 0;
                        result.put("progress", BigDecimal.valueOf(progress).setScale(1, RoundingMode.HALF_UP));

                        // Remaining time
                        double remainingRatio = 1.0 - (double) currentIndex / Math.max(pathPoints.size() - 1, 1);
                        double remainingMinutes = route.getTotalDuration() * remainingRatio;
                        result.put("remainingTime", BigDecimal.valueOf(remainingMinutes).setScale(0, RoundingMode.HALF_UP));
                    }
                }
            }

            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("查询订单物流失败, orderId={}", orderId, e);
            return Result.error("查询物流信息失败: " + e.getMessage());
        }
    }

    @Operation(summary = "停止配送", description = "配送员停止当前配送批次")
    @PostMapping("/stop")
    public Result<Void> stopDelivery(@Parameter(description = "路线ID") @RequestParam Long routeId) {
        try {
            deliverySimulationService.stopSimulation(routeId);
            return Result.success();
        } catch (Exception e) {
            log.error("停止配送失败", e);
            return Result.error("停止配送失败: " + e.getMessage());
        }
    }
}
