# Changelog

本项目所有重要变更都将记录在此文件中。版本号遵循 [Semantic Versioning](https://semver.org/)。

格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。

## [Unreleased]

### 计划中
- M12 云原生部署方案（K8s / Helm / Prometheus）
- M14 AI 辅助能力（智能总结、沟通文案）
- 标注回复功能完整化
- 确认单导出（PDF）

## [0.1.0] - 2026-07-17

首个公开发布版本。

### ✨ 新增（Added）

#### 后端能力
- **M01 账号、门店与权限**：JWT + Redis 黑名单、RBAC（owner / designer / admin）、Spring Security
- **M02 审稿项目**：项目生命周期管理、状态日志
- **M03 设计稿版本**：自动递增版本号、首版上传触发状态流转
- **M04 文件存储**：MinIO 集成 + 预签名 URL
- **M05 客户审稿链接**：SHA-256 哈希 Token、禁用/启用/删除
- **M06 在线标注评论**：点位标注、状态联动、语音批注（Pro 增值）
- **M07 客户确认定稿**：幂等确认、IP/UA 留痕、状态自动变更
- **M08 审计日志**：通用 AuditLogService + 时间线查询
- **M09 后台工作台**：状态看板、最近项目、近期动态
- **M10 站内通知**：项目动态自动触发、未读计数
- **M11 系统配置**：自定义 Logo、品牌色
- **M13 套餐与账单**：XPay 模拟沙箱、Webhook 验签、邀请裂变、自动到期降级

#### 前端能力
- 登录、注册、自助门店创建
- 工作台看板
- 项目列表与详情（含标注、版本管理、确认状态）
- 员工管理（CRUD + 重置密码）
- 审稿链接管理（生成 / 禁用 / 删除）
- 套餐选择与模拟收银台
- 通知中心
- 公开客户审稿页（点图标注 + 语音批注 + 确认定稿）

### 🐛 修复（Fixed）
- 完成率显示为 NaN 的问题
- ID 精度丢失（数字 → 字符串）
- 项目服务空指针
- 文件上传大小限制异常处理

### 📦 基础设施
- Docker Compose 一键启动 MySQL / Redis / MinIO
- 后端 / 前端 Dockerfile（多阶段构建）
- `.env.example` 环境变量模板
- `docs/quickstart.md` 5 分钟跑起来
- 完整的数据库 DDL + 开发种子数据分离

### 🔒 安全
- 移除 `application.yml` 中 RSA 私钥的默认值（必须环境变量注入）
- 默认 MinIO / MySQL 凭据替换为官方开发默认值（minioadmin / proofly_dev）
- `.gitignore` 完善（`.claude/`、`.mcp.json`、`.antigravitycli/` 等）

### 📝 文档
- README.md 重写（特性、架构、快速启动、模块清单）
- LICENSE (GPL-3.0)
- CONTRIBUTING.md
- SECURITY.md
- CODE_OF_CONDUCT.md
- AGENT.md / GEMINI.md 清理私人路径
- 拆分 `docs/mysql-schema.sql`（DDL）与 `docs/seed-dev.sql`（种子数据）

### ⚠️ 注意事项
- 真实 XPay 商户对接尚未完成，当前为模拟沙箱
- 默认管理员账号 `admin` / `admin123` 仅供本地开发，**生产部署必须修改**
- 所有敏感密钥（JWT secret、RSA 私钥、数据库密码）**必须**通过环境变量注入

[Unreleased]: https://github.com/liyu-1028/Proofly/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/liyu-1028/Proofly/releases/tag/v0.1.0