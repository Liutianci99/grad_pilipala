package com.logistics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.logistics.entity.DeliveryBatch;
import org.apache.ibatis.annotations.Mapper;

/**
 * 运输批次Mapper接口
 */
@Mapper
public interface DeliveryBatchMapper extends BaseMapper<DeliveryBatch> {
}
