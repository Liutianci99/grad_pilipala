# Claude Code 最佳实践

本文档整理自 Anthropic 官方文档和社区最佳实践，适配本项目使用。

## 核心原则

### 1. 给 Claude 验证方式

**最重要的一条：让 Claude 能自己检查工作成果。**

- 提供测试用例、预期输出、截图对比
- 让 Claude 运行测试、对比结果、修复失败
- 不要只说"实现功能"，要说"实现功能，写测试，运行测试，修复失败"

示例：
```
❌ 实现一个邮箱验证函数
✅ 写一个 validateEmail 函数。测试用例：user@example.com 返回 true，
   invalid 返回 false，user@.com 返回 false。实现后运行测试并修复失败
```

### 2. 先探索，再计划，再编码

**分离研究和实施，避免解决错误的问题。**

推荐流程：
1. **探索**：使用 Plan Mode，Claude 只读文件、回答问题，不修改代码
2. **计划**：让 Claude 创建详细实施计划，按 Ctrl+G 在编辑器中审查
3. **实施**：切回 Normal Mode，让 Claude 按计划编码并验证
4. **提交**：让 Claude 提交代码并创建 PR

何时跳过计划：
- 范围明确且改动小（修改拼写、添加日志、重命名变量）
- 能用一句话描述 diff 的改动

### 3. 提供具体上下文

**越精确的指令，越少的修正。**

| 策略 | 之前 | 之后 |
|------|------|------|
| 明确范围 | "给 foo.py 加测试" | "给 foo.py 写测试，覆盖用户登出的边界情况，避免 mock" |
| 指向源头 | "为什么 ExecutionFactory 的 API 这么奇怪？" | "查看 ExecutionFactory 的 git 历史，总结它的 API 是如何演变的" |
| 参考现有模式 | "添加日历组件" | "看看首页现有组件的实现模式，HotDogWidget.php 是个好例子。按照这个模式实现一个日历组件，让用户选择月份并前后翻页选择年份。从零开始构建，只用代码库中已有的库" |
| 描述症状 | "修复登录 bug" | "用户报告会话超时后登录失败。检查 src/auth/ 中的认证流程，特别是 token 刷新。写一个能复现问题的失败测试，然后修复它" |

### 4. 使用富内容

- **用 `@` 引用文件**：不要描述代码在哪，直接 `@文件名`
- **粘贴图片**：复制粘贴或拖拽图片到提示中
- **给 URL**：文档和 API 参考的链接
- **管道输入数据**：`cat error.log | claude`
- **让 Claude 自己获取**：告诉 Claude 用 Bash 命令、MCP 工具或读文件自己拉取上下文

## 环境配置

### CLAUDE.md 文件

**项目根目录的 `CLAUDE.md` 是 Claude 每次会话开始时读取的特殊文件。**

包含内容：
- Bash 命令（Claude 无法从代码推断的）
- 代码风格规则（与默认不同的）
- 测试指令和首选测试运行器
- 仓库礼仪（分支命名、PR 约定）
- 架构决策（项目特有的）
- 开发环境特殊性（必需的环境变量）
- 常见陷阱或非显而易见的行为

不要包含：
- Claude 能从代码中推断的内容
- 标准语言约定（Claude 已经知道的）
- 详细的 API 文档（改为链接到文档）
- 频繁变化的信息
- 长篇解释或教程
- 逐文件的代码库描述
- 不言自明的做法（如"写干净的代码"）

**保持简洁**：对每一行问自己："删除这行会导致 Claude 犯错吗？"如果不会，就删掉。

CLAUDE.md 可以导入其他文件：
```markdown
参见 @README.md 了解项目概览，@package.json 查看可用的 npm 命令。

# 额外说明
- Git 工作流：@docs/git-instructions.md
- 个人覆盖：@~/.claude/my-project-instructions.md
```

位置：
- **主目录** (`~/.claude/CLAUDE.md`)：应用于所有 Claude 会话
- **项目根目录** (`./CLAUDE.md`)：提交到 git 与团队共享，或命名为 `CLAUDE.local.md` 并 `.gitignore`
- **父目录**：适用于 monorepo，`root/CLAUDE.md` 和 `root/foo/CLAUDE.md` 都会自动拉取
- **子目录**：Claude 在处理这些目录中的文件时按需拉取子 CLAUDE.md

### 权限配置

默认情况下，Claude Code 会请求可能修改系统的操作权限：文件写入、Bash 命令、MCP 工具等。

减少中断的两种方式：
- **权限白名单**：允许你知道安全的特定工具（如 `npm run lint` 或 `git commit`）
- **沙箱**：启用 OS 级隔离，限制文件系统和网络访问

### 使用 CLI 工具

**告诉 Claude Code 使用 CLI 工具与外部服务交互。**

如果使用 GitHub，安装 `gh` CLI。Claude 知道如何用它创建 issue、打开 PR、读取评论。

Claude 也擅长学习它不知道的 CLI 工具。试试这样的提示：
```
使用 'foo-cli-tool --help' 了解 foo 工具，然后用它解决 A、B、C
```

### 连接 MCP 服务器

运行 `claude mcp add` 连接外部工具，如 Notion、Figma 或你的数据库。

### 设置 Hooks

**Hooks 用于必须每次都发生、零例外的操作。**

与 CLAUDE.md 指令（建议性）不同，hooks 是确定性的，保证操作发生。

Claude 可以为你编写 hooks。试试这样的提示：
- "写一个 hook，每次文件编辑后运行 eslint"
- "写一个 hook，阻止写入 migrations 文件夹"

### 创建 Skills

**在 `.claude/skills/` 中创建 `SKILL.md` 文件，给 Claude 领域知识和可重用工作流。**

Skills 扩展 Claude 的知识，包含项目、团队或领域特定的信息。Claude 在相关时自动应用，或你可以用 `/skill-name` 直接调用。

示例：
```markdown
---
name: api-conventions
description: 我们服务的 REST API 设计约定
---
# API 约定
- URL 路径使用 kebab-case
- JSON 属性使用 camelCase
- 列表端点始终包含分页
- 在 URL 路径中版本化 API (/v1/, /v2/)
```

可重用工作流示例：
```markdown
---
name: fix-issue
description: 修复 GitHub issue
disable-model-invocation: true
---
分析并修复 GitHub issue: $ARGUMENTS。

1. 使用 `gh issue view` 获取 issue 详情
2. 理解 issue 中描述的问题
3. 在代码库中搜索相关文件
4. 实施必要的更改以修复 issue
5. 编写并运行测试以验证修复
6. 确保代码通过 linting 和类型检查
7. 创建描述性提交消息
8. 推送并创建 PR
```

### 创建自定义 Subagents

**在 `.claude/agents/` 中定义专门的助手，Claude 可以委托给它们处理隔离的任务。**

Subagents 在自己的上下文中运行，有自己的允许工具集。适用于需要读取许多文件或需要专门关注而不弄乱主对话的任务。

## 上下文管理

**最重要的约束：Claude 的上下文窗口填充很快，性能随着填充而下降。**

上下文窗口包含整个对话，包括每条消息、Claude 读取的每个文件和每个命令输出。

当上下文窗口接近满时，Claude 可能开始"忘记"早期指令或犯更多错误。

策略：
- 用自定义状态行持续跟踪上下文使用
- 参见"减少 token 使用"了解减少 token 使用的策略
- 在上下文 >85% 时考虑重置会话

## 本项目适配

### 我们的 CLAUDE.md 应该包含

```markdown
# 项目概览
电商物流管理系统 - Spring Boot 3.2 + Vue 3 + MySQL

# 代码风格
- 后端：RESTful API，统一返回 Result<T>
- 前端：必须用 <script setup>，不用选项式 API
- 前端：全局样式用 design.css，不要重复定义

# 工作流
- 写代码前先读 specs/ 下的 6 个规范文件
- 测试：后端用 JUnit，前端用 vitest
- 提交前运行测试确保通过
- 分支：feat/xxx 或 fix/xxx，只有 main 触发 CI/CD

# 环境
- 代理：export https_proxy=http://127.0.0.1:7890
- 数据库：Docker 容器 grad-logistics-db，端口 3308
- 前端：端口 8888，后端：端口 8080

# 腾讯地图 API
- Key: SEWBZ-W4R6N-V7AFD-SWAI5-E7BRZ-FKBAH
- 用于：驾车路线规划、逆地理编码
```

### 我们的 Skills

可以创建：
- `/review-pr` - PR 审查工作流
- `/deploy-staging` - 部署到测试环境
- `/fix-lint` - 修复所有 lint 错误
- `/update-specs` - 更新规范文档

### 我们的 Hooks

可以设置：
- 文件编辑后自动运行 eslint
- 提交前运行测试
- 推送前检查规范文件是否更新
