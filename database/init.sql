SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `ecommerce_logistics` DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `ecommerce_logistics`;

-- ============================================================
-- 用户表（warehouse_id 仅 driver 有值）
-- ============================================================
CREATE TABLE IF NOT EXISTS `users` (
  `id` INT PRIMARY KEY AUTO_INCREMENT,
  `username` VARCHAR(20) NOT NULL,
  `password` VARCHAR(100) NOT NULL,
  `role` ENUM('merchant','driver','consumer','admin') NOT NULL,
  `warehouse_id` INT NULL COMMENT '配送员所属仓库（仅driver有值）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 仓库表
-- ============================================================
CREATE TABLE IF NOT EXISTS `warehouse` (
  `id` INT PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL,
  `city` VARCHAR(50) NOT NULL,
  `address` VARCHAR(255),
  `longitude` DECIMAL(10,6),
  `latitude` DECIMAL(10,6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 收货地址表
-- ============================================================
CREATE TABLE IF NOT EXISTS `address` (
  `id` INT PRIMARY KEY AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `receiver_name` VARCHAR(50) NOT NULL,
  `receiver_phone` VARCHAR(20) NOT NULL,
  `province` VARCHAR(50),
  `city` VARCHAR(50),
  `district` VARCHAR(50),
  `detail_address` VARCHAR(255),
  `latitude` DECIMAL(10,7),
  `longitude` DECIMAL(10,7),
  `is_default` TINYINT(1) DEFAULT 0,
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 库存表
-- ============================================================
CREATE TABLE IF NOT EXISTS `inventory` (
  `product_id` INT PRIMARY KEY AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `product_name` VARCHAR(100) NOT NULL,
  `description` TEXT,
  `quantity` INT NOT NULL DEFAULT 0,
  `image_url` VARCHAR(255),
  `is_published` TINYINT(1) NOT NULL DEFAULT 0,
  `warehouse_id` INT NOT NULL DEFAULT 1,
  `stock_in_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 商城表
-- ============================================================
CREATE TABLE IF NOT EXISTS `mall` (
  `product_id` INT PRIMARY KEY,
  `merchant_id` INT NOT NULL,
  `warehouse_id` INT NOT NULL DEFAULT 1,
  `product_name` VARCHAR(100) NOT NULL,
  `description` TEXT,
  `available_quantity` INT NOT NULL DEFAULT 0,
  `price` DECIMAL(10,2) NOT NULL,
  `is_published` TINYINT(1) NOT NULL DEFAULT 1,
  `publish_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `image_url` VARCHAR(255),
  FOREIGN KEY (`product_id`) REFERENCES `inventory`(`product_id`),
  FOREIGN KEY (`merchant_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 订单表
-- ============================================================
CREATE TABLE IF NOT EXISTS `orders` (
  `order_id` INT PRIMARY KEY AUTO_INCREMENT,
  `product_id` INT NOT NULL,
  `customer_id` INT NOT NULL,
  `merchant_id` INT NOT NULL,
  `address_id` INT,
  `product_name` VARCHAR(100),
  `quantity` INT NOT NULL DEFAULT 1,
  `unit_price` DECIMAL(10,2),
  `total_amount` DECIMAL(10,2),
  `image_url` VARCHAR(255),
  `status` INT NOT NULL DEFAULT 0 COMMENT '0未发货 1已发货 2已揽收 3运输中 4已到达 5已收货',
  `warehouse_id` INT,
  `order_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `ship_time` DATETIME,
  `pickup_time` DATETIME,
  `delivery_time` DATETIME,
  `receive_time` DATETIME,
  `review_time` DATETIME,
  FOREIGN KEY (`customer_id`) REFERENCES `users`(`id`),
  FOREIGN KEY (`merchant_id`) REFERENCES `users`(`id`),
  FOREIGN KEY (`product_id`) REFERENCES `inventory`(`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 运输批次表（含路线数据）
-- ============================================================
CREATE TABLE IF NOT EXISTS `delivery_batches` (
  `id` INT PRIMARY KEY AUTO_INCREMENT,
  `driver_id` INT NOT NULL,
  `warehouse_id` INT NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待出发 1配送中 2已完成',
  `route_data` LONGTEXT COMMENT '腾讯地图压缩polyline（JSON数组）',
  `total_distance` INT COMMENT '总距离（米）',
  `total_duration` INT COMMENT '总时长（秒）',
  `current_index` INT DEFAULT 0 COMMENT '模拟当前位置索引',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `started_at` DATETIME,
  `completed_at` DATETIME,
  FOREIGN KEY (`driver_id`) REFERENCES `users`(`id`),
  FOREIGN KEY (`warehouse_id`) REFERENCES `warehouse`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 批次订单关联表
-- ============================================================
CREATE TABLE IF NOT EXISTS `delivery_batch_orders` (
  `batch_id` INT NOT NULL,
  `order_id` INT NOT NULL,
  `stop_sequence` TINYINT DEFAULT 0,
  PRIMARY KEY (`batch_id`, `order_id`),
  FOREIGN KEY (`batch_id`) REFERENCES `delivery_batches`(`id`),
  FOREIGN KEY (`order_id`) REFERENCES `orders`(`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 配送位置记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS `delivery_location` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `batch_id` INT NOT NULL,
  `latitude` DECIMAL(10,7),
  `longitude` DECIMAL(10,7),
  `address` VARCHAR(500),
  `path_index` INT,
  `recorded_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`batch_id`) REFERENCES `delivery_batches`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
