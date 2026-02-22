# 流程图绘制规范

本项目使用 Mermaid 在 README 中绘制流程图，GitHub 原生渲染。

---

## 工具

- Mermaid（GitHub 原生支持，无需安装）
- 在线预览：https://mermaid.live
- 参考：[ai-diagrams-toolkit](https://github.com/joserprieto/ai-diagrams-toolkit)

## 基本原则

1. 一张图说清楚，不要拆成多张
2. 少即是多 — 颜色、形状、连线越少越好
3. 节点名用语义化命名（`StockIn`，不要用 `A`、`B`）
4. 注释用英文，用户可见文本用中文

## 主题配置

每个 Mermaid 图开头加：

```
%%{init: {'theme':'base', 'themeVariables': {'primaryColor':'#f0f0f0', 'edgeLabelBackground':'#ffffff'}}}%%
```

## 语义颜色系统

用 `classDef` 定义颜色，按含义分配，不要随意用色：

```mermaid
%% 状态色
classDef operational fill:#4CAF50,stroke:#2E7D32,color:#FFF,stroke-width:2px   %% 绿色 = 完成/成功
classDef info fill:#2196F3,stroke:#1565C0,color:#FFF,stroke-width:2px          %% 蓝色 = 信息/起点
classDef warning fill:#FFC107,stroke:#F57C00,color:#000,stroke-width:2px       %% 黄色 = 决策/注意
classDef error fill:#F44336,stroke:#C62828,color:#FFF,stroke-width:2px         %% 红色 = 错误/失败

%% 架构层色（浅底深边）
classDef processingLayer fill:#E8F5E9,stroke:#2E7D32,color:#1B5E20    %% 处理逻辑
classDef storageLayer fill:#FFF3E0,stroke:#E65100,color:#BF360C       %% 数据存储
classDef communicationLayer fill:#F3E5F5,stroke:#7B1FA2,color:#4A148C %% API/通信
classDef presentationLayer fill:#E0F2F1,stroke:#00796B,color:#004D40  %% UI/展示
```

使用方式：节点后加 `:::className`

```
Start([开始]):::info
Store[(保存)]:::storageLayer
```

## 节点形状

| 形状 | 语法 | 用途 |
|------|------|------|
| 矩形 | `[文本]` | 普通步骤 |
| 圆角 | `([文本])` | 起点/终点 |
| 菱形 | `{文本}` | 决策判断 |
| 圆柱 | `[(文本)]` | 数据库/存储 |
| 双边框 | `[[文本]]` | API调用/子程序 |
| 六边形 | `{{文本}}` | 准备步骤 |
| 平行四边形 | `[/文本/]` | 数据输入 |
| 圆形 | `((文本))` | 连接点 |

## 连线类型

```
A --> B          %% 实线箭头
A -.-> B         %% 虚线箭头（跨角色交互）
A ==> B          %% 粗线箭头（重要流程）
A ~~~  B         %% 不可见连线（控制布局，不画任何东西）
A -->|标签| B     %% 带标签
```

## Subgraph 样式

```mermaid
subgraph MyGroup["显示名称"]
    direction LR
    ...
end

%% style 用 ID（不带引号），不能用显示名称
style MyGroup fill:#FFF8E1,stroke:#F57C00,stroke-width:2px
```

注意：`style` 引用的是 subgraph 的 ID，不是引号里的标签。

## 本项目的角色配色

| 角色 | 填充色 | 边框色 |
|------|--------|--------|
| 管理员 | `#F3E5F5` | `#7B1FA2`（紫色） |
| 商户 | `#FFF8E1` | `#F57C00`（橙色） |
| 消费者 | `#E8F5E9` | `#388E3C`（绿色） |
| 配送员 | `#E3F2FD` | `#1976D2`（蓝色） |

## 布局技巧

- `flowchart TB` = 从上到下（适合业务流程）
- `flowchart LR` = 从左到右（适合管道/链路）
- subgraph 内用 `direction LR` 让内部节点横排
- 用 `~~~` 不可见连线控制节点顺序（如让管理员在最上面）

## 避坑指南

1. `classDef` 只在 `flowchart`/`graph` 中有效，sequence/class/state/ER 图不支持
2. 不要用 `end`、`class`、`style` 作为节点 ID（保留字）
3. 节点 ID 不要以 `o` 或 `x` 开头（会被解析为特殊边类型）
4. `classDef` 写在节点之后（不是之前）
5. subgraph 的 `style` 必须用 ID，不能用引号字符串
6. 中文冒号可能导致解析错误，用引号包裹：`["状态: 活跃"]`
7. 一张图节点不要超过 20 个，太多就拆分
