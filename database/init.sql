SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `ecommerce_logistics` DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `ecommerce_logistics`;

-- 用户表
CREATE TABLE IF NOT EXISTS `users` (
  `id` INT PRIMARY KEY AUTO_INCREMENT,
  `username` VARCHAR(20) NOT NULL,
  `password` VARCHAR(100) NOT NULL,
  `role` ENUM('merchant','driver','consumer','admin') NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- BCrypt hash of "123"
INSERT INTO `users` (`username`,`password`,`role`) VALUES
('admin','$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2','admin'),
('张商户','$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2','merchant'),
('李配送','$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2','driver'),
('王消费','$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2','consumer'),
('刘天赐','$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2','merchant'),
('刘天赐','$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2','driver'),
('刘天赐','$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2','consumer'),
('刘天赐','$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2','admin');

-- 仓库表
CREATE TABLE IF NOT EXISTS `warehouse` (
  `id` INT PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL,
  `city` VARCHAR(20) NOT NULL,
  `address` VARCHAR(200),
  `longitude` DECIMAL(10,6),
  `latitude` DECIMAL(10,6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `warehouse` VALUES
(1,'华北一号仓','北京','北京市大兴区亦庄开发区',116.527834,39.793421),
(2,'华东中心仓','上海','上海市嘉定区江桥镇物流园',121.329876,31.302547),
(3,'华南智能仓','广州','广州市白云区太和镇物流基地',113.298765,23.256432),
(4,'西南枢纽仓','成都','成都市双流区航空港物流园',104.023456,30.567891),
(5,'华中分拨仓','武汉','武汉市东西湖区保税物流园',114.134567,30.678912);

-- 配送员表
CREATE TABLE IF NOT EXISTS `delivery_personnel` (
  `id` INT PRIMARY KEY AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `warehouse_id` INT NOT NULL,
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`),
  FOREIGN KEY (`warehouse_id`) REFERENCES `warehouse`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `delivery_personnel` VALUES (1, 3, 1), (2, 6, 2);

-- 地址表
CREATE TABLE IF NOT EXISTS `address` (
  `id` INT PRIMARY KEY AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `receiver_name` VARCHAR(50) NOT NULL,
  `phone` VARCHAR(20) NOT NULL,
  `province` VARCHAR(20),
  `city` VARCHAR(20),
  `district` VARCHAR(20),
  `detail` VARCHAR(200),
  `is_default` TINYINT(1) DEFAULT 0,
  `longitude` DECIMAL(10,6),
  `latitude` DECIMAL(10,6),
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `address` (`user_id`,`receiver_name`,`phone`,`province`,`city`,`district`,`detail`,`is_default`,`longitude`,`latitude`) VALUES
(4,'王消费','13800001111','北京','北京市','朝阳区','建国路88号',1,116.472017,39.920843),
(4,'王消费','13800001111','上海','上海市','浦东新区','陆家嘴环路1000号',0,121.499718,31.239703),
(7,'刘天赐','13900002222','广州','广州市','天河区','天河路385号',1,113.330803,23.137024),
(7,'刘天赐','13900002222','北京','北京市','海淀区','中关村大街1号',0,116.310905,39.992806);

-- 库存表
CREATE TABLE IF NOT EXISTS `inventory` (
  `product_id` INT PRIMARY KEY AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `warehouse_id` INT NOT NULL DEFAULT 1,
  `product_name` VARCHAR(100) NOT NULL,
  `quantity` INT NOT NULL DEFAULT 0,
  `stock_in_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `image_url` VARCHAR(255),
  `is_published` TINYINT(1) NOT NULL DEFAULT 0,
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `inventory` (`user_id`,`warehouse_id`,`product_name`,`quantity`,`stock_in_date`,`is_published`) VALUES
(2,1,'小米14 Ultra手机',50,'2026-01-15 10:00:00',1),
(2,1,'华为MatePad Pro平板',30,'2026-01-16 14:00:00',1),
(2,2,'索尼WH-1000XM5耳机',100,'2026-01-17 09:00:00',1),
(2,1,'戴森V15吸尘器',20,'2026-01-18 11:00:00',1),
(5,3,'MacBook Pro 16寸',15,'2026-01-20 08:00:00',1),
(5,2,'iPad Air 6',40,'2026-01-21 10:00:00',1),
(5,1,'AirPods Pro 3',200,'2026-01-22 09:00:00',1),
(2,4,'小米电动滑板车',25,'2026-02-01 10:00:00',0),
(5,5,'Switch 2游戏机',60,'2026-02-05 14:00:00',1);

-- 商城表
CREATE TABLE IF NOT EXISTS `mall` (
  `product_id` INT PRIMARY KEY,
  `warehouse_id` INT NOT NULL DEFAULT 1,
  `product_name` VARCHAR(100) NOT NULL,
  `description` TEXT,
  `available_quantity` INT NOT NULL DEFAULT 0,
  `price` DECIMAL(10,2) NOT NULL,
  `is_published` TINYINT(1) NOT NULL DEFAULT 1,
  `publish_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `image_url` VARCHAR(255),
  FOREIGN KEY (`product_id`) REFERENCES `inventory`(`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `mall` VALUES
(1,1,'小米14 Ultra手机','骁龙8 Gen3，徕卡影像，5000mAh大电池',50,5999.00,1,'2026-01-15 12:00:00',NULL),
(2,1,'华为MatePad Pro平板','12.6英寸OLED屏，麒麟9000S芯片',30,4999.00,1,'2026-01-16 15:00:00',NULL),
(3,2,'索尼WH-1000XM5耳机','行业领先降噪，30小时续航',100,2499.00,1,'2026-01-17 10:00:00',NULL),
(4,1,'戴森V15吸尘器','激光探测灰尘，智能吸力调节',20,4990.00,1,'2026-01-18 12:00:00',NULL),
(5,3,'MacBook Pro 16寸','M4 Pro芯片，36GB内存，1TB存储',15,19999.00,1,'2026-01-20 09:00:00',NULL),
(6,2,'iPad Air 6','M3芯片，10.9英寸，支持Apple Pencil',40,4799.00,1,'2026-01-21 11:00:00',NULL),
(7,1,'AirPods Pro 3','主动降噪，空间音频，USB-C充电',200,1899.00,1,'2026-01-22 10:00:00',NULL),
(9,5,'Switch 2游戏机','次世代掌机，4K输出，向下兼容',60,2599.00,1,'2026-02-05 15:00:00',NULL);

-- 订单表
CREATE TABLE IF NOT EXISTS `orders` (
  `order_id` INT PRIMARY KEY AUTO_INCREMENT,
  `warehouse_id` INT NOT NULL DEFAULT 1,
  `user_id` INT NOT NULL,
  `product_id` INT NOT NULL,
  `product_name` VARCHAR(100),
  `quantity` INT NOT NULL DEFAULT 1,
  `total_price` DECIMAL(10,2),
  `address_id` INT,
  `status` INT NOT NULL DEFAULT 0 COMMENT '0待发货 1已发货 2已取件 3配送中 4已送达 5已完成',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`),
  FOREIGN KEY (`product_id`) REFERENCES `inventory`(`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `orders` (`warehouse_id`,`user_id`,`product_id`,`product_name`,`quantity`,`total_price`,`address_id`,`status`,`create_time`) VALUES
(1,4,1,'小米14 Ultra手机',1,5999.00,1,5,'2026-01-20 14:00:00'),
(1,4,7,'AirPods Pro 3',2,3798.00,1,5,'2026-01-23 10:00:00'),
(2,4,3,'索尼WH-1000XM5耳机',1,2499.00,2,4,'2026-01-25 16:00:00'),
(1,7,2,'华为MatePad Pro平板',1,4999.00,3,5,'2026-01-28 09:00:00'),
(3,7,5,'MacBook Pro 16寸',1,19999.00,3,3,'2026-02-01 11:00:00'),
(1,4,4,'戴森V15吸尘器',1,4990.00,1,2,'2026-02-05 14:00:00'),
(2,7,6,'iPad Air 6',1,4799.00,4,1,'2026-02-10 10:00:00'),
(5,4,9,'Switch 2游戏机',2,5198.00,2,0,'2026-02-15 09:00:00'),
(1,7,1,'小米14 Ultra手机',1,5999.00,3,0,'2026-02-18 16:00:00'),
(1,4,7,'AirPods Pro 3',1,1899.00,1,0,'2026-02-19 08:00:00');

-- 配送批次表
CREATE TABLE IF NOT EXISTS `delivery_batches` (
  `id` INT PRIMARY KEY AUTO_INCREMENT,
  `driver_id` INT NOT NULL,
  `warehouse_id` INT NOT NULL,
  `status` VARCHAR(20) DEFAULT 'pending',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `start_time` DATETIME,
  `end_time` DATETIME,
  FOREIGN KEY (`warehouse_id`) REFERENCES `warehouse`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 配送批次订单关联表
CREATE TABLE IF NOT EXISTS `delivery_batch_orders` (
  `id` INT PRIMARY KEY AUTO_INCREMENT,
  `batch_id` INT NOT NULL,
  `order_id` INT NOT NULL,
  `sequence` INT DEFAULT 0,
  FOREIGN KEY (`batch_id`) REFERENCES `delivery_batches`(`id`),
  FOREIGN KEY (`order_id`) REFERENCES `orders`(`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 配送位置表
CREATE TABLE IF NOT EXISTS `delivery_location` (
  `id` INT PRIMARY KEY AUTO_INCREMENT,
  `batch_id` INT NOT NULL,
  `longitude` DECIMAL(10,6),
  `latitude` DECIMAL(10,6),
  `timestamp` DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`batch_id`) REFERENCES `delivery_batches`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 配送路线表
CREATE TABLE IF NOT EXISTS `delivery_route` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `batch_id` INT NULL,
  `delivery_time` DATETIME NOT NULL,
  `warehouse_id` INT NOT NULL,
  `route_data` TEXT NOT NULL,
  `total_distance` DECIMAL(10,2),
  `total_duration` INT,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  `current_index` INT DEFAULT 0,
  `started_at` DATETIME,
  `completed_at` DATETIME,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_batch_id` (`batch_id`),
  INDEX `idx_delivery_time_warehouse` (`delivery_time`, `warehouse_id`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
