# 贡献指南

感谢你考虑为 **审稿宝（Proofly）** 做出贡献！🎉

本文档说明如何参与项目开发、提交 Issue 与 Pull Request。

## 📋 目录

- [行为准则](#行为准则)
- [我能贡献什么](#我能贡献什么)
- [开发流程](#开发流程)
- [代码规范](#代码规范)
- [Commit 规范](#commit-规范)
- [Pull Request 流程](#pull-request-流程)

## 行为准则

本项目采用 [Contributor Covenant v2.1](CODE_OF_CONDUCT.md)。参与即代表你同意遵守其中的条款。

## 我能贡献什么

- 🐛 **Bug 报告**：使用 Issue 模板
- ✨ **功能建议**：使用 Feature Request 模板
- 📝 **文档改进**：错别字、翻译、补充示例
- 💻 **代码贡献**：新功能、bug 修复、性能优化
- 🧪 **测试**：单元测试、集成测试

## 开发流程

### 1. Fork & Clone

```bash
git clone https://github.com/<your-name>/Proofly.git
cd Proofly
git remote add upstream https://github.com/liyu-1028/Proofly.git
```

### 2. 创建分支

```bash
git checkout -b feat/your-feature-name
# 或 fix/issue-number-description
```

### 3. 本地开发

参考 [docs/quickstart.md](docs/quickstart.md) 启动项目。

### 4. 保持同步

```bash
git fetch upstream
git rebase upstream/main
```

### 5. 提交 & 推送

```bash
git add .
git commit -m "feat(scope): 简短描述"
git push origin feat/your-feature-name
```

### 6. 创建 PR

在 GitHub 上发起 Pull Request，**目标分支为 `main`**。

## 代码规范

### 后端（Java / Spring Boot）

- **JDK 17** 语法特性可用
- 遵循 [Google Java Style](https://google.github.io/styleguide/javaguide.html)（4 空格缩进）
- 所有 Controller 必须通过 `@Valid` 校验入参
- Service 承载业务逻辑、事务与跨资源编排；**禁止** Controller 直接调用 DAO
- 数据库实体与 DTO 分离，**禁止**直接暴露表结构
- 多租户隔离：所有业务表必须按 `store_id` 过滤

### 前端（Vue 3 / TypeScript）

- `<script setup>` 语法
- **TypeScript 严格模式**开启，禁止使用 `any`
- API 请求统一通过 `src/api/*.ts` 封装
- 组件命名 PascalCase，文件名同组件名
- Element Plus 主题色与青绿主色保持一致（详见 `docs/frontend-style-guide.md`）

### 数据库

- 新增表必须包含 `store_id`（多租户隔离）
- 表名使用单数（`user` 而非 `users`）
- 主键统一 BIGINT，由应用层 IdWorker 生成
- 所有时间字段使用 `DATETIME(3)`，自动维护 `created_at` / `updated_at`
- 软删除使用 `deleted TINYINT(1) DEFAULT 0`

### 文档

- 新增模块时同步更新 `docs/system-module-list.md` 与 `docs/module-completion.md`
- 新增/修改接口时更新对应模块的 `docs/api-mXX-*.md`

## Commit 规范

采用 [Conventional Commits](https://www.conventionalcommits.org/zh-hans/)：

```
<type>(<scope>): <subject>

<body>

<footer>
```

**type**：

| 类型 | 用途 |
|------|------|
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `docs` | 仅文档变更 |
| `style` | 代码格式（不影响逻辑） |
| `refactor` | 重构（既不是新功能也不是 Bug 修复） |
| `perf` | 性能优化 |
| `test` | 添加或修改测试 |
| `chore` | 构建/工具/依赖变更 |

**scope**（可选）：模块名，如 `auth`、`project`、`billing`。

**示例**：

```bash
feat(billing): 添加年度套餐自动折扣
fix(annotation): 修复点位坐标存储偏差
docs(readme): 更新快速启动命令
```

## Pull Request 流程

### PR 标题

与 commit message 一致：

```
feat(billing): 添加年度套餐自动折扣
```

### PR 描述模板

```markdown
## 背景
（为什么做这个改动？解决什么问题？）

## 改动内容
- 改动 1
- 改动 2

## 测试
- [ ] 单元测试
- [ ] 集成测试
- [ ] 手动验证步骤：...

## 截图
（如有 UI 改动）

## 相关 Issue
Closes #123
```

### 审核要求

- ✅ CI 通过（lint + 编译）
- ✅ 至少 1 位维护者 Review 通过
- ✅ 无合并冲突
- ✅ 涉及数据库变更时同步提供 DDL

### 合并策略

- Squash and merge（保持 main 历史干净）
- 或 Rebase and merge（保留原子提交）

## 发布流程

1. 维护者从 `main` 创建 `release/vX.Y.Z` 分支
2. 更新 `CHANGELOG.md` 并打 tag `vX.Y.Z`
3. PR 合入 `main`
4. 推送 tag 触发 GitHub Release

---

有问题？在 [GitHub Discussions](https://github.com/liyu-1028/Proofly/discussions) 发帖讨论。