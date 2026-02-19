package com.logistics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.logistics.entity.DeliveryLocation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 配送位置记录Mapper接口
 */
@Mapper
public interface DeliveryLocationMapper extends BaseMapper<DeliveryLocation> {

    /**
     * 查询指定路线的最新位置记录
     *
     * @param routeId 路线ID
     * @return 最新的位置记录
     */
    @Select("SELECT * FROM delivery_location WHERE route_id = #{routeId} ORDER BY recorded_at DESC LIMIT 1")
    DeliveryLocation selectLatestByRouteId(@Param("routeId") Long routeId);
}
