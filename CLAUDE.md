# CLAUDE.md — 项目备忘

## 已知 Bug

### BUG-001: 下单时未设置 warehouse_id（严重）
- **位置**: `OrderServiceImpl.createOrder()`
- **现象**: 订单的 `warehouse_id` 为 null，配送员按仓库筛选待揽收订单时漏单
- **状态**: ✅ 已修复

### BUG-002: 下单时未扣减商城库存（严重）
- **位置**: `OrderServiceImpl.createOrder()`
- **现象**: `mall.available_quantity` 只检查不扣减，可超卖
- **状态**: ✅ 已修复

### BUG-003: driverId 参数类型不一致
- **位置**: `OrderController` / `OrderService` / `OrderServiceImpl`
- **现象**: `driverId` 用 `Long`，其他用户 ID（customerId, merchantId）用 `Integer`，且 `DeliveryBatch.driverId` 也是 `Integer`
- **状态**: ✅ 已修复

### BUG-004: init.sql 严重过时
- **位置**: `database/init.sql`
- **现象**: 还有 `delivery_route`、`delivery_personnel` 表，字段名不匹配实体（`user_id` vs `customer_id`，`total_price` vs `total_amount` 等）
- **状态**: ✅ 已修复

## 修复记录

### 2026-02-21: fix/order-create-bugs
- BUG-001: `createOrder()` 添加 `order.setWarehouseId(mall.getWarehouseId())`
- BUG-002: `createOrder()` 插入订单后扣减 `mall.available_quantity`，方法已有 `@Transactional` 保证原子性

### 2026-02-21: fix/backend-consistency
- BUG-003: `Long driverId` → `Integer driverId`（Controller + Service + ServiceImpl），去掉 `.intValue()` 转换
- BUG-004: 重写 `init.sql`，删除 `delivery_route`/`delivery_personnel`，所有表结构匹配当前实体定义
