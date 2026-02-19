package com.logistics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.logistics.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 库存 Mapper
 */
@Mapper
public interface InventoryMapper extends BaseMapper<Inventory> {
    
    /**
     * 查询库存并关联仓库信息
     */
    @Select("SELECT i.*, w.name as warehouseName " +
            "FROM inventory i " +
            "LEFT JOIN warehouse w ON i.warehouse_id = w.id " +
            "WHERE i.user_id = #{userId} " +
            "ORDER BY i.stock_in_date DESC")
    List<Inventory> selectWithWarehouse(Integer userId);
    
    /**
     * 根据条件查询库存并关联仓库信息
     */
    @Select("<script>" +
            "SELECT i.*, w.name as warehouseName " +
            "FROM inventory i " +
            "LEFT JOIN warehouse w ON i.warehouse_id = w.id " +
            "WHERE i.user_id = #{userId} " +
            "<if test='productName != null and productName != \"\"'>" +
            "  AND i.product_name LIKE CONCAT('%', #{productName}, '%') " +
            "</if>" +
            "<if test='minStock != null'>" +
            "  AND i.quantity &gt;= #{minStock} " +
            "</if>" +
            "<if test='maxStock != null'>" +
            "  AND i.quantity &lt;= #{maxStock} " +
            "</if>" +
            "<if test='isPublished != null'>" +
            "  AND i.is_published = #{isPublished} " +
            "</if>" +
            "ORDER BY i.stock_in_date DESC" +
            "</script>")
    List<Inventory> selectWithWarehouseByConditions(Integer userId, String productName, Integer minStock, Integer maxStock, Integer isPublished);
}
