# 电商物流管理系统

Spring Boot 3.2 + Vue 3 + MySQL 全栈物流管理系统，支持管理员、商户、消费者、配送员四种角色。

## 技术栈

| 层 | 技术 |
|---|------|
| 后端 | Spring Boot 3.2 · MyBatis-Plus · JWT · MySQL 8.0 |
| 前端 | Vue 3 · Vite · Element Plus · Lucide Icons |
| 部署 | Docker Compose · GitHub Actions → GHCR |
| 地图 | 腾讯地图 JavaScript API |

## 业务流程图

```mermaid
%%{init: {'theme':'base', 'themeVariables': {'primaryColor':'#f0f0f0', 'edgeLabelBackground':'#ffffff'}}}%%
flowchart TB
%% === 管理员：监控全局 ===
    subgraph AdminFlow["⚙️ 管理员 — 全局监控"]
        direction LR
        UserMgmt[用户管理] ~~~ OrderMgmt[订单管理] ~~~ DataAnalysis[数据分析]
    end

%% === 核心业务流程 ===
    subgraph BusinessFlow[" "]
        direction TB

        subgraph MerchantFlow["🏪 商户"]
            direction LR
            StockIn[商品入库] --> Publish[商城上架] --> ReceiveOrder[收到订单] --> ShipOrder[确认发货]
        end

        subgraph ConsumerFlow["🛒 消费者"]
            direction LR
            BrowseMall[浏览商城] --> PlaceOrder[下单] --> TrackLogistics[查看物流] --> ConfirmReceive([确认收货])
        end

        subgraph DriverFlow["🚛 配送员"]
            direction LR
            Pickup[揽收] --> CreateBatch[创建批次] --> StartDelivery[开始运输] --> OnRoute[沿路线配送] --> CompleteBatch([完成批次])
        end

        Publish -.->|商品展示| BrowseMall
        PlaceOrder -.->|生成订单| ReceiveOrder
        ShipOrder -.->|订单流转| Pickup
        OnRoute -.->|实时GPS| TrackLogistics
        CompleteBatch -.->|已送达| ConfirmReceive
    end

    style AdminFlow fill:#F3E5F5,stroke:#7B1FA2,stroke-width:2px,color:#4A148C
    style BusinessFlow fill:none,stroke:none
    style MerchantFlow fill:#FFF8E1,stroke:#F57C00,stroke-width:2px,color:#E65100
    style ConsumerFlow fill:#E8F5E9,stroke:#388E3C,stroke-width:2px,color:#1B5E20
    style DriverFlow fill:#E3F2FD,stroke:#1976D2,stroke-width:2px,color:#0D47A1

    classDef default fill:#ffffff,stroke:#999999,color:#333333,stroke-width:1px
    classDef done fill:#4CAF50,stroke:#2E7D32,color:#FFFFFF,stroke-width:2px
    class ConfirmReceive,CompleteBatch done
```


## 功能概览

### 管理员
- 用户管理（CRUD、角色筛选、搜索）
- 订单管理（状态修改、搜索、删除）
- 数据分析（概览卡片、角色分布、订单趋势图）

### 商户
- 商城浏览、商品上架/下架/入库
- 库存管理（搜索、编辑、删除）
- 订单管理（确认、发货）
- 物流查询（地图路线、时间线、实时追踪）

### 消费者
- 商城购物（选商品、选地址、下单）
- 我的订单（状态筛选、搜索）
- 地址管理（CRUD、默认地址）
- 物流查询

### 配送员
- 待揽收 / 待送货
- 创建配送批次（多单合并）
- 批次详情与完成配送
- 历史任务

## 在线访问

🔗 **http://121.5.23.149:8888**

### 测试账号

| 角色 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| 管理员 | 系统管理员 | 123 | 用户管理、订单管理、数据分析 |
| 商户 | 京东自营 | 123 | 库存管理、商品上架、订单发货、物流查询 |
| 配送员 | 张伟 | 123 | 揽收、创建批次、运输、历史任务 |
| 消费者 | 刘天赐 | 123 | 商城购物、我的订单、地址管理、物流查询 |

## 项目结构

```
grad_pilipala/
├── .github/workflows/ci-cd.yml   # CI/CD（push main → GHCR → 服务器部署）
├── backend/
│   └── src/main/java/com/logistics/
│       ├── controller/            # 8 个 REST 控制器
│       ├── entity/                # 数据库实体
│       ├── mapper/                # MyBatis 数据访问
│       ├── service/               # 业务逻辑
│       ├── config/                # CORS、JWT、OpenAPI
│       └── util/                  # JWT 工具类
├── frontend/
│   └── src/
│       ├── views/                 # 页面（按角色分目录）
│       │   ├── admin/             # 用户管理、订单管理、数据分析
│       │   ├── merchant/          # 库存、订单、上架/下架/入库、物流
│       │   ├── consumer/          # 我的订单、地址、物流
│       │   ├── delivery/          # 揽收、配送、批次、历史
│       │   └── general/           # 商城
│       ├── assets/design.css      # 全局设计系统
│       ├── router/                # 路由配置
│       └── utils/request.js       # Axios 封装 + JWT 拦截
├── database/init.sql              # 建表 + 假数据
├── specs/                         # 项目规范
│   ├── constitution.md            # 项目宪法
│   ├── requirements.md            # 功能需求（按角色划分）
│   ├── rule.md                    # 编码规范
│   ├── api.md                     # 后端接口规范
│   └── frontend_design.md         # 前端设计规范
├── docker-compose.yml
└── .env.example
```

## CI/CD

推送到 `main` 分支自动触发：
1. 构建后端/前端 Docker 镜像
2. 推送到 GitHub Container Registry (GHCR)
3. SSH 到服务器拉取新镜像并重启

## 开发规范

详见 `specs/` 目录。新功能开发流程：
1. 更新 `specs/requirements.md`
2. 按 `rule.md` + `api.md` + `frontend_design.md` 编码
3. 功能分支 `feat/xxx` → PR → 合并到 `main`

## License

MIT
