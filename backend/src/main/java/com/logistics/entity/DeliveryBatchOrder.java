package com.logistics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 运输批次订单关联实体类
 */
@Data
@TableName("delivery_batch_orders")
public class DeliveryBatchOrder implements Serializable {
    
    /**
     * 批次ID（主键的一部分，无需 @TableId）
     */
    private Integer batchId;
    
    /**
     * 订单ID（主键的一部分，无需 @TableId）
     */
    private Integer orderId;
    
    /**
     * 高德返回的最优停靠顺序（1,2,3...）
     */
    private Integer stopSequence;
}
