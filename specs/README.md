# specs/ — 规范驱动开发（Spec-Driven Development）

参考 [GitHub spec-kit](https://github.com/github/spec-kit) 方法论。

## 核心理念
- 规范是主要产物，代码是规范的表达
- 聚焦 WHAT（做什么）和 WHY（为什么），不纠结 HOW（怎么做）
- 用 `[待确认]` 标记不确定的地方，不要猜

## 流程
1. 写 `specs/功能名.md`（参考 `templates/spec-template.md`）
2. Boss 审核 → 通过 / 修改
3. 按规范写代码
4. 对照验收标准验证
5. 完成后移到 `specs/done/`

## 目录结构
```
specs/
├── README.md              # 本文件
├── constitution.md        # 项目宪法（核心原则和约束）
├── templates/
│   └── spec-template.md   # 规范模板
├── route-guards.md        # 当前规范
├── global-exception-handler.md
└── done/                  # 已完成的规范
```

## 什么需要写规范
- 新功能、新页面、新接口
- 改变行为的重大重构

## 什么不需要写规范
- Bug修复、代码清理、格式化、依赖更新
