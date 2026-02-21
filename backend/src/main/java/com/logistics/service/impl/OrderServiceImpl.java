package com.logistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.logistics.dto.CreateOrderRequest;
import com.logistics.entity.Address;
import com.logistics.entity.DeliveryBatch;
import com.logistics.entity.DeliveryBatchOrder;
import com.logistics.entity.Inventory;
import com.logistics.entity.Mall;
import com.logistics.entity.Order;
import com.logistics.entity.User;
import com.logistics.entity.Warehouse;
import com.logistics.mapper.AddressMapper;
import com.logistics.mapper.DeliveryBatchMapper;
import com.logistics.mapper.DeliveryBatchOrderMapper;
import com.logistics.mapper.InventoryMapper;
import com.logistics.mapper.MallMapper;
import com.logistics.mapper.OrderMapper;
import com.logistics.mapper.UserMapper;
import com.logistics.mapper.WarehouseMapper;
import com.logistics.service.OrderService;
import com.logistics.service.TencentMapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {
    
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private MallMapper mallMapper;
    @Autowired
    private InventoryMapper inventoryMapper;
    @Autowired
    private AddressMapper addressMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WarehouseMapper warehouseMapper;
    @Autowired
    private DeliveryBatchMapper deliveryBatchMapper;
    @Autowired
    private DeliveryBatchOrderMapper deliveryBatchOrderMapper;
    @Autowired
    private TencentMapService tencentMapService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(CreateOrderRequest request, Integer customerId) {
        Mall mall = mallMapper.selectById(request.getProductId());
        if (mall == null) {
            throw new RuntimeException("商品不存在");
        }
        if (mall.getIsPublished() == null || mall.getIsPublished() != 1) {
            throw new RuntimeException("商品未上架");
        }
        if (mall.getAvailableQuantity() < request.getQuantity()) {
            throw new RuntimeException("库存不足，当前可用库存：" + mall.getAvailableQuantity());
        }
        if (mall.getPrice().compareTo(request.getPrice()) != 0) {
            throw new RuntimeException("商品价格已变动，请刷新页面");
        }
        
        QueryWrapper<Inventory> inventoryQuery = new QueryWrapper<>();
        inventoryQuery.eq("product_id", request.getProductId());
        Inventory inventory = inventoryMapper.selectOne(inventoryQuery);
        if (inventory == null) {
            throw new RuntimeException("商品信息异常");
        }
        Integer merchantId = inventory.getUserId();
        
        Integer addressId = request.getAddressId();
        if (addressId == null) {
            QueryWrapper<Address> addressQuery = new QueryWrapper<>();
            addressQuery.eq("user_id", customerId);
            addressQuery.eq("is_default", 1);
            Address defaultAddress = addressMapper.selectOne(addressQuery);
            if (defaultAddress != null) {
                addressId = defaultAddress.getId().intValue();
            }
        }
        
        BigDecimal totalAmount = mall.getPrice().multiply(new BigDecimal(request.getQuantity()));
        
        Order order = new Order();
        order.setProductId(request.getProductId());
        order.setCustomerId(customerId);
        order.setMerchantId(merchantId);
        order.setAddressId(addressId);
        order.setProductName(mall.getProductName());
        order.setQuantity(request.getQuantity());
        order.setUnitPrice(mall.getPrice());
        order.setTotalAmount(totalAmount);
        order.setImageUrl(mall.getImageUrl());
        order.setStatus(0);
        order.setOrderTime(LocalDateTime.now());
        
        orderMapper.insert(order);
        return order;
    }
    
    @Override
    public List<Order> getCustomerOrders(Integer customerId, Integer status, String search) {
        QueryWrapper<Order> qw = new QueryWrapper<>();
        qw.eq("customer_id", customerId);
        if (status != null) qw.eq("status", status);
        if (search != null && !search.trim().isEmpty()) qw.like("product_name", search);
        qw.orderByDesc("order_time");
        return orderMapper.selectList(qw);
    }
    
    @Override
    public List<Order> getMerchantOrders(Integer merchantId, Integer status, String search) {
        QueryWrapper<Order> qw = new QueryWrapper<>();
        qw.eq("merchant_id", merchantId);
        if (status != null) qw.eq("status", status);
        if (search != null && !search.trim().isEmpty()) qw.like("product_name", search);
        qw.orderByDesc("order_time");
        return orderMapper.selectList(qw);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceipt(Integer orderId, Integer customerId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new RuntimeException("订单不存在");
        if (!order.getCustomerId().equals(customerId)) throw new RuntimeException("无权操作此订单");
        if (order.getStatus() != 4) throw new RuntimeException("订单状态不正确，无法确认收货");
        
        order.setStatus(5);
        order.setReceiveTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipOrder(Integer orderId, Integer merchantId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new RuntimeException("订单不存在");
        if (!order.getMerchantId().equals(merchantId)) throw new RuntimeException("无权操作此订单");
        if (order.getStatus() != 0) throw new RuntimeException("订单状态不正确，无法发货");
        
        order.setStatus(1);
        order.setShipTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }
    
    @Override
    public List<Order> getPendingPickupOrders(Long driverId, String search) {
        // 直接从 users 表获取 warehouse_id
        User driver = userMapper.selectById(driverId);
        if (driver == null || driver.getWarehouseId() == null) {
            throw new RuntimeException("配送员或仓库信息不存在");
        }
        return orderMapper.selectPendingPickupOrders(driver.getWarehouseId(), search);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmPickup(Integer orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new RuntimeException("订单不存在");
        if (order.getStatus() != 1) throw new RuntimeException("订单状态不正确，无法揽收");
        
        order.setStatus(2);
        order.setPickupTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }
    
    @Override
    public List<Order> getPendingDeliveryOrders(Long driverId) {
        User driver = userMapper.selectById(driverId);
        if (driver == null || driver.getWarehouseId() == null) {
            throw new RuntimeException("配送员或仓库信息不存在");
        }
        return orderMapper.selectPendingDeliveryOrders(driver.getWarehouseId());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public com.logistics.dto.CreateBatchResponse createDeliveryBatch(Long driverId, List<Integer> orderIds) {
        if (driverId == null) throw new RuntimeException("配送员ID不能为空");
        if (orderIds == null || orderIds.isEmpty()) throw new RuntimeException("订单列表不能为空");
        if (orderIds.size() > 5) throw new RuntimeException("每个批次最多只能选择5个订单");

        // 直接从 users 表获取配送员信息
        User driver = userMapper.selectById(driverId);
        if (driver == null || driver.getWarehouseId() == null) {
            throw new RuntimeException("配送员或仓库信息不存在");
        }
        Warehouse warehouse = warehouseMapper.selectById(driver.getWarehouseId());
        if (warehouse == null || warehouse.getLongitude() == null || warehouse.getLatitude() == null) {
            throw new RuntimeException("仓库坐标信息不完整");
        }

        List<Order> orders = new ArrayList<>();
        List<Address> addresses = new ArrayList<>();
        for (Integer orderId : orderIds) {
            Order order = orderMapper.selectById(orderId);
            if (order == null) throw new RuntimeException("订单不存在: " + orderId);
            if (order.getStatus() != 2) throw new RuntimeException("订单状态不正确，只能配送已揽收的订单: " + orderId);

            // 检查订单是否已在活跃批次中
            Long existingCount = deliveryBatchOrderMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<DeliveryBatchOrder>()
                    .eq("order_id", orderId)
                    .inSql("batch_id", "SELECT id FROM delivery_batches WHERE status IN (0, 1)")
            );
            if (existingCount > 0) throw new RuntimeException("订单已在活跃批次中: " + orderId);

            Address addr = addressMapper.selectOne(
                new LambdaQueryWrapper<Address>()
                    .eq(Address::getUserId, order.getCustomerId().longValue())
                    .eq(Address::getIsDefault, 1)
            );
            if (addr == null) throw new RuntimeException("顾客未设置默认收货地址: " + orderId);
            if (addr.getLongitude() == null || addr.getLatitude() == null) {
                throw new RuntimeException("默认收货地址缺少坐标信息: " + orderId);
            }

            orders.add(order);
            addresses.add(addr);
        }

        // 调用腾讯地图API
        String origin = warehouse.getLatitude().toPlainString() + "," + warehouse.getLongitude().toPlainString();
        String destination;
        String waypoints = null;
        if (addresses.size() == 1) {
            Address dest = addresses.get(0);
            destination = dest.getLatitude().toPlainString() + "," + dest.getLongitude().toPlainString();
        } else {
            Address dest = addresses.get(addresses.size() - 1);
            destination = dest.getLatitude().toPlainString() + "," + dest.getLongitude().toPlainString();
            waypoints = addresses.subList(0, addresses.size() - 1).stream()
                .map(a -> a.getLatitude().toPlainString() + "," + a.getLongitude().toPlainString())
                .collect(Collectors.joining(";"));
        }

        JSONObject routeResponse = tencentMapService.planRoute(origin, destination, waypoints);
        Integer totalDistance = null;
        Integer totalDuration = null;

        if (routeResponse != null) {
            JSONObject result = routeResponse.getJSONObject("result");
            if (result != null) {
                JSONArray routes = result.getJSONArray("routes");
                if (routes != null && !routes.isEmpty()) {
                    JSONObject route = routes.getJSONObject(0);
                    totalDistance = route.getInteger("distance");
                    totalDuration = route.getInteger("duration");
                }
            }
        }

        // 保存批次 — driver_id 直接用 users.id
        DeliveryBatch batch = new DeliveryBatch();
        batch.setDriverId(driverId.intValue());
        batch.setWarehouseId(warehouse.getId());
        batch.setStatus(0);
        batch.setCreatedAt(LocalDateTime.now());
        batch.setTotalDistance(totalDistance);
        batch.setTotalDuration(totalDuration);
        batch.setCurrentIndex(0);
        deliveryBatchMapper.insert(batch);

        // 保存批次-订单关联
        for (int seq = 0; seq < orders.size(); seq++) {
            DeliveryBatchOrder bo = new DeliveryBatchOrder();
            bo.setBatchId(batch.getId());
            bo.setOrderId(orders.get(seq).getOrderId());
            bo.setStopSequence(seq + 1);
            deliveryBatchOrderMapper.insert(bo);
        }

        // 构建响应
        com.logistics.dto.CreateBatchResponse resp = new com.logistics.dto.CreateBatchResponse();
        resp.setBatchId(batch.getId());
        resp.setTotalDistance(totalDistance);
        resp.setTotalDuration(totalDuration);
        resp.setOrderCount(orders.size());
        List<Integer> stopOrderByOrderId = orders.stream()
            .map(Order::getOrderId).collect(Collectors.toList());
        resp.setStopOrder(stopOrderByOrderId);
        return resp;
    }
    
    @Override
    public List<Order> getDeliveryBatchOrders(Integer warehouseId) {
        List<Order> orders = orderMapper.selectDeliveryBatchOrders(warehouseId);
        for (Order order : orders) {
            if (order.getAddressId() != null) {
                order.setAddress(addressMapper.selectById(order.getAddressId()));
            }
        }
        return orders;
    }

    @Override
    public List<Order> getDeliveryBatchOrders(LocalDateTime deliveryTime, Integer warehouseId) {
        List<Order> orders = orderMapper.selectDeliveryBatchOrdersByTimeAndWarehouse(deliveryTime, warehouseId);
        for (Order order : orders) {
            if (order.getAddressId() != null) {
                order.setAddress(addressMapper.selectById(order.getAddressId()));
            }
        }
        return orders;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrdersToInTransit(List<Order> orders) {
        for (Order order : orders) {
            if (order.getStatus() == 2) {
                order.setStatus(3);
                order.setDeliveryTime(LocalDateTime.now());
                orderMapper.updateById(order);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeDelivery(List<Integer> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) throw new RuntimeException("订单列表不能为空");
        for (Integer orderId : orderIds) {
            Order order = orderMapper.selectById(orderId);
            if (order == null) throw new RuntimeException("订单不存在: " + orderId);
            if (order.getStatus() != 3) throw new RuntimeException("订单状态不正确，无法完成送货: " + orderId);
            order.setStatus(4);
            orderMapper.updateById(order);
        }
    }

    @Override
    public List<Order> getCompletedDeliveryBatches(Long driverId, LocalDateTime startTime, LocalDateTime endTime) {
        User driver = userMapper.selectById(driverId);
        if (driver == null || driver.getWarehouseId() == null) {
            throw new RuntimeException("配送员或仓库信息不存在");
        }

        QueryWrapper<Order> qw = new QueryWrapper<>();
        qw.eq("warehouse_id", driver.getWarehouseId());
        qw.eq("status", 4);
        if (startTime != null) qw.ge("delivery_time", startTime);
        if (endTime != null) qw.le("delivery_time", endTime);
        qw.orderByDesc("delivery_time");

        List<Order> orders = orderMapper.selectList(qw);
        for (Order order : orders) {
            if (order.getAddressId() != null) {
                order.setAddress(addressMapper.selectById(order.getAddressId()));
            }
        }
        return orders;
    }

    @Override
    public List<com.logistics.dto.DeliveryBatchResponse> getDeliveryBatchesWithStatus(Long driverId, Integer warehouseId) {
        // driver_id 现在直接是 users.id
        QueryWrapper<DeliveryBatch> batchQuery = new QueryWrapper<>();
        batchQuery.eq("driver_id", driverId);
        batchQuery.in("status", 0, 1);
        batchQuery.orderByDesc("created_at");

        List<DeliveryBatch> batches = deliveryBatchMapper.selectList(batchQuery);
        return buildBatchResponses(batches);
    }

    @Override
    public List<com.logistics.dto.DeliveryBatchResponse> getCompletedBatchesWithStatus(Long driverId, LocalDateTime startTime, LocalDateTime endTime) {
        QueryWrapper<DeliveryBatch> batchQuery = new QueryWrapper<>();
        batchQuery.eq("driver_id", driverId);
        batchQuery.eq("status", 2);
        if (startTime != null) batchQuery.ge("completed_at", startTime);
        if (endTime != null) batchQuery.le("completed_at", endTime);
        batchQuery.orderByDesc("completed_at");

        List<DeliveryBatch> batches = deliveryBatchMapper.selectList(batchQuery);
        return buildBatchResponses(batches);
    }

    @Override
    public List<Order> getOrdersByBatchId(Integer batchId) {
        QueryWrapper<DeliveryBatchOrder> qw = new QueryWrapper<>();
        qw.eq("batch_id", batchId).orderByAsc("stop_sequence");
        List<DeliveryBatchOrder> batchOrders = deliveryBatchOrderMapper.selectList(qw);

        List<Order> orders = new ArrayList<>();
        for (DeliveryBatchOrder bo : batchOrders) {
            Order order = orderMapper.selectOrderWithDetails(bo.getOrderId());
            if (order != null) {
                if (order.getAddressId() != null) {
                    order.setAddress(addressMapper.selectById(order.getAddressId()));
                }
                orders.add(order);
            }
        }
        return orders;
    }

    // ── Private helper ──

    private List<com.logistics.dto.DeliveryBatchResponse> buildBatchResponses(List<DeliveryBatch> batches) {
        List<com.logistics.dto.DeliveryBatchResponse> responseList = new ArrayList<>();
        for (DeliveryBatch batch : batches) {
            com.logistics.dto.DeliveryBatchResponse response = new com.logistics.dto.DeliveryBatchResponse();
            response.setBatchId(batch.getId());
            response.setStatus(batch.getStatus());
            response.setCreatedAt(batch.getCreatedAt());
            response.setStartedAt(batch.getStartedAt());
            response.setCompletedAt(batch.getCompletedAt());
            response.setTotalDistance(batch.getTotalDistance());
            response.setTotalDuration(batch.getTotalDuration());

            QueryWrapper<DeliveryBatchOrder> orderQuery = new QueryWrapper<>();
            orderQuery.eq("batch_id", batch.getId()).orderByAsc("stop_sequence");
            List<DeliveryBatchOrder> batchOrders = deliveryBatchOrderMapper.selectList(orderQuery);

            List<Order> orders = new ArrayList<>();
            for (DeliveryBatchOrder bo : batchOrders) {
                Order order = orderMapper.selectOrderWithDetails(bo.getOrderId());
                if (order != null) {
                    if (order.getAddressId() != null) {
                        order.setAddress(addressMapper.selectById(order.getAddressId()));
                    }
                    orders.add(order);
                }
            }
            response.setOrders(orders);
            responseList.add(response);
        }
        return responseList;
    }
}
