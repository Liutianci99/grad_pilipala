-- ============================================================
-- 电商物流管理系统 - 假数据 (fake_data.sql)
-- 业务流程: 商家入库 → 上架 → 顾客下单 → 商家发货 → 快递员揽收 → 运输 → 到达 → 顾客收货
-- 订单状态: 0=未发货, 1=已发货, 2=已揽收, 3=运输中, 4=已到达, 5=已收货
-- 批次状态: 0=待出发, 1=配送中, 2=已完成
-- 路线数据存储在 delivery_batches.route_data
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 清空所有表（按依赖顺序）
DELETE FROM delivery_location;
DELETE FROM delivery_batch_orders;
DELETE FROM delivery_batches;
DELETE FROM orders;
DELETE FROM mall;
DELETE FROM inventory;
DELETE FROM address;
DELETE FROM users;

-- ============================================================
-- 1. 用户 (BCrypt hash of "123")
-- ============================================================
INSERT INTO users (id, username, password, role, warehouse_id) VALUES
(1,  '系统管理员', '$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2', 'admin',    NULL),
(2,  '京东自营',   '$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2', 'merchant',  NULL),
(3,  '天猫超市',   '$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2', 'merchant',  NULL),
(4,  '苏宁易购',   '$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2', 'merchant',  NULL),
(5,  '张伟',       '$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2', 'driver',    1),
(6,  '李强',       '$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2', 'driver',    2),
(7,  '王磊',       '$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2', 'driver',    3),
(8,  '赵刚',       '$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2', 'driver',    6),
(9,  '刘天赐',     '$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2', 'consumer',  NULL),
(10, '陈小明',     '$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2', 'consumer',  NULL),
(11, '林美琪',     '$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2', 'consumer',  NULL),
(12, '黄晓峰',     '$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2', 'consumer',  NULL),
(13, '周雨萱',     '$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2', 'consumer',  NULL),
(14, '吴志远',     '$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2', 'consumer',  NULL),
(15, '赵丽颖',     '$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2', 'consumer',  NULL),
(16, '孙浩然',     '$2a$10$plrVTP6AlVFrM2QWZnWiguiqrxkbsl2IscIn.AUfyDVR08JEzM9q2', 'consumer',  NULL);

-- ============================================================
-- 2. 消费者收货地址
-- ============================================================
INSERT INTO address (id, user_id, receiver_name, receiver_phone, province, city, district, detail_address, latitude, longitude, is_default) VALUES
(1,  9,  '刘天赐', '13800001001', '江苏省', '南京市', '玄武区',   '新街口金鹰国际购物中心',              32.0484200, 118.7780500, 1),
(2,  9,  '刘天赐', '13800001001', '江苏省', '南京市', '建邺区',   '河西万达广场',                       32.0158000, 118.7312000, 0),
(3,  10, '陈小明', '13800001002', '江苏省', '苏州市', '工业园区', '苏州中心广场',                        31.3115000, 120.6802000, 1),
(4,  11, '林美琪', '13800001003', '江苏省', '无锡市', '滨湖区',   '太湖新城万象城',                     31.4912000, 120.2856000, 1),
(5,  12, '黄晓峰', '13800001004', '江苏省', '常州市', '天宁区',   '南大街商业步行街',                    31.7798000, 119.9745000, 1),
(6,  13, '周雨萱', '13800001005', '江苏省', '扬州市', '广陵区',   '东关街历史文化街区',                  32.3975000, 119.4352000, 1),
(7,  14, '吴志远', '13800001006', '江苏省', '镇江市', '京口区',   '大市口商业中心',                     32.2045000, 119.4558000, 1),
(8,  10, '陈小明', '13800001002', '江苏省', '苏州市', '姑苏区',   '观前街商业步行街',                    31.3103000, 120.6315000, 0),
(9,  11, '林美琪', '13800001003', '江苏省', '无锡市', '梁溪区',   '南禅寺步行街',                       31.5668000, 120.3052000, 0),
(10, 14, '吴志远', '13800001006', '江苏省', '镇江市', '润州区',   '西津渡古街',                         32.2136000, 119.4253000, 0),
(11, 15, '赵丽颖', '13800001007', '江苏省', '南通市', '崇川区',   '南大街文峰城市广场',                  32.0162000, 120.8735000, 1),
(12, 16, '孙浩然', '13800001008', '江苏省', '徐州市', '云龙区',   '彭城广场金鹰购物中心',                34.2615000, 117.1958000, 1);

-- ============================================================
-- 4. 库存 (商家入库) — 带商品描述
-- R2: https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/
-- ============================================================
INSERT INTO inventory (product_id, user_id, product_name, description, quantity, image_url, is_published, warehouse_id, stock_in_date) VALUES
(1001, 2, 'PS5游戏手柄',       'Sony DualSense无线手柄，自适应扳机+触觉反馈，USB-C充电，兼容PS5/PC，午夜黑配色',                200,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/ps5-controller.jpg',       1, 1, '2026-01-15 09:00:00'),
(1002, 2, 'Switch游戏机',      'Nintendo Switch OLED版，7英寸OLED屏幕，64GB内存，白色Joy-Con，支持TV/桌面/掌机三模式',          150,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/nintendo-switch.webp',     1, 1, '2026-01-15 09:30:00'),
(1003, 2, 'AirPods Pro',       'Apple AirPods Pro 第二代，主动降噪+通透模式，自适应音频，MagSafe充电盒，USB-C接口',              300,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/airpods-pro.jpg',          1, 1, '2026-01-16 10:00:00'),
(1004, 2, '罗技无线鼠标',      'Logitech MX Anywhere 3，4000DPI激光传感器，蓝牙+USB双模，可在玻璃上使用，70天续航',             500,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/wireless-mouse.jpg',       1, 1, '2026-01-16 10:30:00'),
(1005, 2, '机械键盘',          'Logitech K845机械键盘，Cherry MX红轴，全尺寸104键，白色背光，铝合金面板，有线USB',              180,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/mechanical-keyboard.jpg',  1, 1, '2026-01-17 08:00:00'),
(1006, 3, '苹果17Pro',         'iPhone 17 Pro，A19 Pro芯片，6.3英寸ProMotion屏幕，4800万像素三摄，钛金属边框，256GB',           100,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/iphone-17-pro.jpg',       1, 2, '2026-01-18 09:00:00'),
(1007, 3, '黑人牙膏',          '黑人（DARLIE）双重薄荷牙膏225g，含氟防蛀，清新口气持久，温和不刺激，家庭装',                    800,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/darlie-toothpaste.jpg',   1, 2, '2026-01-18 09:30:00'),
(1008, 3, '飞利浦电动牙刷',    'Philips Sonicare 9900 Prestige，声波震动62000次/分钟，AI智能感应，4种模式，续航14天',            250,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/philips-toothbrush.jpg',  1, 2, '2026-01-19 10:00:00'),
(1009, 3, '康师傅红烧牛肉面',  '康师傅经典红烧牛肉面，大块牛肉粒+浓郁汤底，方便面袋装5连包，净含量500g',                       1000, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/instant-noodles.jpg',     1, 2, '2026-01-19 10:30:00'),
(1010, 3, '农夫山泉矿泉水',    '农夫山泉天然饮用水550ml，源自长白山深层泉水，弱碱性，适合日常饮用，单瓶装',                     2000, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/mineral-water.jpg',       1, 2, '2026-01-20 08:00:00'),
(1011, 4, '乐事薯片',          '乐事（Lay''s）经典原味薯片75g，精选优质土豆，薄脆口感，休闲零食必备',                            600,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/lays-chips.jpg',          1, 3, '2026-01-20 09:00:00'),
(1012, 4, '徕芬高速吹风机',    '徕芬SE高速吹风机，11万转无刷电机，1400W大功率，3分钟速干，负离子护发，轻量设计仅325g',           120,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/laifen-hairdryer.jpg',    1, 3, '2026-01-21 09:30:00'),
(1013, 4, 'Anker充电宝',       'Anker 安克 PowerCore 10000mAh移动电源，USB-C双向快充20W，超薄便携，可充iPhone约3次',             350,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/power-bank.jpg',          1, 3, '2026-01-21 10:00:00'),
(1014, 4, '小米蓝牙耳机',      '小米Buds 4 Pro真无线降噪耳机，48dB深度降噪，LDAC高清音质，IP54防水，续航9小时',                  400,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/xiaomi-earbuds.jpg',      1, 3, '2026-01-22 10:30:00'),
(1015, 4, 'USB-C数据线',       'Anker USB-C to USB-C 100W快充数据线1.8m，尼龙编织耐弯折，支持PD3.0/QC4.0，兼容MacBook/iPad',    1500, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/usb-c-cable.jpg',         1, 3, '2026-01-22 11:00:00');

-- ============================================================
-- 5. 商城上架 — 带商品描述
-- ============================================================
INSERT INTO mall (product_id, merchant_id, product_name, description, available_quantity, price, is_published, image_url, warehouse_id, publish_time) VALUES
(1001, 2, 'PS5游戏手柄',       'Sony DualSense无线手柄，自适应扳机+触觉反馈，USB-C充电，兼容PS5/PC，午夜黑配色',                80,  469.00,  1, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/ps5-controller.jpg',       1, '2026-01-20 10:00:00'),
(1002, 2, 'Switch游戏机',      'Nintendo Switch OLED版，7英寸OLED屏幕，64GB内存，白色Joy-Con，支持TV/桌面/掌机三模式',          50,  2099.00, 1, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/nintendo-switch.webp',     1, '2026-01-20 10:30:00'),
(1003, 2, 'AirPods Pro',       'Apple AirPods Pro 第二代，主动降噪+通透模式，自适应音频，MagSafe充电盒，USB-C接口',              100, 1799.00, 1, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/airpods-pro.jpg',          1, '2026-01-21 09:00:00'),
(1004, 2, '罗技无线鼠标',      'Logitech MX Anywhere 3，4000DPI激光传感器，蓝牙+USB双模，可在玻璃上使用，70天续航',             200, 299.00,  1, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/wireless-mouse.jpg',       1, '2026-01-21 09:30:00'),
(1005, 2, '机械键盘',          'Logitech K845机械键盘，Cherry MX红轴，全尺寸104键，白色背光，铝合金面板，有线USB',              60,  599.00,  1, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/mechanical-keyboard.jpg',  1, '2026-01-22 08:00:00'),
(1006, 3, '苹果17Pro',         'iPhone 17 Pro，A19 Pro芯片，6.3英寸ProMotion屏幕，4800万像素三摄，钛金属边框，256GB',           30,  8999.00, 1, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/iphone-17-pro.jpg',       2, '2026-01-23 09:00:00'),
(1007, 3, '黑人牙膏',          '黑人（DARLIE）双重薄荷牙膏225g，含氟防蛀，清新口气持久，温和不刺激，家庭装',                    300, 12.90,   1, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/darlie-toothpaste.jpg',   2, '2026-01-23 09:30:00'),
(1008, 3, '飞利浦电动牙刷',    'Philips Sonicare 9900 Prestige，声波震动62000次/分钟，AI智能感应，4种模式，续航14天',            80,  349.00,  1, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/philips-toothbrush.jpg',  2, '2026-01-24 10:00:00'),
(1009, 3, '康师傅红烧牛肉面',  '康师傅经典红烧牛肉面，大块牛肉粒+浓郁汤底，方便面袋装5连包，净含量500g',                       500, 4.50,    1, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/instant-noodles.jpg',     2, '2026-01-24 10:30:00'),
(1010, 3, '农夫山泉矿泉水',    '农夫山泉天然饮用水550ml，源自长白山深层泉水，弱碱性，适合日常饮用，单瓶装',                     800, 2.00,    1, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/mineral-water.jpg',       2, '2026-01-25 08:00:00'),
(1011, 4, '乐事薯片',          '乐事（Lay''s）经典原味薯片75g，精选优质土豆，薄脆口感，休闲零食必备',                            200, 8.90,    1, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/lays-chips.jpg',          3, '2026-01-25 09:00:00'),
(1012, 4, '徕芬高速吹风机',    '徕芬SE高速吹风机，11万转无刷电机，1400W大功率，3分钟速干，负离子护发，轻量设计仅325g',           40,  399.00,  1, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/laifen-hairdryer.jpg',    3, '2026-01-26 09:30:00'),
(1013, 4, 'Anker充电宝',       'Anker 安克 PowerCore 10000mAh移动电源，USB-C双向快充20W，超薄便携，可充iPhone约3次',             150, 129.00,  1, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/power-bank.jpg',          3, '2026-01-26 10:00:00'),
(1014, 4, '小米蓝牙耳机',      '小米Buds 4 Pro真无线降噪耳机，48dB深度降噪，LDAC高清音质，IP54防水，续航9小时',                  180, 199.00,  1, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/xiaomi-earbuds.jpg',      3, '2026-01-27 10:30:00'),
(1015, 4, 'USB-C数据线',       'Anker USB-C to USB-C 100W快充数据线1.8m，尼龙编织耐弯折，支持PD3.0/QC4.0，兼容MacBook/iPad',    600, 19.90,   1, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/usb-c-cable.jpg',         3, '2026-01-27 11:00:00');

-- ============================================================
-- 6. 订单 (30笔)
-- 状态: 0=未发货, 1=已发货, 2=已揽收, 3=运输中, 4=已到达, 5=已收货
-- ============================================================
INSERT INTO orders (order_id, customer_id, merchant_id, product_id, product_name, quantity, unit_price, total_amount, address_id, image_url, status, warehouse_id, order_time, ship_time, pickup_time, delivery_time, receive_time) VALUES
-- === 已收货 (status=5) — 12笔 ===
(10001, 9,  2, 1001, 'PS5游戏手柄',      1,  469.00,  469.00,  1,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/ps5-controller.jpg',      5, 1, '2026-02-01 14:30:00', '2026-02-01 17:00:00', '2026-02-02 08:30:00', '2026-02-03 14:00:00', '2026-02-04 16:00:00'),
(10002, 10, 3, 1006, '苹果17Pro',         1,  8999.00, 8999.00, 3,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/iphone-17-pro.jpg',      5, 2, '2026-02-01 15:00:00', '2026-02-01 18:00:00', '2026-02-02 09:00:00', '2026-02-03 16:00:00', '2026-02-04 10:30:00'),
(10003, 11, 4, 1012, '徕芬高速吹风机',    1,  399.00,  399.00,  4,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/laifen-hairdryer.jpg',   5, 3, '2026-02-02 09:15:00', '2026-02-02 14:00:00', '2026-02-03 08:30:00', '2026-02-04 15:00:00', '2026-02-05 11:00:00'),
(10004, 12, 3, 1009, '康师傅红烧牛肉面',  24, 4.50,    108.00,  5,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/instant-noodles.jpg',    5, 2, '2026-02-02 10:00:00', '2026-02-02 15:00:00', '2026-02-03 09:00:00', '2026-02-04 17:00:00', '2026-02-05 14:00:00'),
(10005, 13, 2, 1003, 'AirPods Pro',        1,  1799.00, 1799.00, 6,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/airpods-pro.jpg',        5, 1, '2026-02-03 11:30:00', '2026-02-03 16:00:00', '2026-02-04 08:00:00', '2026-02-05 14:00:00', '2026-02-06 09:00:00'),
(10006, 14, 4, 1011, '乐事薯片',          10, 8.90,    89.00,   7,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/lays-chips.jpg',         5, 3, '2026-02-03 14:00:00', '2026-02-03 18:00:00', '2026-02-04 08:30:00', '2026-02-05 16:00:00', '2026-02-06 15:30:00'),
(10007, 9,  3, 1007, '黑人牙膏',          5,  12.90,   64.50,   1,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/darlie-toothpaste.jpg',  5, 2, '2026-02-04 08:00:00', '2026-02-04 12:00:00', '2026-02-05 08:00:00', '2026-02-06 14:00:00', '2026-02-07 10:00:00'),
(10008, 10, 2, 1004, '罗技无线鼠标',      2,  299.00,  598.00,  3,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/wireless-mouse.jpg',     5, 1, '2026-02-05 09:30:00', '2026-02-05 14:00:00', '2026-02-06 08:00:00', '2026-02-07 15:00:00', '2026-02-08 11:00:00'),
(10009, 15, 3, 1008, '飞利浦电动牙刷',    1,  349.00,  349.00,  11, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/philips-toothbrush.jpg', 5, 2, '2026-02-05 10:00:00', '2026-02-05 15:00:00', '2026-02-06 08:30:00', '2026-02-07 16:00:00', '2026-02-08 09:00:00'),
(10010, 16, 4, 1013, 'Anker充电宝',       1,  129.00,  129.00,  12, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/power-bank.jpg',         5, 3, '2026-02-06 11:00:00', '2026-02-06 16:00:00', '2026-02-07 08:00:00', '2026-02-08 14:00:00', '2026-02-09 10:00:00'),
(10011, 11, 2, 1005, '机械键盘',          1,  599.00,  599.00,  4,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/mechanical-keyboard.jpg',5, 1, '2026-02-07 09:00:00', '2026-02-07 14:00:00', '2026-02-08 08:00:00', '2026-02-09 15:00:00', '2026-02-10 11:00:00'),
(10012, 12, 3, 1010, '农夫山泉矿泉水',    48, 2.00,    96.00,   5,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/mineral-water.jpg',      5, 2, '2026-02-07 10:00:00', '2026-02-07 15:00:00', '2026-02-08 08:30:00', '2026-02-09 16:00:00', '2026-02-10 14:00:00'),
-- === 已到达 (status=4) — 1笔 ===
(10013, 11, 3, 1008, '飞利浦电动牙刷',    1,  349.00,  349.00,  4,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/philips-toothbrush.jpg', 4, 2, '2026-02-10 10:00:00', '2026-02-10 15:00:00', '2026-02-11 08:00:00', '2026-02-13 08:30:00', NULL),
(10014, 12, 4, 1013, 'Anker充电宝',       2,  129.00,  258.00,  5,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/power-bank.jpg',         5, 3, '2026-02-10 11:00:00', '2026-02-10 16:00:00', '2026-02-11 08:30:00', '2026-02-13 09:00:00', '2026-02-16 15:00:00'),
(10015, 15, 2, 1002, 'Switch游戏机',      1,  2099.00, 2099.00, 11, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/nintendo-switch.webp',   5, 1, '2026-02-11 09:00:00', '2026-02-11 14:00:00', '2026-02-12 08:00:00', '2026-02-14 10:00:00', '2026-02-16 10:00:00'),
-- === 运输中 (status=3) — 2笔 ===
(10016, 13, 2, 1002, 'Switch游戏机',      1,  2099.00, 2099.00, 6,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/nintendo-switch.webp',   5, 1, '2026-02-12 14:00:00', '2026-02-12 18:00:00', '2026-02-13 08:00:00', '2026-02-14 14:00:00', '2026-02-16 11:00:00'),
(10017, 14, 3, 1010, '农夫山泉矿泉水',    48, 2.00,    96.00,   7,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/mineral-water.jpg',      3, 2, '2026-02-12 15:00:00', '2026-02-12 19:00:00', '2026-02-13 08:30:00', NULL, NULL),
(10018, 9,  4, 1014, '小米蓝牙耳机',      1,  199.00,  199.00,  2,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/xiaomi-earbuds.jpg',     5, 3, '2026-02-13 09:00:00', '2026-02-13 14:00:00', '2026-02-14 08:00:00', '2026-02-15 10:00:00', '2026-02-16 14:00:00'),
(10019, 16, 3, 1006, '苹果17Pro',         1,  8999.00, 8999.00, 12, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/iphone-17-pro.jpg',      3, 2, '2026-02-13 10:00:00', '2026-02-13 15:00:00', '2026-02-14 08:30:00', NULL, NULL),
-- === 已揽收 (status=2) — 3笔 ===
(10020, 10, 2, 1005, '机械键盘',          1,  599.00,  599.00,  8,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/mechanical-keyboard.jpg',2, 1, '2026-02-15 10:00:00', '2026-02-15 15:00:00', '2026-02-16 09:00:00', NULL, NULL),
(10021, 11, 4, 1015, 'USB-C数据线',       3,  19.90,   59.70,   9,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/usb-c-cable.jpg',        2, 3, '2026-02-15 11:00:00', '2026-02-15 16:00:00', '2026-02-16 10:00:00', NULL, NULL),
(10022, 15, 4, 1011, '乐事薯片',          20, 8.90,    178.00,  11, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/lays-chips.jpg',         2, 3, '2026-02-16 09:00:00', '2026-02-16 14:00:00', '2026-02-17 08:00:00', NULL, NULL),
-- === 已发货 (status=1) — 3笔 ===
(10023, 12, 2, 1001, 'PS5游戏手柄',       2,  469.00,  938.00,  5,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/ps5-controller.jpg',     1, 1, '2026-02-17 14:00:00', '2026-02-17 16:00:00', NULL, NULL, NULL),
(10024, 13, 3, 1007, '黑人牙膏',          10, 12.90,   129.00,  6,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/darlie-toothpaste.jpg',  1, 2, '2026-02-18 09:00:00', '2026-02-18 11:00:00', NULL, NULL, NULL),
(10025, 16, 2, 1004, '罗技无线鼠标',      1,  299.00,  299.00,  12, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/wireless-mouse.jpg',     1, 1, '2026-02-18 10:00:00', '2026-02-18 14:00:00', NULL, NULL, NULL),
-- === 未发货 (status=0) — 5笔 ===
(10026, 14, 2, 1003, 'AirPods Pro',        1,  1799.00, 1799.00, 10, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/airpods-pro.jpg',        0, 1, '2026-02-19 10:00:00', NULL, NULL, NULL, NULL),
(10027, 9,  4, 1012, '徕芬高速吹风机',    1,  399.00,  399.00,  1,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/laifen-hairdryer.jpg',   0, 3, '2026-02-19 15:30:00', NULL, NULL, NULL, NULL),
(10028, 10, 3, 1009, '康师傅红烧牛肉面',  12, 4.50,    54.00,   3,  'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/instant-noodles.jpg',    0, 2, '2026-02-20 08:00:00', NULL, NULL, NULL, NULL),
(10029, 15, 4, 1014, '小米蓝牙耳机',      2,  199.00,  398.00,  11, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/xiaomi-earbuds.jpg',     0, 3, '2026-02-20 09:00:00', NULL, NULL, NULL, NULL),
(10030, 16, 2, 1001, 'PS5游戏手柄',       1,  469.00,  469.00,  12, 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/ps5-controller.jpg',     0, 1, '2026-02-20 10:00:00', NULL, NULL, NULL, NULL);

-- ============================================================
-- 7. 配送批次 (12个)
-- ============================================================
INSERT INTO delivery_batches (id, driver_id, warehouse_id, status, created_at, started_at, completed_at, route_data, total_distance, total_duration, current_index) VALUES
-- 已完成 (status=2) — 无路线数据（历史批次）
(1,  5, 1, 2, '2026-02-02 08:00:00', '2026-02-02 08:30:00', '2026-02-04 16:00:00', NULL, NULL, NULL, 0),
(2,  6, 2, 2, '2026-02-02 08:30:00', '2026-02-02 09:00:00', '2026-02-05 14:00:00', NULL, NULL, NULL, 0),
(3,  7, 3, 2, '2026-02-03 08:00:00', '2026-02-03 08:30:00', '2026-02-06 15:30:00', NULL, NULL, NULL, 0),
(4,  5, 1, 2, '2026-02-05 08:00:00', '2026-02-06 08:00:00', '2026-02-08 11:00:00', NULL, NULL, NULL, 0),
(5,  6, 2, 2, '2026-02-04 08:00:00', '2026-02-05 08:00:00', '2026-02-07 10:00:00', NULL, NULL, NULL, 0),
(6,  7, 3, 2, '2026-02-06 08:00:00', '2026-02-07 08:00:00', '2026-02-09 10:00:00', NULL, NULL, NULL, 0),
(7,  5, 1, 2, '2026-02-08 08:00:00', '2026-02-08 08:30:00', '2026-02-10 11:00:00', NULL, NULL, NULL, 0),
(8,  5, 1, 2, '2026-02-14 08:00:00', '2026-02-14 08:30:00', '2026-02-15 18:00:00', NULL, NULL, NULL, 0),
(10, 7, 3, 2, '2026-02-14 09:00:00', '2026-02-14 09:30:00', '2026-02-15 20:00:00', NULL, NULL, NULL, 0),
-- 配送中 (status=1) — route_data由"开始运输"时调用腾讯地图API填充
(9,  6, 2, 1, '2026-02-14 08:30:00', '2026-02-14 09:00:00', NULL, NULL, NULL, NULL, 0),
-- 待出发 (status=0)
(12, 7, 3, 0, '2026-02-16 08:30:00', NULL, NULL, NULL, NULL, NULL, 0);

-- ============================================================
-- 8. 批次-订单关联
-- ============================================================
INSERT INTO delivery_batch_orders (batch_id, order_id, stop_sequence) VALUES
-- 已完成批次
(1, 10001, 1), (1, 10005, 2),
(2, 10002, 1), (2, 10004, 2),
(3, 10003, 1), (3, 10006, 2),
(4, 10008, 1),
(5, 10007, 1), (5, 10009, 2),
(6, 10010, 1),
(7, 10011, 1), (7, 10012, 2),
(8, 10016, 1), (8, 10015, 2),
(10, 10018, 1), (10, 10014, 2),
-- 配送中批次
(9,  10017, 1), (9,  10013, 2), (9, 10019, 3),
-- 待出发批次
(12, 10021, 1), (12, 10022, 2);

-- ============================================================
-- 9. 实时位置 (delivery_location)
-- 配送中批次的GPS轨迹由模拟服务自动生成，初始数据为空
-- ============================================================

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 数据统计
-- 用户: 16 (1管理员 + 3商户 + 4配送员 + 8消费者)
-- 库存: 15个商品
-- 商城: 15个上架商品
-- 订单: 30笔 (16已收货 + 1已到达 + 2运输中 + 2已揽收 + 1已发货 + 5未发货 + 3待揽收)
-- 批次: 11个 (9已完成 + 1配送中 + 1待出发)
-- 每个配送员同时只有1个活跃批次
-- 地址: 12个
-- 密码: 全部为 "123"
-- 配送员仓库: users.warehouse_id
-- 路线数据: delivery_batches.route_data（由"开始运输"时调用腾讯地图API填充）
-- ============================================================
