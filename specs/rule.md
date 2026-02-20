# 编码规范

## 后端（Spring Boot 3.2 + MyBatis-Plus）

### 统一返回格式
- 所有接口返回 `Result<T>`：`{ code, message, data }`
- 成功：`Result.success(data)`
- 失败：`Result.error("错误信息")`

### Controller 规范
- 不写 try-catch，由 `GlobalExceptionHandler` 统一处理
- 业务错误抛 `BusinessException`
- RESTful 风格：GET 查询、POST 创建、PUT 更新、DELETE 删除

### 命名规范

| 操作 | 方法名 | 示例 |
|------|--------|------|
| 查询列表 | `listXXX` | `listUsers` |
| 查询详情 | `getXXX` | `getOrderDetail` |
| 创建 | `createXXX` | `createUser` |
| 更新 | `updateXXX` | `updateOrderStatus` |
| 删除 | `deleteXXX` | `deleteUser` |

### 数据库
- 实体类：`@Data` + `@TableName`
- 非数据库字段：`@TableField(exist = false)`
- 简单查询用 `LambdaQueryWrapper`，复杂查询用 `@Select` 注解

### 安全
- 密码：BCrypt 加密，不可逆
- 敏感配置：环境变量，不硬编码
- 异常：不泄露堆栈给前端

---

## 前端（Vue 3 + Vite）

### 组合式 API
- 必须用 `<script setup>`，不用选项式 API
- 响应式数据：`ref()`
- 计算属性：`computed()`

### 导入顺序
```js
// 1. Vue
import { ref, computed, onMounted } from 'vue'
// 2. 请求
import request from '@/utils/request'
// 3. 组件
// 4. 工具函数和常量
```

### 请求规范
- 统一走 `@/utils/request.js`（axios 封装）
- 自动附加 JWT token
- 响应格式：`{ code, message, data }`
- 成功判断：`res.code === 200`
- 错误提示：`ElMessage`，不用 `alert()`

### 样式规范
- 全局共享样式：`design.css`（filter-bar、tabs、badge、dialog、form 等）
- 页面特有样式：`<style scoped>`
- 不重复定义 `design.css` 已有的类
- 颜色体系：
  - 主色：`#0f1419`
  - 次要：`#536471`
  - 辅助：`#8899a6`
  - 边框：`#eff3f4`
  - 背景：`#f7f9f9`

### 组件复用
- 共享组件放 `src/components/`
- 开发前检查是否已有可用组件
- 表单用 design.css 的 `.form-group`、`.form-input`、`.form-select`

---

## Git 规范

### 分支
- 功能分支：`feat/xxx`
- 修复分支：`fix/xxx`
- 只有 `main` 触发 CI/CD
- 合并用 `--no-ff`

### 提交信息
- `feat: 简短描述`
- `fix: 简短描述`
- `docs: 简短描述`
- 中文或英文都行，保持简洁
