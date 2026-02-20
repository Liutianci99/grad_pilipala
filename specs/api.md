# 后端接口规范

## 基础信息
- 基础路径：`/api`（前端 Nginx 代理到后端 8080）
- 认证：JWT Bearer Token（`Authorization: Bearer xxx`）
- 内容类型：`application/json`

## 统一响应格式

```json
{
    "code": 200,
    "message": "success",
    "data": {}
}
```

| code | 含义 |
|------|------|
| 200 | 成功 |
| 500 | 业务错误（message 里有具体原因） |
| 401 | 未认证 / Token 过期 |

## 现有接口一览

### 认证 `/auth`
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/auth/login` | 登录（username + password + role → token） |
| POST | `/auth/register` | 注册（username + password + role → token，自动登录） |

### 管理员 `/admin`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/users` | 用户列表（?role=&search=） |
| POST | `/admin/users` | 创建用户 |
| PUT | `/admin/users/{id}` | 编辑用户 |
| DELETE | `/admin/users/{id}` | 删除用户 |
| GET | `/admin/orders` | 所有订单（?status=&search=） |
| PUT | `/admin/orders/{id}/status` | 修改订单状态 |
| DELETE | `/admin/orders/{id}` | 删除订单 |
| GET | `/admin/stats/overview` | 数据概览（用户数、订单数、营收等） |
| GET | `/admin/stats/daily-orders` | 每日订单趋势（?days=7） |

### 订单 `/orders`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/orders/my` | 我的订单（消费者） |
| GET | `/orders/merchant` | 商户订单 |
| POST | `/orders/create` | 创建订单 |
| PUT | `/orders/{id}/confirm` | 确认订单 |
| PUT | `/orders/{id}/ship` | 发货 |
| PUT | `/orders/{id}/pickup` | 揽收 |
| GET | `/orders/pending-pickup` | 待揽收列表 |
| GET | `/orders/pending-delivery` | 待配送列表 |
| POST | `/orders/delivery-batch` | 创建配送批次 |
| GET | `/orders/delivery-batches` | 配送批次列表 |
| POST | `/orders/delivery-complete` | 完成配送 |
| GET | `/orders/completed-batches` | 已完成批次 |

### 商城 `/mall`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/mall/products` | 商品列表 |

### 仓库 `/warehouse`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/warehouse/list` | 仓库列表 |
| GET | `/warehouse/{id}` | 仓库详情 |

### 地址 `/address`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/address/my` | 我的地址列表 |
| POST | `/address/add` | 添加地址 |
| PUT | `/address/update` | 更新地址 |
| DELETE | `/address/{id}` | 删除地址 |

### 库存 `/inventory`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/inventory/list` | 库存列表 |
| POST | `/inventory/add` | 添加商品 |
| PUT | `/inventory/update` | 更新商品 |

## 新增接口规范

新增接口时遵循以下约定：
1. URL 用小写 + 短横线（`/delivery-batch`，不用驼峰）
2. 列表接口支持 `?search=` 模糊搜索
3. 状态筛选用 `?status=` 数字
4. 返回格式必须是 `Result<T>`
5. 错误抛 `BusinessException`，不在 Controller 里 try-catch
