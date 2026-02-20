# 规范：路由守卫 + 角色权限控制

## 概述
保护所有路由，用户只能访问自己角色对应的页面。未授权访问自动跳转。

## 现状分析
- 路由 `/demo` 有 `meta.requiresAuth`，但子路由没有角色限制
- `beforeEach` 守卫只检查 token 是否存在，不检查角色
- 顾客手动输入 `/admin/user-management` 可以直接进入（安全漏洞）
- `sessionStorage` 存储：`token`、`userInfo`（JSON，含 id、username、role、token、warehouseId、warehouseName）

## 改动详情

### 1. 路由元信息 — 给每个子路由加 `meta.roles`

```js
// router/index.js — 子路由示例
{ path: '/admin/user-management', component: UserManagement, meta: { roles: ['admin'] } },
{ path: '/admin/order-management', component: AdminOrderManagement, meta: { roles: ['admin'] } },
{ path: '/admin/data-analysis', component: DataAnalysis, meta: { roles: ['admin'] } },
{ path: '/merchant/inventory-management', component: InventoryManagement, meta: { roles: ['merchant'] } },
{ path: '/general/mall', component: Mall, meta: { roles: ['merchant', 'consumer'] } },
{ path: '/consumer/my-orders', component: MyOrders, meta: { roles: ['consumer'] } },
{ path: '/driver/pending-pickup', component: PendingPickup, meta: { roles: ['driver'] } },
// ... 其他路由同理，和 demo.vue 里 menuItems 的 roles 数组保持一致
```

### 2. 增强 `beforeEach` 守卫

```js
router.beforeEach((to, from, next) => {
    const token = sessionStorage.getItem('token')
    const isLoggedIn = !!token

    // 未登录 → 跳转登录页
    if (to.meta.requiresAuth !== false && !isLoggedIn) {
        return next({ name: 'login' })
    }

    // 已登录访问登录页 → 跳转首页
    if (to.name === 'login' && isLoggedIn) {
        return next({ name: 'home' })
    }

    // 角色检查
    if (to.meta.roles && isLoggedIn) {
        const userInfo = JSON.parse(sessionStorage.getItem('userInfo') || '{}')
        if (!to.meta.roles.includes(userInfo.role)) {
            // 无权限 → 跳转首页（不是登录页）
            return next({ name: 'home' })
        }
    }

    next()
})
```

### 3. Axios 401 拦截器

在 `request.js` 响应拦截器中增加 401 处理：

```js
// request.js — 响应拦截器
import router from '@/router'

request.interceptors.response.use(
    response => response.data,
    error => {
        if (error.response?.status === 401) {
            sessionStorage.clear()
            ElMessage.error('登录已过期，请重新登录')
            router.push('/')
            return Promise.reject(error)
        }
        // ... 其他错误处理
    }
)
```

### 4. Token 过期处理
- 前端不解析 JWT，让后端拒绝过期 token
- 前端收到 401 → 自动清除 session → 跳转登录页
- 无需前端定时器或 JWT decode 库

## 涉及文件
| 文件 | 改动 |
|------|------|
| `frontend/src/router/index.js` | 子路由加 `meta.roles`，增强 `beforeEach` |
| `frontend/src/utils/request.js` | 加 401 拦截器，自动跳转登录 |

## 验收标准
- [ ] 未登录用户访问 `/admin/*`、`/merchant/*`、`/consumer/*`、`/driver/*` → 跳转到登录页
- [ ] 顾客访问 `/admin/user-management` → 跳转到 `/demo`（首页），不是登录页
- [ ] 管理员访问 `/admin/data-analysis` → 正常进入
- [ ] 商户访问 `/consumer/my-orders` → 跳转到 `/demo`
- [ ] 后端返回 401 → 自动清除 sessionStorage + 跳转登录页 + ElMessage 错误提示
- [ ] 已登录用户访问 `/`（登录页）→ 跳转到 `/demo`
- [ ] 刷新页面后守卫仍然生效（token 从 sessionStorage 读取）
