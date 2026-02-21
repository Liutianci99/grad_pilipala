package com.logistics.service;

import com.logistics.dto.CreateOrderRequest;
import com.logistics.entity.Order;

import java.util.List;

/**
 * 订单服务接口
 */
public interface OrderService {
    
    Order createOrder(CreateOrderRequest request, Integer customerId);
    
    List<Order> getCustomerOrders(Integer customerId, Integer status, String search);
    
    List<Order> getMerchantOrders(Integer merchantId, Integer status, String search);
    
    void confirmReceipt(Integer orderId, Integer customerId);
    
    void shipOrder(Integer orderId, Integer merchantId);
    
    void confirmPickup(Integer orderId);
    
    /**
     * 获取配送员的待揽收订单列表（根据配送员所属仓库筛选）
     * @param driverId 配送员用户ID
     */
    List<Order> getPendingPickupOrders(Long driverId, String search);
    
    /**
     * 获取配送员的待送货订单列表（已揽收状态）
     * @param driverId 配送员用户ID
     */
    List<Order> getPendingDeliveryOrders(Long driverId);
    
    /**
     * 创建送货批次
     * @param driverId 配送员用户ID
     */
    com.logistics.dto.CreateBatchResponse createDeliveryBatch(Long driverId, List<Integer> orderIds);
    
    List<Order> getDeliveryBatchOrders(Integer warehouseId);

    List<Order> getDeliveryBatchOrders(java.time.LocalDateTime deliveryTime, Integer warehouseId);

    void completeDelivery(List<Integer> orderIds);

    void updateOrdersToInTransit(List<Order> orders);

    /**
     * 获取已完成的运输批次（历史任务）
     * @param driverId 配送员用户ID
     */
    List<Order> getCompletedDeliveryBatches(Long driverId, java.time.LocalDateTime startTime, java.time.LocalDateTime endTime);

    /**
     * 获取运输批次列表（包含批次状态信息）
     * @param driverId 配送员用户ID
     */
    List<com.logistics.dto.DeliveryBatchResponse> getDeliveryBatchesWithStatus(Long driverId, Integer warehouseId);

    /**
     * 获取已完成的运输批次列表
     * @param driverId 配送员用户ID
     */
    List<com.logistics.dto.DeliveryBatchResponse> getCompletedBatchesWithStatus(Long driverId, java.time.LocalDateTime startTime, java.time.LocalDateTime endTime);

    List<Order> getOrdersByBatchId(Integer batchId);
}
