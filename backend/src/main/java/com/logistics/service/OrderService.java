package com.logistics.service;

import com.logistics.dto.CreateOrderRequest;
import com.logistics.entity.Order;

import java.util.List;

/**
 * 订单服务接口
 */
public interface OrderService {
    
    /**
     * 创建订单
     * @param request 订单请求
     * @param customerId 顾客ID
     * @return 订单信息
     */
    Order createOrder(CreateOrderRequest request, Integer customerId);
    
    /**
     * 获取顾客的订单列表
     * @param customerId 顾客ID
     * @param status 订单状态（可选）
     * @param search 搜索关键词（可选）
     * @return 订单列表
     */
    List<Order> getCustomerOrders(Integer customerId, Integer status, String search);
    
    /**
     * 获取商户的订单列表
     * @param merchantId 商户ID
     * @param status 订单状态（可选）
     * @param search 搜索关键词（可选）
     * @return 订单列表
     */
    List<Order> getMerchantOrders(Integer merchantId, Integer status, String search);
    
    /**
     * 确认收货
     * @param orderId 订单ID
     * @param customerId 顾客ID
     */
    void confirmReceipt(Integer orderId, Integer customerId);
    
    /**
     * 商户发货
     * @param orderId 订单ID
     * @param merchantId 商户ID
     */
    void shipOrder(Integer orderId, Integer merchantId);
    
    /**
     * 配送员确认揽收
     * @param orderId 订单ID
     */
    void confirmPickup(Integer orderId);
    
    /**
     * 获取配送员的待揽收订单列表（根据配送员所属仓库筛选）
     * @param deliveryPersonnelId 配送员ID
     * @param search 搜索关键词（可选）
     * @return 订单列表
     */
    List<Order> getPendingPickupOrders(Long deliveryPersonnelId, String search);
    
    /**
     * 获取配送员的待送货订单列表（已揽收状态，根据配送员所属仓库筛选）
     * @param deliveryPersonnelId 配送员ID
     * @return 订单列表
     */
    List<Order> getPendingDeliveryOrders(Long deliveryPersonnelId);
    
    /**
    * 创建送货批次（调用高德API规划路径，将选中的订单状态更新为运输中）
    * @param deliveryPersonnelId 配送员ID
     * @param orderIds 订单ID列表
     */
    com.logistics.dto.CreateBatchResponse createDeliveryBatch(Long deliveryPersonnelId, List<Integer> orderIds);
    
    /**
     * 获取运输中的订单列表（用于显示送货批次）
     * @param warehouseId 仓库ID（可选）
     * @return 订单列表
     */
    List<Order> getDeliveryBatchOrders(Integer warehouseId);

    /**
     * 获取指定配送批次的订单列表
     * @param deliveryTime 配送时间
     * @param warehouseId 仓库ID
     * @return 订单列表
     */
    List<Order> getDeliveryBatchOrders(java.time.LocalDateTime deliveryTime, Integer warehouseId);

    /**
     * 完成送货（将订单状态更新为已到达）
     * @param orderIds 订单ID列表
     */
    void completeDelivery(List<Integer> orderIds);

    /**
     * 批量更新订单状态为运输中(3)
     * @param orders 订单列表
     */
    void updateOrdersToInTransit(List<Order> orders);

    /**
     * 获取已完成的运输批次（历史任务）
     * @param deliveryPersonnelId 配送员ID
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 订单列表
     */
    List<Order> getCompletedDeliveryBatches(Long deliveryPersonnelId, java.time.LocalDateTime startTime, java.time.LocalDateTime endTime);

    /**
     * 获取运输批次列表（包含批次状态信息）
     * @param deliveryPersonnelId 配送员ID
     * @param warehouseId 仓库ID（可选）
     * @return 批次列表
     */
    List<com.logistics.dto.DeliveryBatchResponse> getDeliveryBatchesWithStatus(Long deliveryPersonnelId, Integer warehouseId);

    /**
     * 获取已完成的运输批次列表
     * @param deliveryPersonnelId 配送员ID
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 批次列表
     */
    List<com.logistics.dto.DeliveryBatchResponse> getCompletedBatchesWithStatus(Long deliveryPersonnelId, java.time.LocalDateTime startTime, java.time.LocalDateTime endTime);

    /**
     * 根据批次ID获取订单列表（含地址信息）
     */
    List<Order> getOrdersByBatchId(Integer batchId);
}
