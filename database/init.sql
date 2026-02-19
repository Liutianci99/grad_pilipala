-- 创建数据库
CREATE DATABASE IF NOT EXISTS `ecommerce_logistics` DEFAULT CHARSET utf8mb4;

-- 创建user表
CREATE TABLE IF NOT EXISTS `users` (
  `id` INT PRIMARY KEY AUTO_INCREMENT,
  `username` VARCHAR(20) NOT NULL,
  `password` VARCHAR(100) NOT NULL,
  `role` ENUM('merchant','driver','consumer','admin') NOT NULL

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入初始数据（不写 id，避免主键冲突）
-- 密码: 123 (BCrypt加密)
INSERT INTO `users` (`username`,`password`,`role`) VALUES
('刘天赐','$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2','merchant'),
('刘天赐','$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2','driver'),
('刘天赐','$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2','consumer'),
('刘天赐','$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2','admin');

-- 创建库存表
CREATE TABLE IF NOT EXISTS `inventory` (
  `product_id` INT PRIMARY KEY AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `warehouse_id` INT NOT NULL DEFAULT 1 COMMENT '所属仓库ID',
  `product_name` VARCHAR(100) NOT NULL,
  `quantity` INT NOT NULL DEFAULT 0,
  `stock_in_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `image_url` VARCHAR(255),
  `is_published` TINYINT(1) NOT NULL DEFAULT 0,
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建商城表
CREATE TABLE IF NOT EXISTS `mall` (
  `product_id` INT PRIMARY KEY,
  `warehouse_id` INT NOT NULL DEFAULT 1 COMMENT '所属仓库ID',
  `product_name` VARCHAR(100) NOT NULL,
  `description` TEXT,
  `available_quantity` INT NOT NULL DEFAULT 0,
  `price` DECIMAL(10, 2) NOT NULL,
  `is_published` TINYINT(1) NOT NULL DEFAULT 1,
  `publish_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `image_url` VARCHAR(255),
  FOREIGN KEY (`product_id`) REFERENCES `inventory`(`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建地址表（必须在orders之前创建）
CREATE TABLE IF NOT EXISTS `address` (
  `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '地址ID',
  `user_id` INT NOT NULL COMMENT '用户ID（顾客）',
  `receiver_name` VARCHAR(50) NOT NULL COMMENT '收货人姓名',
  `receiver_phone` VARCHAR(20) NOT NULL COMMENT '收货人电话',
  `province` VARCHAR(50) NOT NULL COMMENT '省份',
  `city` VARCHAR(50) NOT NULL COMMENT '城市',
  `district` VARCHAR(50) NOT NULL COMMENT '区/县',
  `detail_address` VARCHAR(255) NOT NULL COMMENT '详细地址',
  `latitude` DECIMAL(10, 7) COMMENT '纬度',
  `longitude` DECIMAL(10, 7) COMMENT '经度',
  `is_default` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否默认地址：0-否，1-是',
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址表';

-- 创建订单表
CREATE TABLE `orders` (
  `order_id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
  `warehouse_id` INT NOT NULL DEFAULT 1 COMMENT '发货仓库ID',
  `product_id` INT NOT NULL COMMENT '商品ID',
  `customer_id` INT NOT NULL COMMENT '顾客ID',
  `merchant_id` INT NOT NULL COMMENT '商户ID',
  `address_id` INT COMMENT '收货地址ID',
  `product_name` VARCHAR(100) NOT NULL COMMENT '商品名称',
  `quantity` INT NOT NULL COMMENT '商品数量',
  `unit_price` DECIMAL(10, 2) NOT NULL COMMENT '商品单价',
  `total_amount` DECIMAL(10, 2) NOT NULL COMMENT '总金额',
  `image_url` VARCHAR(255) COMMENT '商品图片URL',
  `status` INT NOT NULL DEFAULT 0 COMMENT '订单状态：-1=取消订单，0=未发货，1=已发货，2=已揽收，3=运输中，4=已到达，5=已收货，6=待评价，7=已评价',
  `order_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
  `ship_time` DATETIME COMMENT '发货时间',
  `pickup_time` DATETIME COMMENT '揽收时间',
  `delivery_time` DATETIME COMMENT '送达时间',
  `receive_time` DATETIME COMMENT '收货时间',
  `review_time` DATETIME COMMENT '评价时间',
  FOREIGN KEY (`product_id`) REFERENCES `mall`(`product_id`),
  FOREIGN KEY (`customer_id`) REFERENCES `users`(`id`),
  FOREIGN KEY (`merchant_id`) REFERENCES `users`(`id`),
  FOREIGN KEY (`address_id`) REFERENCES `address`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 创建仓库表
CREATE TABLE IF NOT EXISTS `warehouse` (
  `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '仓库ID',
  `name` VARCHAR(100) NOT NULL COMMENT '仓库名称',
  `city` VARCHAR(50) NOT NULL COMMENT '所在城市',
  `address` VARCHAR(255) NOT NULL COMMENT '详细地址',
  `longitude` DECIMAL(10, 6) NOT NULL COMMENT '经度',
  `latitude` DECIMAL(10, 6) NOT NULL COMMENT '纬度'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库表';

-- 插入仓库数据
INSERT INTO `warehouse` (`name`, `city`, `address`, `longitude`, `latitude`) VALUES
('华北一号仓', '北京', '北京市大兴区亦庄开发区', 116.527834, 39.793421),
('华东中心仓', '上海', '上海市嘉定区江桥镇物流园', 121.329876, 31.302547),
('华南智能仓', '广州', '广州市白云区太和镇物流基地', 113.298765, 23.256432),
('西南枢纽仓', '成都', '成都市双流区航空港物流园', 104.023456, 30.567891),
('华中分拨仓', '武汉', '武汉市东西湖区保税物流园', 114.134567, 30.678912),
('华东电商仓', '杭州', '杭州市余杭区仁和街道菜鸟仓', 120.187654, 30.432109),
('华南冷链仓', '深圳', '深圳市龙岗区平湖物流基地', 114.123456, 22.678901),
('西北区域仓', '西安', '西安市未央区草滩物流园', 108.923456, 34.345678),
('东北转运仓', '沈阳', '沈阳市于洪区沙岭物流园', 123.345678, 41.789012),
('华东前置仓', '苏州', '苏州市相城区澄阳路电商园', 120.654321, 31.398765);

-- 创建配送员表
CREATE TABLE IF NOT EXISTS `delivery_personnel` (
  `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '配送员ID',
  `user_id` INT NOT NULL COMMENT '用户ID',
  `warehouse_id` INT NOT NULL COMMENT '所属仓库ID',
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`),
  FOREIGN KEY (`warehouse_id`) REFERENCES `warehouse`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配送员表';

-- 插入配送员数据（将现有的driver角色用户关联到仓库）
INSERT INTO `delivery_personnel` (`user_id`, `warehouse_id`) 
SELECT u.id, 1 FROM `users` u WHERE u.role = 'driver';

-- 创建运输批次主表
CREATE TABLE IF NOT EXISTS `delivery_batches` (
  `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '运输批次ID',
  `driver_id` INT NOT NULL COMMENT '配送员ID',
  `warehouse_id` INT NOT NULL COMMENT '起始仓库ID',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0=待出发, 1=配送中, 2=已完成',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `started_at` DATETIME COMMENT '实际出发时间',
  `completed_at` DATETIME COMMENT '完成时间',
  `route_polyline` TEXT COMMENT '高德返回的编码路线字符串（用于前端绘图）',
  `total_duration` INT COMMENT '预计总耗时（秒）',
  `total_distance` INT COMMENT '总距离（米）',
  FOREIGN KEY (`driver_id`) REFERENCES `delivery_personnel`(`id`) ON DELETE RESTRICT,
  FOREIGN KEY (`warehouse_id`) REFERENCES `warehouse`(`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运输批次主表';

-- 创建运输批次订单关联表
CREATE TABLE IF NOT EXISTS `delivery_batch_orders` (
  `batch_id` INT NOT NULL,
  `order_id` INT NOT NULL,
  `stop_sequence` TINYINT NOT NULL COMMENT '高德返回的最优停靠顺序（1,2,3...）',
  PRIMARY KEY (`batch_id`, `order_id`),
  UNIQUE KEY `uk_batch_stop_seq` (`batch_id`, `stop_sequence`),
  FOREIGN KEY (`batch_id`) REFERENCES `delivery_batches`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`order_id`) REFERENCES `orders`(`order_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运输批次包含的订单及停靠顺序';

-- ============================================================
-- 配送员实时追踪系统表（2026-02-03新增）
-- ============================================================

-- 配送路线表（存储路线规划结果和实时状态）
CREATE TABLE IF NOT EXISTS `delivery_route` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  `delivery_time` DATETIME NOT NULL COMMENT '配送批次时间',
  `warehouse_id` INT NOT NULL COMMENT '仓库ID',
  `route_data` TEXT NOT NULL COMMENT '路线数据(polyline压缩坐标JSON数组)',
  `total_distance` DECIMAL(10, 2) COMMENT '总距离(公里)',
  `total_duration` INT COMMENT '总时长(分钟)',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING=待开始, DELIVERING=运输中, COMPLETED=已完成, STOPPED=已停止',
  `current_index` INT DEFAULT 0 COMMENT '当前路径点索引',
  `started_at` DATETIME COMMENT '开始时间',
  `completed_at` DATETIME COMMENT '完成时间',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_delivery_time` (`delivery_time`),
  INDEX `idx_warehouse_id` (`warehouse_id`),
  INDEX `idx_status` (`status`),
  UNIQUE KEY `uk_delivery_warehouse` (`delivery_time`, `warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配送路线表-存储路线规划和实时状态';

-- 配送位置记录表（记录配送员历史轨迹）
CREATE TABLE IF NOT EXISTS `delivery_location` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  `route_id` BIGINT NOT NULL COMMENT '路线ID',
  `latitude` DECIMAL(10, 7) NOT NULL COMMENT '纬度',
  `longitude` DECIMAL(10, 7) NOT NULL COMMENT '经度',
  `address` VARCHAR(500) COMMENT '地址（通过逆地理编码获取）',
  `path_index` INT NOT NULL COMMENT '路径点索引',
  `recorded_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  INDEX `idx_route_id` (`route_id`),
  INDEX `idx_recorded_at` (`recorded_at`),
  FOREIGN KEY (`route_id`) REFERENCES `delivery_route`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配送位置记录表-记录配送员历史轨迹';
