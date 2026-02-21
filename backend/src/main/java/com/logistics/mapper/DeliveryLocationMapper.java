package com.logistics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.logistics.entity.DeliveryLocation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DeliveryLocationMapper extends BaseMapper<DeliveryLocation> {

    @Select("SELECT * FROM delivery_location WHERE batch_id = #{batchId} ORDER BY recorded_at DESC LIMIT 1")
    DeliveryLocation selectLatestByBatchId(@Param("batchId") Integer batchId);
}
