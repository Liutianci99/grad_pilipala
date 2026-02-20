# specs/ — 规范驱动开发

每个新功能开发前，先在这里写规范文档。

## 流程
1. 创建 `specs/功能名.md`
2. Boss 审核 → 通过 / 修改
3. 按规范写代码
4. 对照验收标准验证
5. 合并后移到 `specs/done/`

## 需要写规范的
- 新功能、新页面、新接口
- 改变行为的重大重构

## 不需要写规范的
- Bug修复、代码清理、格式化、依赖更新

---

## 编码规范（参考标准）

以下规范从项目实际代码和参考文档中提炼，所有新代码必须遵循。

### 后端（Spring Boot）

#### API 接口规范
- 统一返回 `Result<T>`，包含 `code`、`message`、`data`
- 成功：`Result.success(data)`，失败：`Result.error("错误信息")`
- RESTful 风格：GET 查询、POST 创建、PUT 更新、DELETE 删除

#### 函数命名规范

| 操作类型 | Controller 方法名 | 示例 |
|----------|-------------------|------|
| 查询列表 | `listXXX` | `listUsers`、`listOrders` |
| 查询详情 | `getXXX` | `getOrderDetail` |
| 创建 | `createXXX` | `createUser` |
| 更新 | `updateXXX` | `updateOrderStatus` |
| 删除 | `deleteXXX` | `deleteUser` |

#### 异常处理
- 使用 `GlobalExceptionHandler`（`@RestControllerAdvice`）统一处理
- 业务异常抛 `BusinessException`，由全局处理器捕获
- Controller 中不写 try-catch

#### 数据库
- 使用 MyBatis-Plus，复杂查询用 `@Select` 注解
- 实体类用 `@Data` + `@TableName`
- 非数据库字段用 `@TableField(exist = false)`

### 前端（Vue 3）

#### 组合式 API 原则
- 使用 Vue 3 `<script setup>` 组合式 API
- 响应式数据用 `ref()`，计算属性用 `computed()`
- 不使用选项式 API

#### 请求规范
- 所有请求通过 `@/utils/request.js`（axios 封装）
- 请求拦截器自动附加 JWT token
- 响应格式：`{ code, message, data }`
- 成功判断：`res.code === 200`

#### 错误处理
- 使用 `ElMessage` 提示错误，不用 `alert()`
- 请求失败在 catch 中处理，显示友好提示

#### 样式规范
- 全局共享样式写在 `design.css`（filter-bar、tabs、badge、dialog、form 等）
- 页面特有样式用 `<style scoped>`
- 不重复定义 design.css 已有的类
- 颜色体系：`#0f1419`（主色）、`#536471`（次要）、`#8899a6`（辅助）、`#eff3f4`（边框）

#### 组件复用
- 共享组件放 `src/components/`
- 开发前检查是否已有可用组件，避免重复开发
- 表单用 design.css 的 `.form-group`、`.form-input`、`.form-select` 等

#### 页面结构标准模式
```vue
<template>
    <div class="page-container">
        <h1>页面标题</h1>
        <div class="filter-bar">
            <div class="search-box">
                <input class="search-input" ... />
                <button class="btn btn-ghost">搜索</button>
            </div>
            <div class="tabs">
                <button :class="['tab', { active: ... }]">标签</button>
            </div>
        </div>
        <!-- 列表内容 -->
        <div v-if="list.length === 0" class="empty-state"><p>暂无数据</p></div>
    </div>
</template>
```

#### 脚本导入顺序
```js
// 1. Vue 相关
import { ref, computed, onMounted } from 'vue'
// 2. 请求工具
import request from '@/utils/request'
// 3. 组件（如有）
// 4. 常量和工具函数
```

### 分支与提交规范
- 每个功能一个分支：`feat/xxx`、`fix/xxx`
- 只有 `main` 触发 CI/CD
- 提交信息格式：`feat: 简短描述` 或 `fix: 简短描述`
- 合并到 main 用 `--no-ff`

### 验收标准模板
每个规范文档必须包含验收标准（checklist），格式：
```markdown
## 验收标准
- [ ] 具体可验证的条件1
- [ ] 具体可验证的条件2
```
