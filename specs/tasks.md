# 任务清单

## 用户注册功能

### 后端

- [ ] T020 [P] UserService: 添加 register 方法（校验用户名重复、BCrypt 加密、自动登录返回 token）
- [ ] T021 [P] AuthController: 添加 POST /auth/register 端点
- [ ] T022 [P] WebMvcConfig: JWT 拦截器排除 /auth/register

### 前端

- [ ] T023 [P] Register.vue: 新建注册页面（用户名、密码、确认密码、角色选择，不含管理员）
- [ ] T024 [P] Login.vue: 添加"注册"按钮（与登录按钮并排，跳转 /register）
- [ ] T025 [P] router: 添加 /register 路由
- [ ] T026 前端 build 验证
- [ ] T027 提交并推送到 main

---

## 代码重构 — 匹配 specs 规范（已完成 ✅）

### 后端

- [ ] T001 [P] OrderController: 移除所有 try-catch，参数校验改为抛 BusinessException
- [ ] T002 [P] DeliveryBatchController: 移除所有 try-catch，业务逻辑抽到 Service 层
- [ ] T003 [P] AddressController: 移除 System.out.println 调试代码
- [ ] T004 [P] AuthController: 移除 System.out.println，返回 Result<T> 统一格式
- [ ] T005 [P] AdminController: 添加 @RequiredArgsConstructor 替换 @Autowired，添加 Swagger 注解

### 前端

- [ ] T006 [P] admin/OrderManagement: 检查 alert→ElMessage，统一 design.css 类
- [ ] T007 [P] admin/UserManagement: 检查 alert→ElMessage，统一 design.css 类
- [ ] T008 [P] delivery/DeliveryBatch: 清理 console.log，统一样式
- [ ] T009 [P] delivery/DeliveryBatchDetail: 清理 console.log，统一样式
- [ ] T010 [P] delivery/PendingPickup: 清理 console.log，统一样式
- [ ] T011 [P] delivery/PendingDelivery: 清理 console.log，统一样式
- [ ] T012 [P] delivery/HistoryTasks: 清理 console.log，统一样式
- [ ] T013 [P] merchant/InventoryManagement: 清理 console.log，统一样式
- [ ] T014 [P] merchant/OrderManagement: 清理 console.log，统一样式
- [ ] T015 [P] merchant/ProductListing: 清理 console.log，统一样式
- [ ] T016 [P] merchant/ProductDelisting: 统一样式
- [ ] T017 [P] merchant/StockIn: 清理 console.log，统一样式

### 构建验证

- [ ] T018 前端 build 验证
- [ ] T019 提交并推送
