# 规范：路由守卫 + 角色权限控制

## 概述
保护所有路由，用户只能访问自己角色对应的页面。未授权访问自动跳转。

## 现状
- 路由的 `/demo` 有 `meta.requiresAuth`，但子路由没有角色限制
- `beforeEach` 守卫只检查 token 是否存在，不检查角色
- 顾客手动输入 `/admin/user-management` 可以直接进入
- `sessionStorage` 存储了：`token`、`userInfo`（JSON，含 id、username、role、token、warehouseId、warehouseName）

## 改动

### 1. 路由元信息 — 给每个子路由加 `roles`
```js
{ path: '/admin/user-management', component: UserManagement, meta: { roles: ['admin'] } }
{ path: '/general/mall', component: Mall, meta: { roles: ['merchant', 'consumer'] } }
// 其他路由同理 — 和 demo.vue 里 menuItems 的 roles 数组保持一致
```

### 2. 增强 `beforeEach` 守卫
```
如果路由需要认证 且 没有 token → 跳转到 /（登录页）
如果路由有 meta.roles 且 用户角色不在列表中 → 跳转到 /demo（首页）
如果已登录 且 访问 / → 跳转到 /demo
否则 → 放行
```

### 3. Axios 401 拦截器
在 `request.js` 响应拦截器中，如果后端返回 401：
- 清空 sessionStorage
- 通过 `router.push('/')` 跳转到登录页
- 显示 ElMessage.error('登录已过期，请重新登录')

### 4. Token 过期处理
前端不需要解析 JWT，让后端拒绝过期 token，前端收到 401 自动登出即可。

## 涉及文件
- `frontend/src/router/index.js` — 子路由加 meta.roles，增强 beforeEach
- `frontend/src/utils/request.js` — 加 401 拦截器，自动跳转登录

## 验收标准
- [ ] 未登录用户访问 `/admin/*`、`/merchant/*`、`/consumer/*`、`/driver/*` → 跳转到登录页
- [ ] 顾客访问 `/admin/user-management` → 跳转到 `/demo`（首页）
- [ ] 管理员访问 `/admin/data-analysis` → 正常进入
- [ ] 后端返回 401 → 自动登出 + 跳转登录页 + 错误提示
- [ ] 已登录用户访问登录页 → 跳转到 `/demo`
