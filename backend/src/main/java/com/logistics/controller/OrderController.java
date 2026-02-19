package com.logistics.controller;

import com.logistics.common.Result;
import com.logistics.dto.CreateOrderRequest;
import com.logistics.entity.Order;
import com.logistics.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单控制器
 */
@RestController
@RequestMapping("/orders")
@Tag(name = "订单管理", description = "订单查询、创建、发货、收货等相关接口")
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    /**
     * 查询顾客的订单列表
     */
    @Operation(summary = "查询顾客订单列表", description = "根据客户ID查询该客户的所有订单，支持按状态和关键词筛选")
    @GetMapping("/my")
    public Result<List<Order>> getMyOrders(
            @Parameter(description = "客户ID") @RequestParam(required = false) Integer customerId,
            @Parameter(description = "订单状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String search) {
        try {
            if (customerId == null) {
                return Result.error("用户ID不能为空");
            }
            
            List<Order> orders = orderService.getCustomerOrders(customerId, status, search);
            return Result.success("查询成功", orders);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 查询商户的订单列表
     */
    @Operation(summary = "查询商户订单列表", description = "根据商户ID查询该商户的所有订单，支持按状态和关键词筛选")
    @GetMapping("/merchant")
    public Result<List<Order>> getMerchantOrders(
            @Parameter(description = "商户ID") @RequestParam(required = false) Integer merchantId,
            @Parameter(description = "订单状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String search) {
        try {
            if (merchantId == null) {
                return Result.error("商户ID不能为空");
            }
            
            List<Order> orders = orderService.getMerchantOrders(merchantId, status, search);
            return Result.success("查询成功", orders);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 查询配送员的待揽收订单列表
     * 状态=1（已发货）的订单
     * 只返回配送员所属仓库的订单
     */
    @Operation(summary = "查询待揽收订单", description = "获取配送员待揽收的订单列表（订单状态为已发货）")
    @GetMapping("/pending-pickup")
    public Result<List<Order>> getPendingPickupOrders(
            @Parameter(description = "配送员ID") @RequestParam(required = false) Long deliveryPersonnelId,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String search) {
        try {
            if (deliveryPersonnelId == null) {
                return Result.error("配送员ID不能为空");
            }
            List<Order> orders = orderService.getPendingPickupOrders(deliveryPersonnelId, search);
            return Result.success("查询成功", orders);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 确认收货
     */
    @Operation(summary = "确认收货", description = "客户确认收到订单，将订单状态更新为已完成")
    @PutMapping("/{orderId}/confirm")
    public Result<Void> confirmReceipt(
            @Parameter(description = "订单ID") @PathVariable Integer orderId,
            @Parameter(description = "客户ID") @RequestParam Integer customerId) {
        try {
            if (customerId == null) {
                return Result.error("用户ID不能为空");
            }
            
            orderService.confirmReceipt(orderId, customerId);
            return Result.success("确认收货成功", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 商户发货
     */
    @Operation(summary = "商户发货", description = "商户确认发货，将订单状态更新为已发货")
    @PutMapping("/{orderId}/ship")
    public Result<Void> shipOrder(
            @Parameter(description = "订单ID") @PathVariable Integer orderId,
            @Parameter(description = "商户ID") @RequestParam Integer merchantId) {
        try {
            if (merchantId == null) {
                return Result.error("商户ID不能为空");
            }
            
            orderService.shipOrder(orderId, merchantId);
            return Result.success("发货成功", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 配送员确认揽收
     * 将订单状态从1（已发货）更新为2（已揽收）
     */
    @Operation(summary = "确认揽收", description = "配送员确认揽收订单，将订单状态更新为已揽收")
    @PutMapping("/{orderId}/pickup")
    public Result<Void> confirmPickup(@Parameter(description = "订单ID") @PathVariable Integer orderId) {
        try {
            orderService.confirmPickup(orderId);
            return Result.success("揽收成功", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 查询配送员的待送货订单列表
     * 状态=2（已揽收）的订单
     * 只返回配送员所属仓库的订单
     */
    @Operation(summary = "查询待送货订单", description = "获取配送员待送货的订单列表（订单状态为已揽收）")
    @GetMapping("/pending-delivery")
    public Result<List<Order>> getPendingDeliveryOrders(
            @Parameter(description = "配送员ID") @RequestParam(required = false) Long deliveryPersonnelId) {
        try {
            if (deliveryPersonnelId == null) {
                return Result.error("配送员ID不能为空");
            }
            List<Order> orders = orderService.getPendingDeliveryOrders(deliveryPersonnelId);
            return Result.success("查询成功", orders);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
    * 创建送货批次（调用高德API规划路径）
    * 将选中的订单（最多5个）状态更新为运输中(3)，并保存批次和路径信息
     */
    @Operation(summary = "创建送货批次", description = "配送员创建送货批次，最多包含5个订单，调用高德API规划最优路线")
    @PostMapping("/delivery-batch")
    public Result<com.logistics.dto.CreateBatchResponse> createDeliveryBatch(
            @Parameter(description = "配送员ID") @RequestParam Long deliveryPersonnelId,
            @Parameter(description = "订单ID列表") @RequestBody List<Integer> orderIds) {
        try {
            if (deliveryPersonnelId == null) {
                return Result.error("配送员ID不能为空");
            }
            var resp = orderService.createDeliveryBatch(deliveryPersonnelId, orderIds);
            return Result.success("创建送货批次成功", resp);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 创建订单（下单）
     * TODO: 需要实现用户认证机制，目前临时使用请求参数传递用户ID
     */
    @Operation(summary = "创建订单", description = "客户下单购买商品，创建新的订单")
    @PostMapping("/create")
    public Result<Order> createOrder(@Parameter(description = "订单创建信息") @RequestBody CreateOrderRequest request) {
        try {
            // TODO: 从认证上下文获取用户ID，这里暂时从请求中获取
            // 临时方案：前端需要在请求体中包含customerId
            if (request.getCustomerId() == null) {
                return Result.error("用户ID不能为空");
            }
            
            // 校验请求参数
            if (request.getProductId() == null) {
                return Result.error("商品ID不能为空");
            }
            if (request.getQuantity() == null || request.getQuantity() <= 0) {
                return Result.error("购买数量必须大于0");
            }
            if (request.getPrice() == null) {
                return Result.error("商品价格不能为空");
            }
            
            // 创建订单
            Order order = orderService.createOrder(request, request.getCustomerId());
            
            return Result.success("下单成功", order);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取运输中的订单列表（用于显示送货批次）
     */
    @Operation(summary = "查询送货批次订单", description = "获取运输中的订单列表，用于显示正在进行的送货批次")
    @GetMapping("/delivery-batches")
    public Result<List<Order>> getDeliveryBatches(
            @Parameter(description = "仓库ID") @RequestParam(required = false) Integer warehouseId) {
        try {
            List<Order> orders = orderService.getDeliveryBatchOrders(warehouseId);
            return Result.success("查询成功", orders);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取运输批次列表（包含批次状态信息）
     */
    @Operation(summary = "查询运输批次列表", description = "获取配送员的运输批次列表，包含批次状态信息")
    @GetMapping("/delivery-batches-with-status")
    public Result<List<com.logistics.dto.DeliveryBatchResponse>> getDeliveryBatchesWithStatus(
            @Parameter(description = "配送员ID") @RequestParam(required = false) Long deliveryPersonnelId,
            @Parameter(description = "仓库ID") @RequestParam(required = false) Integer warehouseId) {
        try {
            if (deliveryPersonnelId == null) {
                return Result.error("配送员ID不能为空");
            }
            List<com.logistics.dto.DeliveryBatchResponse> batches = orderService.getDeliveryBatchesWithStatus(deliveryPersonnelId, warehouseId);
            return Result.success("查询成功", batches);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 完成送货（将订单状态更新为已到达）
     */
    @Operation(summary = "完成送货", description = "配送员确认完成送货，将订单状态更新为已到达")
    @PostMapping("/delivery-complete")
    public Result<Void> completeDelivery(@Parameter(description = "订单ID列表") @RequestBody List<Integer> orderIds) {
        try {
            orderService.completeDelivery(orderIds);
            return Result.success("完成送货成功", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取已完成的运输批次（历史任务）
     */
    @Operation(summary = "查询历史任务", description = "获取配送员已完成的运输批次列表，支持按时间范围筛选")
    @GetMapping("/completed-batches")
    public Result<List<Order>> getCompletedBatches(
            @Parameter(description = "配送员ID") @RequestParam(required = false) Long deliveryPersonnelId,
            @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime) {
        try {
            if (deliveryPersonnelId == null) {
                return Result.error("配送员ID不能为空");
            }

            // 解析时间参数
            java.time.LocalDateTime startDateTime = null;
            java.time.LocalDateTime endDateTime = null;
            if (startTime != null && !startTime.isEmpty()) {
                startDateTime = java.time.LocalDateTime.parse(startTime + "T00:00:00");
            }
            if (endTime != null && !endTime.isEmpty()) {
                endDateTime = java.time.LocalDateTime.parse(endTime + "T23:59:59");
            }

            List<Order> orders = orderService.getCompletedDeliveryBatches(deliveryPersonnelId, startDateTime, endDateTime);
            return Result.success("查询成功", orders);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取已完成的运输批次列表（包含批次状态信息）
     */
    @Operation(summary = "查询已完成批次列表", description = "获取配送员已完成的运输批次列表，包含批次状态信息")
    @GetMapping("/completed-batches-with-status")
    public Result<List<com.logistics.dto.DeliveryBatchResponse>> getCompletedBatchesWithStatus(
            @Parameter(description = "配送员ID") @RequestParam(required = false) Long deliveryPersonnelId,
            @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime) {
        try {
            if (deliveryPersonnelId == null) {
                return Result.error("配送员ID不能为空");
            }

            // 解析时间参数
            java.time.LocalDateTime startDateTime = null;
            java.time.LocalDateTime endDateTime = null;
            if (startTime != null && !startTime.isEmpty()) {
                startDateTime = java.time.LocalDateTime.parse(startTime + "T00:00:00");
            }
            if (endTime != null && !endTime.isEmpty()) {
                endDateTime = java.time.LocalDateTime.parse(endTime + "T23:59:59");
            }

            List<com.logistics.dto.DeliveryBatchResponse> batches = orderService.getCompletedBatchesWithStatus(deliveryPersonnelId, startDateTime, endDateTime);
            return Result.success("查询成功", batches);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}

