package com.logistics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.logistics.entity.DeliveryBatchOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 运输批次订单关联Mapper接口
 */
@Mapper
public interface DeliveryBatchOrderMapper extends BaseMapper<DeliveryBatchOrder> {
}
