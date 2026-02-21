# 后端规范（数据库 + 接口 + 编码）

## 数据库

### 表结构

#### users — 用户表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT 主键 | 用户ID |
| username | VARCHAR(20) | 用户名 |
| password | VARCHAR(100) | BCrypt加密密码 |
| role | ENUM | merchant / driver / consumer / admin |
| warehouse_id | INT 外键 → warehouse | 配送员所属仓库（仅driver有值） |

#### warehouse — 仓库表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT 主键 | 仓库ID |
| name | VARCHAR(100) | 仓库名称 |
| city | VARCHAR(50) | 所在城市 |
| address | VARCHAR(255) | 详细地址 |
| longitude | DECIMAL(10,6) | 经度 |
| latitude | DECIMAL(10,6) | 纬度 |

#### inventory — 库存表
| 字段 | 类型 | 说明 |
|------|------|------|
| product_id | INT 主键 | 商品ID |
| user_id | INT 外键 → users | 商户ID |
| product_name | VARCHAR(100) | 商品名称 |
| description | TEXT | 商品描述 |
| quantity | INT | 库存数量 |
| image_url | VARCHAR(255) | 商品图片URL |
| is_published | TINYINT | 是否已上架 |
| warehouse_id | INT 外键 → warehouse | 所属仓库 |

#### mall — 商城表
| 字段 | 类型 | 说明 |
|------|------|------|
| product_id | INT 主键 外键 → inventory | 商品ID |
| merchant_id | INT 外键 → users | 商户ID |
| product_name | VARCHAR(100) | 商品名称 |
| description | TEXT | 商品描述 |
| available_quantity | INT | 可售数量 |
| price | DECIMAL(10,2) | 售价 |
| is_published | TINYINT | 是否上架中 |
| image_url | VARCHAR(255) | 商品图片 |
| warehouse_id | INT 外键 → warehouse | 所属仓库 |

#### address — 收货地址表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT 主键 | 地址ID |
| user_id | INT 外键 → users | 消费者ID |
| receiver_name | VARCHAR(50) | 收货人 |
| receiver_phone | VARCHAR(20) | 电话 |
| province / city / district | VARCHAR(50) | 省市区 |
| detail_address | VARCHAR(255) | 详细地址 |
| latitude | DECIMAL(10,7) | 纬度 |
| longitude | DECIMAL(10,7) | 经度 |
| is_default | TINYINT | 是否默认地址 |

#### orders — 订单表
| 字段 | 类型 | 说明 |
|------|------|------|
| order_id | INT 主键 | 订单ID |
| product_id | INT 外键 → mall | 商品ID |
| customer_id | INT 外键 → users | 消费者ID |
| merchant_id | INT 外键 → users | 商户ID |
| address_id | INT 外键 → address | 收货地址ID |
| product_name | VARCHAR(100) | 商品名称（冗余，方便查询） |
| quantity | INT | 数量 |
| unit_price | DECIMAL(10,2) | 单价 |
| total_amount | DECIMAL(10,2) | 总金额 |
| image_url | VARCHAR(255) | 商品图片 |
| status | INT | 订单状态（见状态机） |
| warehouse_id | INT 外键 → warehouse | 发货仓库 |
| order_time | DATETIME | 下单时间 |
| ship_time | DATETIME | 发货时间 |
| pickup_time | DATETIME | 揽收时间 |
| delivery_time | DATETIME | 送达时间 |
| receive_time | DATETIME | 收货时间 |

#### delivery_batches — 运输批次表（含路线数据）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT 主键 | 批次ID |
| driver_id | INT 外键 → users | 配送员用户ID |
| warehouse_id | INT 外键 → warehouse | 起始仓库 |
| status | TINYINT | 0=待出发, 1=配送中, 2=已完成 |
| route_data | LONGTEXT | 腾讯地图压缩polyline（JSON数组） |
| total_distance | INT | 总距离（米） |
| total_duration | INT | 总时长（秒） |
| current_index | INT 默认0 | 模拟当前位置索引 |
| created_at | DATETIME | 创建时间 |
| started_at | DATETIME | 出发时间 |
| completed_at | DATETIME | 完成时间 |

#### delivery_batch_orders — 批次订单关联表
| 字段 | 类型 | 说明 |
|------|------|------|
| batch_id | INT 外键 → delivery_batches | 批次ID |
| order_id | INT 外键 → orders | 订单ID |
| stop_sequence | TINYINT | 停靠顺序 |
| 主键 | (batch_id, order_id) | 联合主键 |

#### delivery_location — 配送位置记录表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT 主键 | 记录ID |
| batch_id | INT 外键 → delivery_batches | 批次ID |
| latitude | DECIMAL(10,7) | 纬度 |
| longitude | DECIMAL(10,7) | 经度 |
| address | VARCHAR(500) | 逆地理编码地址 |
| path_index | INT | 路径点索引 |
| recorded_at | DATETIME | 记录时间 |

### 表关系

```
users ──┬── inventory (user_id = 商户)
        ├── mall (merchant_id = 商户)
        ├── orders (customer_id / merchant_id)
        ├── address (user_id = 消费者)
        └── delivery_batches (driver_id = 配送员)
                ├── delivery_batch_orders → orders
                └── delivery_location (GPS轨迹)

warehouse ── users.warehouse_id (配送员所属仓库)
```

### 已删除的表
- ~~delivery_personnel~~ → warehouse_id 移到 users 表
- ~~delivery_route~~ → 合并到 delivery_batches

---

## 订单状态机

```
未发货(0) → 已发货(1) → 已揽收(2) → 运输中(3) → 已到达(4) → 已收货(5)
```

| 状态 | 值 | 触发角色 | 后端动作 |
|------|---|---------|---------|
| 未发货 | 0 | 消费者下单 | POST /orders/create |
| 已发货 | 1 | 商家发货 | PUT /orders/{id}/ship |
| 已揽收 | 2 | 配送员揽收 | PUT /orders/{id}/pickup |
| 运输中 | 3 | 配送员开始运输 | POST /delivery-batch/start，批量更新订单 |
| 已到达 | 4 | 配送员完成配送 | POST /delivery-batch/complete，批量更新订单 |
| 已收货 | 5 | 消费者确认收货 | PUT /orders/{id}/confirm |

---

## 腾讯地图 API

- 密钥：环境变量 `TENCENT_MAP_API_KEY`
- 驾车路线规划：`https://apis.map.qq.com/ws/direction/v1/driving/`
  - 参数：`from=纬度,经度&to=纬度,经度&waypoints=纬1,经1;纬2,经2&key=xxx`
  - 返回：`result.routes[0].polyline`（压缩坐标数组）、`distance`（米）、`duration`（秒）
- 逆地理编码：`https://apis.map.qq.com/ws/geocoder/v1/`
  - 参数：`location=纬度,经度&key=xxx`
  - 返回：`result.address`

### polyline 解压算法
腾讯地图返回的 polyline 是差值压缩的 JSON 数组 `[纬度0, 经度0, 差值纬度1, 差值经度1, ...]`：
```
for i in range(2, len(arr)):
    arr[i] = arr[i-2] + arr[i] / 1000000
坐标点 = [(arr[i], arr[i+1]) for i in range(0, len(arr), 2)]
```

### 配送模拟流程
1. 配送员点击"开始运输"→ 后端调用 `planRoute` 获取路线
2. 将 polyline、distance、duration 存入 `delivery_batches`
3. 异步线程以恒定速度遍历解压后的坐标点，更新 `current_index`
4. 每 N 个点记录一次 `delivery_location`（减少数据库压力）
5. 前端轮询 `current_index` 获取配送员实时位置

---

## 接口列表

### 基础信息
- 基础路径：`/api`
- 认证：JWT Bearer Token（`Authorization: Bearer xxx`）
- 统一响应：`{ code: 200, message: "success", data: {} }`
- 错误码：200=成功，500=业务错误，401=未认证

### 认证 `/auth`
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /auth/login | 登录（username + password + role → token） |
| POST | /auth/register | 注册（不允许注册admin） |

### 管理员 `/admin`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /admin/users | 用户列表（?role=&search=） |
| POST | /admin/users | 创建用户 |
| PUT | /admin/users/{id} | 编辑用户 |
| DELETE | /admin/users/{id} | 删除用户 |
| GET | /admin/orders | 订单列表（?status=&search=） |
| PUT | /admin/orders/{id}/status | 修改订单状态 |
| DELETE | /admin/orders/{id} | 删除订单 |
| GET | /admin/stats/overview | 数据概览 |
| GET | /admin/stats/daily-orders | 每日订单趋势 |

### 订单 `/orders`
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /orders/create | 创建订单 |
| GET | /orders/my | 我的订单（消费者，?customerId=） |
| GET | /orders/merchant | 商户订单（?merchantId=） |
| PUT | /orders/{id}/ship | 商家发货（0→1） |
| PUT | /orders/{id}/pickup | 配送员揽收（1→2） |
| PUT | /orders/{id}/confirm | 消费者确认收货（4→5） |
| GET | /orders/pending-pickup | 待揽收列表（status=1） |
| GET | /orders/pending-delivery | 待配送列表（status=2） |
| POST | /orders/delivery-batch | 创建配送批次 |

### 配送 `/delivery-batch`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /delivery-batch/list | 配送员的批次列表（?driverId=&status=） |
| POST | /delivery-batch/start | 开始运输（调腾讯地图API规划路线） |
| POST | /delivery-batch/complete | 完成配送 |
| GET | /delivery-batch/detail | 批次详情（?batchId=） |
| GET | /delivery-batch/location | 实时位置（?batchId=） |
| GET | /delivery-batch/track | 订单物流追踪（?orderId=） |

### 商城 `/mall`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /mall/products | 商品列表 |
| POST | /mall/publish | 商品上架 |

### 库存 `/inventory`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /inventory/list | 库存列表（?userId=） |
| POST | /inventory/stock-in | 商品入库（multipart，含图片） |

### 仓库 `/warehouse`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /warehouse/list | 仓库列表 |
| GET | /warehouse/{id} | 仓库详情 |

### 地址 `/address`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /address/list | 地址列表（?userId=） |
| POST | /address | 添加地址 |
| PUT | /address/{id} | 更新地址 |
| DELETE | /address/{id} | 删除地址 |
| PUT | /address/{id}/default | 设为默认 |

---

## 编码规范

### Controller 层
- 不写 try-catch，由 `GlobalExceptionHandler` 统一处理
- 业务错误抛 `BusinessException`
- RESTful 风格：GET 查询 / POST 创建 / PUT 更新 / DELETE 删除
- URL 用小写 + 短横线（如 `/delivery-batch`）

### Service 层
- 接口 + 实现分离（`OrderService` + `OrderServiceImpl`）
- 写操作加 `@Transactional`
- 外部API调用封装在 `TencentMapService`

### 命名规范
| 操作 | 方法名 | 示例 |
|------|--------|------|
| 查询列表 | listXXX | listUsers |
| 查询详情 | getXXX | getOrderDetail |
| 创建 | createXXX | createBatch |
| 更新 | updateXXX | updateStatus |
| 删除 | deleteXXX | deleteUser |

### 数据库操作
- 实体类：`@Data` + `@TableName`
- 非数据库字段：`@TableField(exist = false)`
- 简单查询：`LambdaQueryWrapper`
- 复杂查询：`@Select` 注解

### 安全
- 密码：BCrypt 加密，不可逆
- 敏感配置：环境变量，不硬编码
- 异常：不泄露堆栈信息给前端
