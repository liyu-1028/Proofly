# 审稿宝（Proofly）

> 一套面向**广告制作店 / 图文打印店 / 快印店 / 包装印刷店 / 设计工作室**的在线审稿确认系统。

![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)
![Status: MVP](https://img.shields.io/badge/Status-MVP-green)
![Backend: Spring Boot 3](https://img.shields.io/badge/Backend-Spring%20Boot%203.5-brightgreen)
![Frontend: Vue 3](https://img.shields.io/badge/Frontend-Vue%203.5-brightgreen)

## 📖 项目简介

**审稿宝**帮助设计工作室与门店，把传统线下"邮件/微信来回发稿"的设计确认流程搬到线上：

```
设计师创建项目 → 上传设计稿 → 生成审稿链接 → 客户标注修改意见
                                          ↓
                              设计师上传新版 → 客户确认定稿
                                          ↓
                              系统保存可追溯确认记录
```

**核心价值**：

- **多方协同**：设计师、客户、门店老板在同一闭环内协作
- **可追溯**：每个版本、每条标注、每次确认都留痕
- **多租户**：所有业务表按门店隔离，支持 SaaS 化运营
- **订阅制**：免费版/Pro 版套餐 + 自动到期管理 + 邀请裂变

## ✨ 核心特性

### 门店后台

- 🏪 **门店与员工管理** — 多角色 RBAC（owner / designer / admin）
- 📁 **审稿项目** — 完整生命周期：草稿 → 待反馈 → 需修改 → 已确认 → 归档
- 🖼️ **设计稿版本管理** — 自动递增版本号，历史不可覆盖
- 📝 **在线标注评论** — 客户在图上点位标注，设计师按版本处理
- ✅ **客户确认定稿** — 幂等确认，IP/UA 留痕
- 📊 **后台工作台** — 状态看板、最近项目、近期动态
- 💬 **站内通知** — 项目动态自动触达
- 💰 **套餐订阅** — 模拟 XPay 沙箱 + 自动续期 + 邀请裂变 + 到期降级

### 客户审稿（公开链接）

- 🔗 **免登录访问** — 哈希 Token，泄露可吊销
- 👁️ **版本切换预览** — 不下载原图
- ✏️ **点图标注** — 直接在浏览器中提交修改意见
- 🎤 **语音批注** — Pro 增值
- 🖋️ **确认定稿** — 提交后项目自动变更状态

### 运营 & 商业化

- 💼 **套餐计费** — 月/6月/年三档，自动应用折扣
- 📈 **用量控制** — 免费版限 3 个活跃项目、1 个老板
- 🎁 **裂变邀请** — 邀请双方各延期 30 天
- 🔔 **到期提醒** — 7/3/1 天前自动通知
- 📜 **账单流水** — 完整订单 + Webhook 回调 + 验签

## 🏗️ 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | JDK 17 · Spring Boot 3.5 · MyBatis-Plus 3.5 |
| 前端 | Vue 3.5 · Vite 6 · TypeScript · Element Plus 2.13 |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis 7.4 |
| 对象存储 | MinIO |
| 认证 | JWT（jjwt 0.13） + RSA 加密传输 |
| 部署 | Docker Compose · 多阶段构建 |

## 🚀 快速启动

```bash
git clone https://github.com/liyu-1028/Proofly.git
cd Proofly

# 1. 准备环境变量（必做！）
cp .env.example .env

# 2. 生成 RSA 密钥对并填入 .env（必做！否则后端启动失败）
mkdir -p backend/keys
openssl genpkey -algorithm RSA -out backend/keys/private.pem -pkeyopt rsa_keygen_bits:2048
openssl rsa -in backend/keys/private.pem -pubout -out backend/keys/public.pem
# 把 public.pem / private.pem 内容（去掉头尾标记、拼成单行）分别填入 .env 中的
# PROOFLY_AUTH_RSA_PUBLIC_KEY 和 PROOFLY_AUTH_RSA_PRIVATE_KEY

# 3. 启动基础设施
docker compose up -d mysql redis minio minio-init

# 4. 等待 MySQL 健康检查通过，然后初始化数据库
until docker compose exec -T mysql mysqladmin ping -h localhost -uroot -pproofly_dev --silent; do sleep 2; done
docker compose exec -T mysql mysql -uroot -pproofly_dev < docs/mysql-schema.sql
docker compose exec -T mysql mysql -uroot -pproofly_dev < docs/seed-dev.sql   # 可选：仅本地开发

# 5. 启动后端（⚠️ Spring 不会自动读取仓库根 .env，需要先把变量导入 shell）
cd backend
set -a && source ../.env && set +a && mvn spring-boot:run

# 6. 新终端启动前端
cd frontend && npm install && npm run dev
```

打开 <http://localhost:5173>：
- 用 `admin` / `admin123` 登录（仅种子账号，开发环境）
- 或在 `/register` 自助注册新门店

> 💡 IDE 用户：在 Run/Debug Configurations 的 Environment variables 中粘贴 `.env` 内容即可。

详细文档：[docs/quickstart.md](docs/quickstart.md)

## 📐 架构

```
┌──────────────────────────────────────────────────────────┐
│                      前端 (Vue 3)                        │
│  门店后台 SPA ──┬── 客户公开审稿页 (免登录 Token)         │
└────────┬────────┴───────────────────────┬───────────────┘
         │ /api/admin/** (JWT)            │ /api/public/** (Token)
┌────────▼────────────────────────────────▼───────────────┐
│                  后端 (Spring Boot 3)                    │
│  Controller → Service → DAO → MyBatis-Plus → MySQL       │
│              ↓                                           │
│  ├─ 鉴权 (JWT + Redis 黑名单 + RSA)                      │
│  ├─ 存储 (MinIO 预签名 URL)                              │
│  ├─ 缓存 (Redis Token)                                   │
│  └─ 定时任务 (套餐到期扫描)                              │
└────────┬────────────────────────────────┬───────────────┘
         │                                │
    ┌────▼─────┐    ┌──────┐    ┌─────────▼────────┐
    │  MySQL   │    │ Redis │    │      MinIO       │
    └──────────┘    └──────┘    └──────────────────┘
```

更多架构说明：[docs/system-module-list.md](docs/system-module-list.md)

## 📦 模块清单

| 模块 | 状态 | 说明 |
|------|------|------|
| M01 账号、门店与权限 | ✅ | RBAC + JWT |
| M02 审稿项目 | ✅ | 项目生命周期 |
| M03 设计稿版本 | ✅ | 自动递增 |
| M04 文件上传与 MinIO | ✅ | 预签名预览 |
| M05 客户审稿链接 | ✅ | 哈希 Token |
| M06 在线标注评论 | ✅ | 含语音 |
| M07 客户确认定稿 | ✅ | 幂等 |
| M08 审稿行为与确认记录 | ✅ | 审计日志 |
| M09 后台工作台 | ✅ | 状态看板 |
| M10 通知与提醒 | ✅ | 站内通知 |
| M11 系统配置 | ✅ | 品牌定制 |
| M12 部署运维 | 🔄 | 云原生方案设计中 |
| M13 套餐、用量与账单 | ✅ | 模拟 XPay + Webhook |
| M14 AI 辅助能力 | ⏸️ | 待规划 |

详细完成情况：[docs/module-completion.md](docs/module-completion.md)

## 🗂️ 目录结构

```
Proofly/
├── backend/                # Spring Boot 后端
│   ├── src/main/java/      # 业务代码
│   ├── src/main/resources/ # 配置 + Mapper
│   ├── Dockerfile
│   └── pom.xml
├── frontend/               # Vue 3 前端
│   ├── src/views/          # 页面（admin / auth / public）
│   ├── src/api/            # API 封装
│   ├── src/stores/         # Pinia 状态
│   ├── src/router/
│   ├── Dockerfile
│   ├── nginx.conf
│   └── package.json
├── docs/                   # 项目文档
│   ├── quickstart.md
│   ├── system-module-list.md
│   ├── module-completion.md
│   ├── database.md
│   ├── mysql-schema.sql    # 表结构 DDL
│   ├── seed-dev.sql        # 开发种子数据
│   └── ...
├── docker-compose.yml      # 一键启动依赖
├── .env.example            # 环境变量模板
├── LICENSE                 # GPL-3.0
└── README.md
```

## 🖼️ 截图

> 截图将在正式发布前补充。
>
> 占位计划：
> - 登录与注册
> - 工作台看板
> - 项目详情 + 标注
> - 客户审稿页
> - 套餐选择 + 模拟收银台

## 🤝 贡献

欢迎贡献！请先阅读 [CONTRIBUTING.md](CONTRIBUTING.md) 了解开发流程与代码规范。

提交 Issue：[GitHub Issues](https://github.com/liyu-1028/Proofly/issues)

## 📜 安全

发现安全漏洞？请阅读 [SECURITY.md](SECURITY.md) 反馈，**不要**在公开 Issue 中披露。

## ⚖️ 许可证

本项目采用 **GNU General Public License v3.0** — 详见 [LICENSE](LICENSE)。

> GPL-3.0 意味着你可以自由使用、修改、分发，但任何衍生作品也必须以 GPL-3.0 开源。

## 🙏 致谢

- [Element Plus](https://element-plus.org/) — Vue 3 组件库
- [Spring Boot](https://spring.io/projects/spring-boot) — 后端框架
- [MyBatis-Plus](https://baomidou.com/) — 持久层增强
- [MinIO](https://min.io/) — 对象存储
- [Redis](https://redis.io/) — 缓存
- 所有贡献者

## 📮 联系方式

- GitHub Issues：功能建议与 Bug 反馈
- 安全漏洞：见 [SECURITY.md](SECURITY.md)

---

Made with ❤️ by Proofly Contributors