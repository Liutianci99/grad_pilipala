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
     *
     * @param deliveryTime 配送时间
     * @param warehouseId  仓库ID
     * @return 配送路线
     */
    @Select("SELECT * FROM delivery_route WHERE delivery_time = #{deliveryTime} AND warehouse_id = #{warehouseId}")
    DeliveryRoute selectByDeliveryTimeAndWarehouse(@Param("deliveryTime") LocalDateTime deliveryTime,
                                                     @Param("warehouseId") Integer warehouseId);
}
