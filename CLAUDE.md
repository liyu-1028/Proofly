# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

审稿宝（Proofly）是一套面向广告制作店、图文打印店、快印店、包装印刷店和设计工作室的在线审稿确认系统。核心闭环：

```
设计师创建项目 → 上传设计稿 → 生成审稿链接 → 客户标注修改意见 → 设计师上传新版 → 客户确认定稿 → 系统保存确认记录
```

**业务规则（不可违反）**：
- 设计稿每次上传生成独立版本，不覆盖历史文件
- 确认记录默认不可物理删除，优先设计为作废或隐藏
- 所有核心业务表（user、project、project_version、annotation、confirmation_record、file_object）必须包含 `store_id` 实现多租户隔离

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | JDK 17、Spring Boot 3.5.14、MyBatis-Plus 3.5.16 |
| 前端 | Vue 3.5、Vite 6、TypeScript、Element Plus 2.13 |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis 7.4.8（Docker 镜像 `redis:7.4.8-alpine`） |
| 对象存储 | MinIO（Docker 镜像 `minio/minio:RELEASE.2024-07-16T23-46-41Z`） |
| API | REST JSON |
| 认证 | JWT（jjwt 0.13.0） |

## 常用命令

### 后端

```bash
cd backend

# 本地开发启动
./mvnw spring-boot:run

# 打包
./mvnw package -DskipTests

# 运行测试
./mvnw test
```

### 前端

```bash
 cd frontend

# 安装依赖
npm install

# 开发模式启动
npm run dev

# 构建生产版本
npm run build

# 类型检查
vue-tsc -b
```

## 架构

### 后端分层

```
Controller → Service → DAO → MySQL
```

- **Controller**：处理 HTTP 入参、权限边界和响应，不直接调用 DAO
- **Service**：承载业务流程、事务和跨资源编排
- **DAO**：基于 MyBatis-Plus Mapper，复杂查询使用 `src/main/resources/mapper/*.xml`
- **Domain/DTO**：实体和请求/响应对象分离，不直接暴露表结构

### 后端包结构

```
backend/src/main/java/com/lyllink/proofly/
├── controller/    # HTTP API
├── service/       # 业务逻辑
├── dao/           # MyBatis-Plus Mapper
├── entity/        # 数据库实体
├── dto/           # 请求和响应 DTO
├── config/        # Spring 配置类
├── security/      # JWT 认证、Token 解析
└── common/        # 统一响应、异常处理
```

### 前端结构

```
frontend/src/
├── api/           # API client（统一封装请求错误处理）
├── assets/        # 静态资源
├── components/    # 通用组件
├── composables/   # 组合式逻辑
├── router/        # 路由（区分门店后台与客户审稿页）
├── stores/        # Pinia 状态管理
├── views/         # 页面组件
└── types/         # TypeScript 类型定义
```

### MinIO 对象路径规范

```
stores/{storeId}/projects/{projectId}/versions/{versionId}/{fileId}-{filename}
```

MySQL 只保存对象 key、文件名、大小、类型、校验信息和业务关系。

## 配置约定

按环境拆分配置文件：
- `application.yml` — 通用配置和默认值
- `application-dev.yml` — 本地 Docker Compose 依赖
- `application-test.yml` — 自动化测试
- `application-prod.yml` — 生产部署

数据库、Redis、MinIO、JWT、文件访问域名等敏感配置必须支持环境变量覆盖，命名格式：`${环境变量名:默认值}`。

## API 规范

- API 默认使用 REST JSON，统一响应结构
- 使用 `@Valid` 进行请求参数校验
- 异常统一处理，不在 Controller 层面直接抛给前端
- 客户公开访问链接接口与门店后台登录接口边界要清晰

## 前端交互要点

- 上传、预览、标注、版本切换、确认按钮是核心交互
- 门店后台突出项目状态、版本列表、标注列表和客户确认状态
- 客户审稿页降低操作复杂度，让客户直接看图、标注、提交、确认
- 关键状态要明确：待客户反馈、需修改、待确认、已确认、已归档
- Element Plus 主题色与青绿色主色保持一致（见 `docs/frontend-style-guide.md`）

## 开发注意事项

- 涉及 Figma 原型或前端页面实现时，必须阅读 `docs/system-module-list.md` 和 `docs/figma-design-system-rules.md`
- 修改前先确认当前文件是否已有用户改动，不要覆盖无关变更
- 优先实现 MVP 需要的功能，不做过度抽象
- Redis 用于会话、短链状态、限流，不用于关键业务确认记录（必须落库）
- 后续如接入 AI 能力（LangChain4j/Spring AI），优先作为独立模块（如 `ai/`）接入，不侵入审稿主流程