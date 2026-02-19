package com.logistics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.logistics.entity.DeliveryPersonnel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 配送员Mapper接口
 */
@Mapper
public interface DeliveryPersonnelMapper extends BaseMapper<DeliveryPersonnel> {
    
    /**
     * 根据用户ID查询配送员信息（包含仓库名称）
     */
    @Select("SELECT dp.*, w.name AS warehouseName " +
            "FROM delivery_personnel dp " +
            "LEFT JOIN warehouse w ON dp.warehouse_id = w.id " +
            "WHERE dp.user_id = #{userId}")
    DeliveryPersonnel selectByUserIdWithWarehouse(@Param("userId") Integer userId);
}
