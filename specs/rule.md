# 编码规范

后端编码规范见 `backend.md`，前端编码规范见 `frontend.md`。

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
- `refactor: 简短描述`
- `test: 简短描述`
- 中文或英文都行，保持简洁
- 一个提交只做一件事，不要混合功能和修复

### 提交前检查
- 后端：确保编译通过（`mvn compile`）
- 前端：确保无 lint 错误
- 运行相关测试，确保通过
- 不要提交 `console.log`、`System.out.println` 等调试代码
- 不要提交硬编码的密钥或密码

---

## 代码审查

### 审查重点
- 是否符合 `backend.md` / `frontend.md` 的规范
- 是否有安全隐患（SQL 注入、XSS、硬编码密钥）
- 是否有性能问题（N+1 查询、不必要的全表扫描）
- 是否有测试覆盖
- 错误处理是否完善（不要吞掉异常）

### 常见问题清单
- [ ] Controller 里有没有 try-catch（应该用 GlobalExceptionHandler）
- [ ] 前端有没有用 `alert()`（应该用 `ElMessage`）
- [ ] 有没有重复定义 `design.css` 已有的样式
- [ ] 密码有没有用 BCrypt
- [ ] 敏感配置有没有走环境变量
- [ ] 前端有没有用 `<script setup>`

---

## 测试规范

### 后端（JUnit 5）
- 测试类放在 `src/test/java/` 对应包下
- 测试方法命名：`should_预期行为_when_条件`
- 每个 Service 至少有基本的单元测试
- 关键业务逻辑（订单状态流转、配送批次）必须有测试

### 前端（Vitest）
- 测试文件放在 `src/__tests__/` 或组件同目录
- 测试文件命名：`xxx.test.js`
- 关键工具函数和组件逻辑必须有测试
- 运行：`npx vitest --run`（不要用 watch 模式）

### 全流程测试
- `tests/full-flow-test.sh`：从入库到收货的完整链路测试
- 每次重大改动后运行，确保数据流转正确
- 测试完自动清理测试数据
