# 前端设计规范

## 设计风格
ElevenLabs 风格 — 深色侧边栏 + 白色内容区 + 极简调色板。

## 颜色体系

| 用途 | 色值 | 示例 |
|------|------|------|
| 主色（文字、按钮） | `#0f1419` | 标题、主按钮 |
| 次要文字 | `#536471` | 描述、副标题 |
| 辅助文字 | `#8899a6` | 时间、提示 |
| 边框 | `#eff3f4` | 卡片边框、分割线 |
| 背景 | `#f7f9f9` | 悬停、次要背景 |
| 侧边栏 | `#0f1419` | 深色侧边栏 |
| 蓝色强调 | `#1d4ed8` | 运输中状态、链接 |
| 橙色警告 | `#b45309` | 已到达状态 |
| 绿色成功 | `#15803d` | 已完成状态 |
| 红色危险 | `#dc2626` | 删除按钮 |

## 页面布局

### 整体结构（demo.vue）
```
┌──────────┬─────────────────────────┐
│          │                         │
│  侧边栏   │      内容区              │
│  深色      │      白色背景            │
│  240px    │      flex: 1            │
│          │                         │
│  菜单项    │   h1 标题               │
│  用户卡片  │   filter-bar            │
│          │   内容列表/表格           │
│          │                         │
└──────────┴─────────────────────────┘
```

### 标准列表页结构
```html
<div class="page-container">
    <h1>页面标题</h1>
    <div class="filter-bar">
        <div class="search-box">
            <input class="search-input" />
            <button class="btn btn-ghost">搜索</button>
        </div>
        <div class="tabs">
            <button class="tab active">标签1</button>
            <button class="tab">标签2</button>
        </div>
    </div>
    <!-- 列表内容 -->
    <div v-if="list.length === 0" class="empty-state"><p>暂无数据</p></div>
</div>
```

## design.css 共享组件

以下类已在 `design.css` 中定义，页面直接使用，不要在 scoped style 中重复定义：

### 按钮
- `.btn` — 基础按钮
- `.btn-primary` — 主按钮（黑底白字）
- `.btn-ghost` — 幽灵按钮（透明底黑字）
- `.btn-danger` — 危险按钮（红色）
- `.btn-sm` — 小号按钮

### 筛选栏
- `.filter-bar` — 筛选容器（flex-column, gap 12px）
- `.search-box` — 搜索框容器
- `.search-input` — 搜索输入框
- `.tabs` — 标签容器（flex-wrap）
- `.tab` / `.tab.active` — 标签按钮

### 徽章
- `.badge` — 基础徽章（圆角药丸）

### 弹窗
- `.dialog-overlay` — 遮罩层
- `.dialog-content` — 弹窗内容
- `.dialog-header` — 弹窗头部
- `.close-btn` — 关闭按钮

### 表单
- `.form-group` — 表单组
- `.form-label` — 标签
- `.form-input` / `.form-select` / `.form-textarea` — 输入控件
- `.form-row` — 两列布局
- `.form-actions` — 按钮行（右对齐）
- `.required` — 必填红星

### 空状态
- `.empty-state` — 空数据提示

## 状态徽章颜色约定

| 状态 | 类名 | 背景 | 文字 |
|------|------|------|------|
| 默认/未发货 | 自定义 | `#f7f9f9` | `#536471` |
| 运输中 | 自定义 | `#eff6ff` | `#1d4ed8` |
| 已到达 | 自定义 | `#fff7e6` | `#b45309` |
| 已完成 | 自定义 | `#f0fdf4` | `#15803d` |

## 响应式
- 移动端断点：`640px`
- 平板断点：`768px`
- filter-bar 的 tabs 用 `flex-wrap` 自动换行
- 表单 `.form-row` 在移动端变为单列
