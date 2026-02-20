package com.logistics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.logistics.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 订单Mapper接口
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    
    /**
     * 查询待揽收订单列表（状态=1），支持仓库和商品名称筛选
     */
    @Select("<script>" +
            "SELECT o.*, w.name AS warehouseName " +
            "FROM orders o " +
            "LEFT JOIN warehouse w ON o.warehouse_id = w.id " +
            "WHERE o.status = 1 " +
            "<if test='warehouseId != null'>" +
            "  AND o.warehouse_id = #{warehouseId} " +
            "</if>" +
            "<if test='search != null and search != \"\"'>" +
            "  AND o.product_name LIKE CONCAT('%', #{search}, '%') " +
            "</if>" +
            "ORDER BY o.ship_time DESC" +
            "</script>")
    List<Order> selectPendingPickupOrders(@Param("warehouseId") Integer warehouseId, 
                                          @Param("search") String search);
    
    /**
     * 查询待送货订单列表（状态=2），支持仓库筛选
     */
    @Select("<script>" +
            "SELECT o.*, w.name AS warehouseName " +
            "FROM orders o " +
            "LEFT JOIN warehouse w ON o.warehouse_id = w.id " +
            "WHERE o.status = 2 " +
            "<if test='warehouseId != null'>" +
            "  AND o.warehouse_id = #{warehouseId} " +
            "</if>" +
            "ORDER BY o.pickup_time DESC" +
            "</script>")
    List<Order> selectPendingDeliveryOrders(@Param("warehouseId") Integer warehouseId);
    
    /**
     * 查询运输中的订单列表（状态=3），用于显示送货批次，支持仓库筛选
     */
    @Select("<script>" +
            "SELECT o.*, w.name AS warehouseName, u.username AS customerName " +
            "FROM orders o " +
            "LEFT JOIN warehouse w ON o.warehouse_id = w.id " +
            "LEFT JOIN users u ON o.customer_id = u.id " +
            "WHERE o.status = 3 " +
            "<if test='warehouseId != null'>" +
            "  AND o.warehouse_id = #{warehouseId} " +
            "</if>" +
            "ORDER BY o.delivery_time DESC, o.order_id DESC" +
            "</script>")
    List<Order> selectDeliveryBatchOrders(@Param("warehouseId") Integer warehouseId);

    /**
     * 查询指定配送批次的订单列表（状态=3），按配送时间和仓库筛选
     */
    @Select("<script>" +
            "SELECT o.*, w.name AS warehouseName, u.username AS customerName " +
            "FROM orders o " +
            "LEFT JOIN warehouse w ON o.warehouse_id = w.id " +
            "LEFT JOIN users u ON o.customer_id = u.id " +
            "WHERE o.status = 3 " +
            "AND o.delivery_time = #{deliveryTime} " +
            "AND o.warehouse_id = #{warehouseId} " +
            "ORDER BY o.order_id DESC" +
            "</script>")
    List<Order> selectDeliveryBatchOrdersByTimeAndWarehouse(
            @Param("deliveryTime") java.time.LocalDateTime deliveryTime,
            @Param("warehouseId") Integer warehouseId);

    /**
     * 根据订单ID查询订单详情（包含顾客名称和仓库名称）
     */
    @Select("SELECT o.*, w.name AS warehouseName, u.username AS customerName " +
            "FROM orders o " +
            "LEFT JOIN warehouse w ON o.warehouse_id = w.id " +
            "LEFT JOIN users u ON o.customer_id = u.id " +
            "WHERE o.order_id = #{orderId}")
    Order selectOrderWithDetails(@Param("orderId") Integer orderId);

    /**
     * 管理员查询所有订单（含顾客名、商户名、仓库名）
     */
    @Select("<script>" +
            "SELECT o.*, u.username AS customerName, w.name AS warehouseName " +
            "FROM orders o " +
            "LEFT JOIN users u ON o.customer_id = u.id " +
            "LEFT JOIN warehouse w ON o.warehouse_id = w.id " +
            "WHERE 1=1 " +
            "<if test='status != null'>" +
            "  AND o.status = #{status} " +
            "</if>" +
            "<if test='search != null and search != \"\"'>" +
            "  AND (o.product_name LIKE CONCAT('%', #{search}, '%') " +
            "       OR u.username LIKE CONCAT('%', #{search}, '%') " +
            "       OR CAST(o.order_id AS CHAR) LIKE CONCAT('%', #{search}, '%')) " +
            "</if>" +
            "ORDER BY o.order_time DESC" +
            "</script>")
    List<Order> selectAllOrdersForAdmin(@Param("status") Integer status,
                                        @Param("search") String search);
}
