# 重构日志 — 2026-02-22

## 背景
Boss 要求重构 API 和数据库，让业务流程更稳定、更正确。

---

## 问题诊断

### 1. 地图不显示路线和配送员标记
**症状：** 物流查询页面地图空白，只有底图，没有路线线条和货车图标。

**根因：** 腾讯地图 API 返回的 polyline 是差值压缩格式：
```
[39.793145, 116.528082, -5920, -12241, -329, -801, ...]
```
只有第一个点是绝对坐标，后续都是相对前一个点的差值（÷1e6）。前端把每对数字当绝对坐标用，导致除第一个点外全部画到了 (0,0) 附近。

**修复思路：**
- 方案A：前端解码（已实现但后来移除）
- 方案B：后端解码后发送扁平数组（最终方案，单一数据源）

### 2. 订单列表缺少地址数据
**症状：** 商户/消费者物流查询页面，目的地标记不显示。

**根因：** `getCustomerOrders()` 和 `getMerchantOrders()` 只做了 `selectList`，没有填充 `address` 字段。Order 实体的 `address` 是 `@TableField(exist = false)` 非数据库字段，需要手动查询填充。

**修复：** 新增 `populateAddresses()` 私有方法，遍历订单列表，根据 `addressId` 查询地址并设置。

### 3. 完成配送不记录送达时间
**症状：** 批次完成后，订单 `delivery_time` 为 NULL。

**根因：** `completeDelivery()` 方法只更新了 `status=4`，没有设置 `delivery_time`。

**修复：** 添加 `order.setDeliveryTime(now)`。

### 4. 逆地理编码浪费 API 配额
**症状：** 模拟配送时，每10个路径点调用一次腾讯地图逆地理编码 API。一条路线几千个点，会产生几百次 API 调用。

**修复：** 移除逆地理编码调用，改为每50个点记录一次位置（只存坐标，不存地址文本）。

### 5. 缺少事务保护
**症状：** `startBatch` 和 `completeBatch` 更新多张表（delivery_batches + orders），但没有 `@Transactional`。如果中途失败，数据会不一致。

**修复：** 添加 `@Transactional(rollbackFor = Exception.class)`。

### 6. 一批次限制放错位置
**症状：** `createDeliveryBatch` 检查配送员是否有活跃批次，但 spec 说配送员可以创建多个批次，只是同时只能配送一个。

**修复：** 从 `createDeliveryBatch` 移除检查，只保留在 `start-batch` 中。

---

## 重构分支
- 分支名：`refactor/business-flow-stability`
- 合并方式：`--no-ff`（保留合并记录）

## 修改文件
| 文件 | 改动 |
|------|------|
| DeliveryBatchController.java | 简化 track-by-order（移除旧格式代码，后端解压 polyline），添加 @Transactional |
| OrderServiceImpl.java | completeDelivery 设置 delivery_time，移除 createBatch 的一批次检查，添加 populateAddresses |
| DeliverySimulationService.java | 移除逆地理编码，降低位置记录频率 |
| merchant/LogisticsQuery.vue | 移除前端 polyline 解码函数 |
| consumer/LogisticsQuery.vue | 移除前端 polyline 解码函数 |

## 净效果
- 5 files changed, 33 insertions(+), 78 deletions(-)
- 代码更少，逻辑更清晰

---

## CI 失败记录

### 编译错误：`cannot find symbol: method setDeliveryTime`
**时间：** 2026-02-22 13:48

**错误信息：**
```
OrderServiceImpl.java:[348,22] cannot find symbol
  symbol:   method setDeliveryTime(java.time.LocalDateTime)
  location: variable order of type com.logistics.entity.Order
```

**分析：** Order 实体有 `private LocalDateTime deliveryTime` 字段，且使用了 `@Data`（Lombok），理论上应该自动生成 setter。同文件其他位置（line 348 `updateOrdersToInTransit`）也调用了 `setDeliveryTime` 且之前能编译通过。

**可能原因：**
1. Docker 构建缓存导致旧 class 文件干扰
2. Lombok 注解处理器在 CI 环境中的版本差异
3. 需要检查 pom.xml 中 Lombok 依赖配置

**状态：** ✅ 已修复并部署

**真正原因：** 重构时添加 `@Override`、`@Transactional`、`@Operation` 注解时，没注意到这些注解已经存在，导致重复注解。Java 不允许非 `@Repeatable` 注解重复使用。

**修复：** 删除重复的注解（花了3次提交才完全清理干净）。

**教训：** 编辑代码时，先检查目标位置是否已有相同注解，避免重复添加。

---

## 最终结果

### 部署状态
- CI/CD: ✅ 通过（第5次尝试）
- 前后端: ✅ 已重新部署
- 验证: ✅ polyline 现在是扁平坐标数组（不再是 delta 编码）

### 代码变更统计
- 5 files changed, 33 insertions(+), 78 deletions(-)
- 净减少 45 行代码 — 更简洁

### 提交记录
1. `1f1f282` - refactor: business flow stability improvements
2. `ba3dfd2` - Merge branch 'refactor/business-flow-stability' into main (--no-ff)
3. `24998f9` - docs: add refactor log
4. `7a22948` - ci: add no-cache to backend build (误判，实际是注解问题)
5. `a0a97ca` - fix: remove duplicate annotations causing compilation errors
6. `8a71e68` - fix: remove remaining duplicate @Override annotation ✅

### API 验证
```bash
# Order 10020 polyline 测试
polyline length: 7918
first 6 values: [39.793145, 116.528082, 39.787225, 116.515841, 39.786896, 116.51504]
all valid coords (no deltas): True ✅
currentLat=35.5211030000001, progress=35.0%
```

前端不再需要解码 polyline — 后端直接发送可用的坐标数组。
