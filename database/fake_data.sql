-- ============================================================
-- 电商物流管理系统 - 假数据 (fake_data.sql)
-- 业务流程: 商家入库 → 上架 → 顾客下单 → 商家发货 → 快递员揽收 → 运输 → 到达 → 顾客收货
-- 订单状态: 0=未发货, 1=已发货, 2=已揽收, 3=运输中, 4=已到达, 5=已收货
-- 批次状态: 0=待出发, 1=配送中, 2=已完成
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 清空所有表（按依赖顺序）
DELETE FROM delivery_location;
DELETE FROM delivery_route;
DELETE FROM delivery_batch_orders;
DELETE FROM delivery_batches;
DELETE FROM orders;
DELETE FROM mall;
DELETE FROM inventory;
DELETE FROM delivery_personnel;
DELETE FROM address;
DELETE FROM users;

-- ============================================================
-- 1. 用户 (BCrypt hash of "123")
-- ============================================================
INSERT INTO users (id, username, password, role) VALUES
(1,  '系统管理员', '$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2', 'admin'),
(2,  '京东自营',   '$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2', 'merchant'),
(3,  '天猫超市',   '$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2', 'merchant'),
(4,  '苏宁易购',   '$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2', 'merchant'),
(5,  '张伟',       '$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2', 'driver'),
(6,  '李强',       '$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2', 'driver'),
(7,  '王磊',       '$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2', 'driver'),
(8,  '赵刚',       '$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2', 'driver'),
(9,  '刘天赐',     '$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2', 'consumer'),
(10, '陈小明',     '$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2', 'consumer'),
(11, '林美琪',     '$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2', 'consumer'),
(12, '黄晓峰',     '$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2', 'consumer'),
(13, '周雨萱',     '$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2', 'consumer'),
(14, '吴志远',     '$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2', 'consumer');

-- ============================================================
-- 2. 配送员绑定仓库
-- ============================================================
INSERT INTO delivery_personnel (id, user_id, warehouse_id) VALUES
(1, 5, 1),
(2, 6, 2),
(3, 7, 3),
(4, 8, 6);

-- ============================================================
-- 3. 消费者收货地址
-- ============================================================
INSERT INTO address (id, user_id, receiver_name, receiver_phone, province, city, district, detail_address, latitude, longitude, is_default) VALUES
(1,  9,  '刘天赐', '13800001001', '北京市', '北京市', '朝阳区', '三里屯SOHO A座1201',          39.9332700, 116.4543200, 1),
(2,  9,  '刘天赐', '13800001001', '北京市', '北京市', '海淀区', '中关村软件园二期8号楼',        40.0508300, 116.3032900, 0),
(3,  10, '陈小明', '13800001002', '上海市', '上海市', '浦东新区', '陆家嘴环路1000号',           31.2397200, 121.4997600, 1),
(4,  11, '林美琪', '13800001003', '广东省', '广州市', '天河区', '天河路385号太古汇',            23.1291100, 113.3250200, 1),
(5,  12, '黄晓峰', '13800001004', '浙江省', '杭州市', '西湖区', '文三路478号华星时代广场',      30.2741500, 120.1301900, 1),
(6,  13, '周雨萱', '13800001005', '四川省', '成都市', '锦江区', '春熙路步行街IFS国际金融中心',   30.6571300, 104.0817200, 1),
(7,  14, '吴志远', '13800001006', '湖北省', '武汉市', '武昌区', '楚河汉街万达广场',            30.5537800, 114.3529500, 1),
(8,  10, '陈小明', '13800001002', '上海市', '上海市', '徐汇区', '漕河泾开发区田林路388号',      31.1685200, 121.4012300, 0),
(9,  11, '林美琪', '13800001003', '广东省', '深圳市', '南山区', '科技园南区深南大道9966号',      22.5431000, 113.9530000, 0),
(10, 14, '吴志远', '13800001006', '湖北省', '武汉市', '江汉区', '武汉天地壹方购物中心',        30.5985600, 114.2935800, 0);

-- ============================================================
-- 4. 库存 (商家入库)
-- R2: https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/
-- ============================================================
INSERT INTO inventory (product_id, user_id, product_name, quantity, image_url, is_published, warehouse_id, stock_in_date) VALUES
(1001, 2, 'PS5游戏手柄',       200,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/ps5-controller.jpg',       1, 1, '2026-01-15 09:00:00'),
(1002, 2, 'Switch游戏机',      150,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/nintendo-switch.webp',     1, 1, '2026-01-15 09:30:00'),
(1003, 2, 'AirPods Pro',       300,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/airpods-pro.jpg',          1, 1, '2026-01-16 10:00:00'),
(1004, 2, '罗技无线鼠标',      500,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/wireless-mouse.jpg',       1, 1, '2026-01-16 10:30:00'),
(1005, 2, '机械键盘',          180,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/mechanical-keyboard.jpg',  1, 1, '2026-01-17 08:00:00'),
(1006, 3, '苹果17Pro',         100,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/iphone-17-pro.jpg',       1, 2, '2026-01-18 09:00:00'),
(1007, 3, '黑人牙膏',          800,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/darlie-toothpaste.jpg',   1, 2, '2026-01-18 09:30:00'),
(1008, 3, '飞利浦电动牙刷',    250,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/philips-toothbrush.jpg',  1, 2, '2026-01-19 10:00:00'),
(1009, 3, '康师傅红烧牛肉面',  1000, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/instant-noodles.jpg',     1, 2, '2026-01-19 10:30:00'),
(1010, 3, '农夫山泉矿泉水',    2000, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/mineral-water.jpg',       1, 2, '2026-01-20 08:00:00'),
(1011, 4, '乐事薯片',          600,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/lays-chips.jpg',          1, 3, '2026-01-20 09:00:00'),
(1012, 4, '徕芬高速吹风机',    120,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/laifen-hairdryer.jpg',    1, 3, '2026-01-21 09:30:00'),
(1013, 4, 'Anker充电宝',       350,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/power-bank.jpg',          1, 3, '2026-01-21 10:00:00'),
(1014, 4, '小米蓝牙耳机',      400,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/xiaomi-earbuds.jpg',      1, 3, '2026-01-22 10:30:00'),
(1015, 4, 'USB-C数据线',       1500, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/usb-c-cable.jpg',         1, 3, '2026-01-22 11:00:00');

-- ============================================================
-- 5. 商城上架
-- ============================================================
INSERT INTO mall (product_id, merchant_id, product_name, available_quantity, price, is_published, image_url, warehouse_id, publish_time) VALUES
(1001, 2, 'PS5游戏手柄',       80,  469.00,  1, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/ps5-controller.jpg',       1, '2026-01-20 10:00:00'),
(1002, 2, 'Switch游戏机',      50,  2099.00, 1, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/nintendo-switch.webp',     1, '2026-01-20 10:30:00'),
(1003, 2, 'AirPods Pro',       100, 1799.00, 1, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/airpods-pro.jpg',          1, '2026-01-21 09:00:00'),
(1004, 2, '罗技无线鼠标',      200, 299.00,  1, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/wireless-mouse.jpg',       1, '2026-01-21 09:30:00'),
(1005, 2, '机械键盘',          60,  599.00,  1, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/mechanical-keyboard.jpg',  1, '2026-01-22 08:00:00'),
(1006, 3, '苹果17Pro',         30,  8999.00, 1, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/iphone-17-pro.jpg',       2, '2026-01-23 09:00:00'),
(1007, 3, '黑人牙膏',          300, 12.90,   1, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/darlie-toothpaste.jpg',   2, '2026-01-23 09:30:00'),
(1008, 3, '飞利浦电动牙刷',    80,  349.00,  1, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/philips-toothbrush.jpg',  2, '2026-01-24 10:00:00'),
(1009, 3, '康师傅红烧牛肉面',  500, 4.50,    1, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/instant-noodles.jpg',     2, '2026-01-24 10:30:00'),
(1010, 3, '农夫山泉矿泉水',    800, 2.00,    1, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/mineral-water.jpg',       2, '2026-01-25 08:00:00'),
(1011, 4, '乐事薯片',          200, 8.90,    1, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/lays-chips.jpg',          3, '2026-01-25 09:00:00'),
(1012, 4, '徕芬高速吹风机',    40,  399.00,  1, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/laifen-hairdryer.jpg',    3, '2026-01-26 09:30:00'),
(1013, 4, 'Anker充电宝',       150, 129.00,  1, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/power-bank.jpg',          3, '2026-01-26 10:00:00'),
(1014, 4, '小米蓝牙耳机',      180, 199.00,  1, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/xiaomi-earbuds.jpg',      3, '2026-01-27 10:30:00'),
(1015, 4, 'USB-C数据线',       600, 19.90,   1, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/usb-c-cable.jpg',         3, '2026-01-27 11:00:00');

-- ============================================================
-- 6. 订单 (顾客下单)
-- 状态: 0=未发货, 1=已发货, 2=已揽收, 3=运输中, 4=已到达, 5=已收货
-- ============================================================
INSERT INTO orders (order_id, customer_id, merchant_id, product_id, product_name, quantity, unit_price, total_amount, address_id, image_url, status, warehouse_id, order_time, ship_time, pickup_time, delivery_time, receive_time) VALUES
-- === 已收货 (status=5) ===
(10001, 9,  2, 1001, 'PS5游戏手柄',      1, 469.00,  469.00,  1, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/ps5-controller.jpg',      5, 1, '2026-02-01 14:30:00', '2026-02-01 17:00:00', '2026-02-02 08:30:00', '2026-02-03 14:00:00', '2026-02-04 16:00:00'),
(10002, 10, 3, 1006, '苹果17Pro',         1, 8999.00, 8999.00, 3, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/iphone-17-pro.jpg',      5, 2, '2026-02-01 15:00:00', '2026-02-01 18:00:00', '2026-02-02 09:00:00', '2026-02-03 16:00:00', '2026-02-04 10:30:00'),
(10003, 11, 4, 1012, '徕芬高速吹风机',    1, 399.00,  399.00,  4, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/laifen-hairdryer.jpg',   5, 3, '2026-02-02 09:15:00', '2026-02-02 14:00:00', '2026-02-03 08:30:00', '2026-02-04 15:00:00', '2026-02-05 11:00:00'),
(10004, 12, 3, 1009, '康师傅红烧牛肉面',  24, 4.50,   108.00,  5, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/instant-noodles.jpg',    5, 2, '2026-02-02 10:00:00', '2026-02-02 15:00:00', '2026-02-03 09:00:00', '2026-02-04 17:00:00', '2026-02-05 14:00:00'),
(10005, 13, 2, 1003, 'AirPods Pro',        1, 1799.00, 1799.00, 6, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/airpods-pro.jpg',        5, 1, '2026-02-03 11:30:00', '2026-02-03 16:00:00', '2026-02-04 08:00:00', '2026-02-05 14:00:00', '2026-02-06 09:00:00'),
(10006, 14, 4, 1011, '乐事薯片',          10, 8.90,   89.00,   7, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/lays-chips.jpg',         5, 3, '2026-02-03 14:00:00', '2026-02-03 18:00:00', '2026-02-04 08:30:00', '2026-02-05 16:00:00', '2026-02-06 15:30:00'),
(10007, 9,  3, 1007, '黑人牙膏',          5,  12.90,  64.50,   1, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/darlie-toothpaste.jpg',  5, 2, '2026-02-04 08:00:00', '2026-02-04 12:00:00', '2026-02-05 08:00:00', '2026-02-06 14:00:00', '2026-02-07 10:00:00'),
(10008, 10, 2, 1004, '罗技无线鼠标',      2,  299.00, 598.00,  3, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/wireless-mouse.jpg',     5, 1, '2026-02-05 09:30:00', '2026-02-05 14:00:00', '2026-02-06 08:00:00', '2026-02-07 15:00:00', '2026-02-08 11:00:00'),
-- === 已到达 (status=4) ===
(10009, 11, 3, 1008, '飞利浦电动牙刷',    1, 349.00,  349.00,  4, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/philips-toothbrush.jpg', 4, 2, '2026-02-10 10:00:00', '2026-02-10 15:00:00', '2026-02-11 08:00:00', '2026-02-13 08:30:00', NULL),
(10010, 12, 4, 1013, 'Anker充电宝',       2, 129.00,  258.00,  5, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/power-bank.jpg',         4, 3, '2026-02-10 11:00:00', '2026-02-10 16:00:00', '2026-02-11 08:30:00', '2026-02-13 09:00:00', NULL),
-- === 运输中 (status=3) ===
(10011, 13, 2, 1002, 'Switch游戏机',      1, 2099.00, 2099.00, 6, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/nintendo-switch.webp',   3, 1, '2026-02-12 14:00:00', '2026-02-12 18:00:00', '2026-02-13 08:00:00', NULL, NULL),
(10012, 14, 3, 1010, '农夫山泉矿泉水',    48, 2.00,   96.00,   7, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/mineral-water.jpg',      3, 2, '2026-02-12 15:00:00', '2026-02-12 19:00:00', '2026-02-13 08:30:00', NULL, NULL),
(10013, 9,  4, 1014, '小米蓝牙耳机',      1, 199.00,  199.00,  2, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/xiaomi-earbuds.jpg',     3, 3, '2026-02-13 09:00:00', '2026-02-13 14:00:00', '2026-02-14 08:00:00', NULL, NULL),
-- === 已揽收 (status=2) ===
(10014, 10, 2, 1005, '机械键盘',          1, 599.00,  599.00,  8, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/mechanical-keyboard.jpg',2, 1, '2026-02-15 10:00:00', '2026-02-15 15:00:00', '2026-02-16 09:00:00', NULL, NULL),
(10015, 11, 4, 1015, 'USB-C数据线',       3, 19.90,   59.70,   9, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/usb-c-cable.jpg',        2, 3, '2026-02-15 11:00:00', '2026-02-15 16:00:00', '2026-02-16 10:00:00', NULL, NULL),
-- === 已发货 (status=1) ===
(10016, 12, 2, 1001, 'PS5游戏手柄',       2, 469.00,  938.00,  5, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/ps5-controller.jpg',     1, 1, '2026-02-17 14:00:00', '2026-02-17 16:00:00', NULL, NULL, NULL),
(10017, 13, 3, 1007, '黑人牙膏',          10, 12.90,  129.00,  6, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/darlie-toothpaste.jpg',  1, 2, '2026-02-18 09:00:00', '2026-02-18 11:00:00', NULL, NULL, NULL),
-- === 未发货 (status=0) ===
(10018, 14, 2, 1003, 'AirPods Pro',        1, 1799.00, 1799.00, 10,'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/airpods-pro.jpg',       0, 1, '2026-02-19 10:00:00', NULL, NULL, NULL, NULL),
(10019, 9,  4, 1012, '徕芬高速吹风机',    1, 399.00,  399.00,  1, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/laifen-hairdryer.jpg',   0, 3, '2026-02-19 15:30:00', NULL, NULL, NULL, NULL),
(10020, 10, 3, 1009, '康师傅红烧牛肉面',  12, 4.50,   54.00,   3, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/instant-noodles.jpg',    0, 2, '2026-02-20 08:00:00', NULL, NULL, NULL, NULL);

-- ============================================================
-- 7. 配送批次
-- ============================================================
INSERT INTO delivery_batches (id, driver_id, warehouse_id, status, created_at, started_at, completed_at) VALUES
(1, 5, 1, 2, '2026-02-02 08:00:00', '2026-02-02 08:30:00', '2026-02-04 16:00:00'),
(2, 6, 2, 2, '2026-02-02 08:30:00', '2026-02-02 09:00:00', '2026-02-05 14:00:00'),
(3, 7, 3, 2, '2026-02-03 08:00:00', '2026-02-03 08:30:00', '2026-02-06 15:30:00'),
(4, 5, 1, 2, '2026-02-05 08:00:00', '2026-02-06 08:00:00', '2026-02-08 11:00:00'),
(5, 6, 2, 2, '2026-02-04 08:00:00', '2026-02-05 08:00:00', '2026-02-07 10:00:00'),
(6, 5, 1, 1, '2026-02-14 08:00:00', '2026-02-14 08:30:00', NULL),
(7, 6, 2, 1, '2026-02-14 08:30:00', '2026-02-14 09:00:00', NULL),
(8, 5, 1, 0, '2026-02-16 08:00:00', NULL, NULL),
(9, 7, 3, 0, '2026-02-16 08:30:00', NULL, NULL);

-- ============================================================
-- 8. 批次-订单关联
-- ============================================================
INSERT INTO delivery_batch_orders (batch_id, order_id, stop_sequence) VALUES
(1, 10001, 1), (1, 10005, 2),
(2, 10002, 1), (2, 10004, 2),
(3, 10003, 1), (3, 10006, 2),
(4, 10008, 1),
(5, 10007, 1),
(6, 10011, 1), (6, 10013, 2),
(7, 10012, 1), (7, 10009, 2), (7, 10010, 3),
(8, 10014, 1),
(9, 10015, 1);

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 数据统计
-- 用户: 14 (1管理员 + 3商户 + 4配送员 + 6消费者)
-- 库存: 15个商品 (3商家各5个)
-- 商城: 15个上架商品
-- 订单: 20笔 (8已收货 + 2已到达 + 3运输中 + 2已揽收 + 2已发货 + 3未发货)
-- 批次: 9个 (5已完成 + 2配送中 + 2待出发)
-- 地址: 10个
-- 密码: 全部为 "123"
-- ============================================================
