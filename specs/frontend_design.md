# 前端设计规范

## 设计风格

ElevenLabs × X.com 风格 — 深色侧边栏 + 白色内容区 + 极简调色板 + 圆角药丸元素。

---

## 颜色体系

### 基础色

| 用途 | 色值 | 场景 |
|------|------|------|
| 主色 | `#0f1419` | 标题、主按钮、侧边栏背景、激活标签 |
| 主色悬停 | `#272c30` | 主按钮 hover |
| 次要文字 | `#536471` | 描述、副标题、标签默认色、订单数量 |
| 辅助文字 | `#8899a6` | 时间戳、提示、禁用按钮、label |
| 边框 | `#eff3f4` | 卡片边框、分割线、输入框边框 |
| 次要背景 | `#f7f9f9` | 搜索框背景、悬停背景、meta 标签、订单头部 |
| 关闭/取消边框 | `#cfd9de` | ghost 按钮边框、卡片 hover 边框 |

### 状态色

| 状态 | 背景 | 文字 | 边框 | 场景 |
|------|------|------|------|------|
| 待处理/警告 | `#fff7e6` | `#b45309` | `#fde68a` | 待确认、待揽收 |
| 运输中/信息 | `#eff6ff` | `#1d4ed8` | `#bfdbfe` | 运输中、配送中 |
| 已完成/成功 | `#f0fdf4` | `#15803d` | `#bbf7d0` | 已完成、已送达 |
| 危险/删除 | `#fff5f5` | `#f4212e` | `#fdc9ce` | 删除按钮 hover |

---

## 页面布局

### 整体结构（demo.vue）

```
┌──────────────┬──────────────────────────────────┐
│              │  顶部导航栏                        │
│  侧边栏       │  [折叠按钮] 面包屑    角色·用户名   │
│  #0f1419     ├──────────────────────────────────┤
│              │                                  │
│  Logo        │  <router-view />                 │
│  角色标签     │  内容区 — 白色背景                  │
│  菜单项       │  max-width: 960px                │
│  (icon+标题)  │                                  │
│              │                                  │
│  用户卡片     │                                  │
│  退出登录     │                                  │
└──────────────┴──────────────────────────────────┘
```

- 侧边栏支持折叠（`collapsed` 状态只显示图标）
- 菜单项：图标 + 标题 + 副标题（折叠时只显示图标）
- 面包屑：支持多级（如 库存管理 / 商品上架）
- 顶部右侧：角色徽章 + 用户名 + 仓库名（配送员）

### 标准列表页结构

```html
<div class="page-container">
    <h1>页面标题</h1>

    <!-- 筛选栏 -->
    <div class="filter-bar">
        <div class="search-box">
            <input class="search-input" placeholder="搜索..." />
            <button class="btn btn-ghost">搜索</button>
        </div>
        <div class="tabs">
            <button class="tab active">全部</button>
            <button class="tab">待确认</button>
            <button class="tab">已完成</button>
        </div>
    </div>

    <!-- 列表 -->
    <div class="order-item" v-for="item in list">
        <div class="order-header">
            <span class="order-id">#{{ item.id }}</span>
            <span class="order-time">{{ item.time }}</span>
            <span class="status-badge status-pending">待确认</span>
        </div>
        <!-- 内容 -->
    </div>

    <!-- 空状态 -->
    <div v-if="list.length === 0" class="empty-state">
        <p>暂无数据</p>
    </div>
</div>
```

### 标准表单页结构

```html
<div class="page-container">
    <h1>页面标题</h1>
    <div class="form-container">
        <div class="form-section">
            <h2>基本信息</h2>
            <div class="form-row">
                <div class="form-group">
                    <label class="form-label">名称 <span class="required">*</span></label>
                    <input class="form-input" />
                </div>
                <div class="form-group">
                    <label class="form-label">价格</label>
                    <input class="form-input" />
                </div>
            </div>
        </div>
        <div class="form-actions">
            <button class="btn btn-ghost">取消</button>
            <button class="btn btn-primary">提交</button>
        </div>
    </div>
</div>
```

---

## design.css 共享组件

以下类已在 `design.css` 中定义，页面直接使用，不要在 `<style scoped>` 中重复定义。

### 按钮

| 类名 | 样式 | 用途 |
|------|------|------|
| `.btn` | 基础按钮（pill 形状，h36px） | 所有按钮的基类 |
| `.btn-primary` | 黑底白字 | 主操作（创建、提交、保存） |
| `.btn-ghost` | 白底黑字灰边框 | 次要操作（搜索、取消） |
| `.btn-danger` | 白底红字红边框 | 危险操作（删除） |
| `.btn-sm` | 小号（h32px） | 行内操作按钮 |

### 筛选栏

| 类名 | 说明 |
|------|------|
| `.filter-bar` | 白色卡片容器，flex-column，gap 12px |
| `.search-box` | 搜索框 + 按钮容器，flex，gap 8px |
| `.search-input` | 圆角搜索输入框（pill 形状，灰色背景） |
| `.tabs` | 标签容器，flex-wrap |
| `.tab` / `.tab.active` | 标签按钮（pill 形状，激活时黑底白字） |

### 卡片与列表项

| 类名 | 说明 |
|------|------|
| `.card` | 白色卡片，1px 边框，12px 圆角 |
| `.list-item` / `.order-item` / `.inventory-item` / `.product-item` / `.batch-item` / `.address-card` | 列表项卡片（统一样式，hover 变深边框） |
| `.order-header` | 订单头部（灰色背景，flex 布局） |
| `.order-id` / `.order-time` | 订单号/时间（12px，灰色） |

### 状态徽章

| 类名 | 样式 |
|------|------|
| `.badge` / `.default-badge` | 默认徽章（灰底灰字，pill 形状） |
| `.status-badge` | 状态徽章基类 |
| `.status-badge.status-pending` | 橙色（待处理） |
| `.status-badge.status-delivering` | 蓝色（运输中） |
| `.status-badge.status-completed` / `.completed` | 绿色（已完成） |

### 弹窗

| 类名 | 说明 |
|------|------|
| `.dialog-overlay` | 遮罩层（黑色半透明 + 模糊） |
| `.dialog-content` | 弹窗主体（白色，16px 圆角，max-width 520px） |
| `.dialog-header` | 弹窗头部（标题 + 关闭按钮） |
| `.close-btn` | 关闭按钮（圆形，hover 灰色背景） |

### 表单

| 类名 | 说明 |
|------|------|
| `.form-container` | 表单外层卡片（白色，24px 内边距） |
| `.form-section` | 表单分区（底部 24px 间距） |
| `.form-group` | 表单组（label + input，flex-column） |
| `.form-label` | 标签（13px，加粗） |
| `.form-input` / `.form-select` / `.form-textarea` | 输入控件（8px 圆角，focus 时黑色边框） |
| `.form-row` | 两列网格布局 |
| `.form-actions` | 按钮行（右对齐，顶部分割线） |
| `.required` | 必填红星 `*` |

### 商品相关

| 类名 | 说明 |
|------|------|
| `.product-image` | 商品图片容器（8px 圆角） |
| `.product-name` | 商品名（14px，加粗） |
| `.product-description` | 商品描述（13px，灰色） |
| `.product-detail` / `.product-meta` | 商品详情/元信息 |
| `.meta-item` | 元信息标签（pill 形状，灰色背景） |
| `.price` | 价格（加粗，黑色） |

### 地图与物流

| 类名 | 说明 |
|------|------|
| `.map-section` | 地图区域卡片 |
| `.map-container` | 地图容器（500px 高） |
| `.route-info` / `.route-info-row` | 路线信息 |
| `.delivery-points` | 配送点列表卡片 |
| `.point-item` | 配送点项（灰色背景，hover 变深） |
| `.point-marker` | 配送点序号（黑色圆形） |

### 空状态

| 类名 | 说明 |
|------|------|
| `.empty-state` | 空数据提示（居中，灰色文字，白色卡片） |

---

## 设计原则

1. 圆角药丸（pill）是核心视觉元素 — 按钮、标签、搜索框、徽章都用 `border-radius: 9999px`
2. 卡片用 `border-radius: 12px`，输入框用 `border-radius: 8px`
3. 颜色克制 — 主要只用黑白灰，状态色只在徽章上出现
4. 交互反馈 — 所有可点击元素都有 `transition: all 0.15s` 和 hover 效果
5. 间距统一 — 卡片间距 16px，内边距 16px/20px/24px
6. 字号体系 — h1: 18px, h2: 15px, 正文: 13-14px, 辅助: 11-12px

## 响应式

- 移动端断点：`640px`
- `.form-row` 在移动端变为单列
- `.order-header` 允许换行
- `.batch-header` 在移动端变为纵向排列
- `.batch-actions` 在移动端变为纵向排列
- `.tabs` 用 `flex-wrap` 自动换行
