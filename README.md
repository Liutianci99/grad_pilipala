# 🚚 电商物流管理系统 (E-commerce Logistics Management System)

> Built by Boss, improved by Pilipala 🦞🎆

基于 Spring Boot 3.2 + Vue 3 + MySQL 的全栈电商物流管理系统，支持多角色物流全流程管理。

## 技术栈

| 层级 | 技术 |
|------|------|
| **后端** | Spring Boot 3.2 · MyBatis Plus 3.5.5 · MySQL 8.0 · JWT |
| **前端** | Vue 3 · Vite 5 · Element Plus · Pinia |
| **部署** | Docker · Docker Compose · GitHub Actions CI/CD |
| **文档** | Springdoc OpenAPI 2.0.2 · ReDoc |

## 核心功能

### 🏪 商户端
- 商品入库 / 上架 / 下架管理
- 库存查询与管理
- 订单管理与物流查询

### 🛒 消费者端
- 商城浏览与在线购物
- 收货地址管理
- 订单跟踪与物流查询

### 🚗 配送员端
- 待取件 / 待配送任务
- 批量配送管理
- 配送路线规划（腾讯地图）
- 历史任务查看

### 👨‍💼 管理员端
- 用户管理
- 全平台订单管理
- 数据分析与统计

## 快速启动

### Docker 部署（推荐）

```bash
# 1. 克隆项目
git clone https://github.com/Liutianci99/grad_pilipala.git
cd grad_pilipala

# 2. 配置环境变量
cp .env.example .env
# 编辑 .env 填入你的配置

# 3. 启动
docker compose up -d

# 前端: http://localhost:80
# 后端: http://localhost:8080
# API文档: http://localhost:8080/api/redoc.html
```

### 本地开发

```bash
# 后端
cd backend
mvn spring-boot:run

# 前端
cd frontend
pnpm install && pnpm dev
```

## 项目结构

```
grad_pilipala/
├── .github/workflows/    # CI/CD 流水线
├── backend/              # Spring Boot 后端
│   ├── src/main/java/com/logistics/
│   │   ├── config/       # 配置（CORS、JWT、OpenAPI）
│   │   ├── controller/   # REST API 控制器
│   │   ├── dto/          # 数据传输对象
│   │   ├── entity/       # 数据库实体
│   │   ├── mapper/       # MyBatis 数据访问层
│   │   ├── service/      # 业务逻辑层
│   │   └── util/         # 工具类（JWT等）
│   └── Dockerfile
├── frontend/             # Vue 3 前端
│   ├── src/
│   │   ├── views/        # 页面组件（按角色分目录）
│   │   ├── router/       # 路由配置
│   │   └── utils/        # 工具函数
│   └── Dockerfile
├── database/
│   └── init.sql          # 数据库初始化脚本
├── docker-compose.yml    # Docker 编排
└── .env.example          # 环境变量模板
```

## CI/CD

推送到 `main` 分支自动触发：
1. ✅ 后端编译检查
2. ✅ 前端构建检查
3. 🚀 自动部署到服务器

## 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | 登录后修改 |
| 商户 | merchant | 登录后修改 |
| 配送员 | driver | 登录后修改 |
| 消费者 | consumer | 登录后修改 |

## License

MIT

---

*🦞 Lobster on the outside, fireworks on the inside 🎆*
