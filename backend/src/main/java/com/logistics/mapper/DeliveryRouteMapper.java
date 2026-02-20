package com.logistics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.logistics.entity.DeliveryRoute;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 配送路线Mapper接口
 */
@Mapper
public interface DeliveryRouteMapper extends BaseMapper<DeliveryRoute> {

    /**
     * 根据配送时间和仓库ID查询路线
     */
    @Select("SELECT * FROM delivery_route WHERE delivery_time = #{deliveryTime} AND warehouse_id = #{warehouseId}")
    DeliveryRoute selectByDeliveryTimeAndWarehouse(@Param("deliveryTime") LocalDateTime deliveryTime,
                                                     @Param("warehouseId") Integer warehouseId);

    /**
     * 根据批次ID查询路线
     */
    @Select("SELECT * FROM delivery_route WHERE batch_id = #{batchId} ORDER BY id DESC LIMIT 1")
    DeliveryRoute selectByBatchId(@Param("batchId") Integer batchId);
}
