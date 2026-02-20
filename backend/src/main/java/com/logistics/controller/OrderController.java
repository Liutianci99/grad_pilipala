package com.logistics.controller;

import com.logistics.common.Result;
import com.logistics.dto.CreateOrderRequest;
import com.logistics.entity.Order;
import com.logistics.exception.BusinessException;
import com.logistics.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单控制器
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "订单管理", description = "订单查询、创建、发货、收货等相关接口")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "查询顾客订单列表")
    @GetMapping("/my")
    public Result<List<Order>> listMyOrders(
            @Parameter(description = "客户ID") @RequestParam Integer customerId,
            @Parameter(description = "订单状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String search) {
        List<Order> orders = orderService.getCustomerOrders(customerId, status, search);
        return Result.success(orders);
    }

    @Operation(summary = "查询商户订单列表")
    @GetMapping("/merchant")
    public Result<List<Order>> listMerchantOrders(
            @Parameter(description = "商户ID") @RequestParam Integer merchantId,
            @Parameter(description = "订单状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String search) {
        List<Order> orders = orderService.getMerchantOrders(merchantId, status, search);
        return Result.success(orders);
    }

    @Operation(summary = "查询待揽收订单")
    @GetMapping("/pending-pickup")
    public Result<List<Order>> listPendingPickupOrders(
            @Parameter(description = "配送员ID") @RequestParam Long deliveryPersonnelId,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String search) {
        List<Order> orders = orderService.getPendingPickupOrders(deliveryPersonnelId, search);
        return Result.success(orders);
    }

    @Operation(summary = "确认收货")
    @PutMapping("/{orderId}/confirm")
    public Result<Void> confirmReceipt(
            @Parameter(description = "订单ID") @PathVariable Integer orderId,
            @Parameter(description = "客户ID") @RequestParam Integer customerId) {
        orderService.confirmReceipt(orderId, customerId);
        return Result.success("确认收货成功", null);
    }

    @Operation(summary = "商户发货")
    @PutMapping("/{orderId}/ship")
    public Result<Void> shipOrder(
            @Parameter(description = "订单ID") @PathVariable Integer orderId,
            @Parameter(description = "商户ID") @RequestParam Integer merchantId) {
        orderService.shipOrder(orderId, merchantId);
        return Result.success("发货成功", null);
    }

    @Operation(summary = "确认揽收")
    @PutMapping("/{orderId}/pickup")
    public Result<Void> confirmPickup(@Parameter(description = "订单ID") @PathVariable Integer orderId) {
        orderService.confirmPickup(orderId);
        return Result.success("揽收成功", null);
    }

    @Operation(summary = "查询待送货订单")
    @GetMapping("/pending-delivery")
    public Result<List<Order>> listPendingDeliveryOrders(
            @Parameter(description = "配送员ID") @RequestParam Long deliveryPersonnelId) {
        List<Order> orders = orderService.getPendingDeliveryOrders(deliveryPersonnelId);
        return Result.success(orders);
    }

    @Operation(summary = "创建送货批次")
    @PostMapping("/delivery-batch")
    public Result<com.logistics.dto.CreateBatchResponse> createDeliveryBatch(
            @Parameter(description = "配送员ID") @RequestParam Long deliveryPersonnelId,
            @Parameter(description = "订单ID列表") @RequestBody List<Integer> orderIds) {
        var resp = orderService.createDeliveryBatch(deliveryPersonnelId, orderIds);
        return Result.success("创建送货批次成功", resp);
    }

    @Operation(summary = "创建订单")
    @PostMapping("/create")
    public Result<Order> createOrder(@Parameter(description = "订单创建信息") @RequestBody CreateOrderRequest request) {
        if (request.getCustomerId() == null) {
            throw new BusinessException("用户ID不能为空");
        }
        if (request.getProductId() == null) {
            throw new BusinessException("商品ID不能为空");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BusinessException("购买数量必须大于0");
        }
        if (request.getPrice() == null) {
            throw new BusinessException("商品价格不能为空");
        }
        Order order = orderService.createOrder(request, request.getCustomerId());
        return Result.success("下单成功", order);
    }

    @Operation(summary = "查询送货批次订单")
    @GetMapping("/delivery-batches")
    public Result<List<Order>> listDeliveryBatches(
            @Parameter(description = "仓库ID") @RequestParam(required = false) Integer warehouseId) {
        List<Order> orders = orderService.getDeliveryBatchOrders(warehouseId);
        return Result.success(orders);
    }

    @Operation(summary = "查询运输批次列表")
    @GetMapping("/delivery-batches-with-status")
    public Result<List<com.logistics.dto.DeliveryBatchResponse>> listDeliveryBatchesWithStatus(
            @Parameter(description = "配送员ID") @RequestParam Long deliveryPersonnelId,
            @Parameter(description = "仓库ID") @RequestParam(required = false) Integer warehouseId) {
        List<com.logistics.dto.DeliveryBatchResponse> batches = orderService.getDeliveryBatchesWithStatus(deliveryPersonnelId, warehouseId);
        return Result.success(batches);
    }

    @Operation(summary = "完成送货")
    @PostMapping("/delivery-complete")
    public Result<Void> completeDelivery(@Parameter(description = "订单ID列表") @RequestBody List<Integer> orderIds) {
        orderService.completeDelivery(orderIds);
        return Result.success("完成送货成功", null);
    }

    @Operation(summary = "查询历史任务")
    @GetMapping("/completed-batches")
    public Result<List<Order>> listCompletedBatches(
            @Parameter(description = "配送员ID") @RequestParam Long deliveryPersonnelId,
            @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime) {
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;
        if (startTime != null && !startTime.isEmpty()) {
            startDateTime = LocalDateTime.parse(startTime + "T00:00:00");
        }
        if (endTime != null && !endTime.isEmpty()) {
            endDateTime = LocalDateTime.parse(endTime + "T23:59:59");
        }
        List<Order> orders = orderService.getCompletedDeliveryBatches(deliveryPersonnelId, startDateTime, endDateTime);
        return Result.success(orders);
    }

    @Operation(summary = "查询已完成批次列表")
    @GetMapping("/completed-batches-with-status")
    public Result<List<com.logistics.dto.DeliveryBatchResponse>> listCompletedBatchesWithStatus(
            @Parameter(description = "配送员ID") @RequestParam Long deliveryPersonnelId,
            @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime) {
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;
        if (startTime != null && !startTime.isEmpty()) {
            startDateTime = LocalDateTime.parse(startTime + "T00:00:00");
        }
        if (endTime != null && !endTime.isEmpty()) {
            endDateTime = LocalDateTime.parse(endTime + "T23:59:59");
        }
        List<com.logistics.dto.DeliveryBatchResponse> batches = orderService.getCompletedBatchesWithStatus(deliveryPersonnelId, startDateTime, endDateTime);
        return Result.success(batches);
    }
}
