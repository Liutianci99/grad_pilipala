# Spec: Route Guards + Role-Based Access Control

## Summary
Protect all routes so users can only access pages matching their role. Redirect unauthorized access gracefully.

## Current State
- Router has `meta.requiresAuth` on `/demo` but child routes have no role metadata
- `beforeEach` guard only checks if token exists — doesn't check role
- If a consumer manually types `/admin/user-management`, they get in
- `sessionStorage` stores: `token`, `userInfo` (JSON with id, username, role, token, warehouseId, warehouseName)

## Changes

### 1. Route metadata — add `roles` to each child route
```js
{ path: '/admin/user-management', component: UserManagement, meta: { roles: ['admin'] } }
{ path: '/general/mall', component: Mall, meta: { roles: ['merchant', 'consumer'] } }
// etc — match the roles array already defined in demo.vue menuItems
```

### 2. Enhanced `beforeEach` guard
```
if route requires auth AND no token → redirect to /
if route has meta.roles AND user role not in list → redirect to /demo (home)
if logged in AND visiting / → redirect to /demo
otherwise → allow
```

### 3. Axios 401 interceptor
In `request.js` response interceptor, if backend returns 401:
- Clear sessionStorage
- Redirect to login via `router.push('/')`
- Show ElMessage.error('登录已过期，请重新登录')

### 4. Token expiry handling
Currently no expiry check on frontend. Add:
- On 401 response → auto-logout (above)
- No need for frontend JWT decode — let backend reject expired tokens

## Files Changed
- `frontend/src/router/index.js` — add meta.roles to child routes, enhance beforeEach
- `frontend/src/utils/request.js` — add 401 interceptor with auto-redirect

## Acceptance Criteria
- [ ] Unauthenticated user visiting any `/admin/*`, `/merchant/*`, `/consumer/*`, `/driver/*` route → redirected to login
- [ ] Consumer visiting `/admin/user-management` → redirected to `/demo` (their home)
- [ ] Admin visiting `/admin/data-analysis` → allowed
- [ ] 401 from backend → auto-logout + redirect to login + error message
- [ ] Login page when already logged in → redirect to `/demo`
