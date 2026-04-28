# 审稿宝（Proofly）

审稿宝是一套面向广告制作店、图文打印店、快印店、包装印刷店和设计工作室的在线审稿确认系统。

它解决广告打印行业中常见的审稿沟通问题：客户在微信里说“往左一点”“字大一点”，设计师需要反复截图、标记、确认，版本容易混乱，最终客户确认留痕也不完整。

审稿宝的核心目标是：

> 设计师上传设计稿，客户通过链接在线查看、标注修改意见，并最终点击“确认定稿”，系统自动保存确认记录。

## 目标用户

| 角色 | 使用场景 |
| --- | --- |
| 门店老板 | 查看所有审稿项目、跟踪订单审稿状态、管理门店成员 |
| 设计师 | 创建审稿项目、上传设计稿、查看客户修改意见、上传新版本 |
| 客户 | 打开审稿链接、在线查看设计稿、标注修改意见、确认定稿 |
| 管理员 | 管理门店、员工、套餐、系统配置和基础数据 |

## 核心痛点

- 客户反馈表达不清楚，设计师需要反复沟通确认。
- 修改意见散落在微信、QQ、电话和截图里，不方便统一管理。
- 设计稿版本混乱，难以确认客户最终认可的是哪一版。
- 客户后续反悔时，门店缺少明确的确认记录。
- 门店老板无法快速了解每个审稿项目的状态和责任人。

## 标准审稿流程

```text
设计师创建审稿项目
→ 上传设计稿
→ 系统生成审稿链接
→ 设计师将链接发送给客户
→ 客户打开链接查看设计稿
→ 客户在线标注修改意见
→ 设计师根据意见修改设计稿
→ 设计师上传新版设计稿
→ 客户再次查看
→ 客户确认定稿
→ 系统保存确认记录
```

## MVP 功能模块

### 审稿项目

- 创建、编辑、归档审稿项目。
- 记录项目名称、客户信息、负责人、状态和备注。
- 支持按门店、设计师、状态筛选项目。

### 设计稿版本

- 每次上传生成独立版本，不覆盖历史文件。
- 支持查看版本列表、当前版本和历史版本。
- 记录上传人、上传时间、版本说明和文件信息。

### 文件上传与预览

- 支持常见设计稿文件的上传与在线预览。
- 文件原始数据存储在 MinIO。
- 文件元数据和业务关系存储在 MySQL。

### 在线标注评论

- 客户可在设计稿上标注位置并填写修改意见。
- 设计师可查看、处理和回复标注。
- 标注内容与具体设计稿版本绑定。

### 客户确认

- 客户可对指定版本点击“确认定稿”。
- 确认动作需要记录客户信息、确认时间、确认版本和访问来源。
- 已确认版本作为后续争议处理和生产依据。

### 确认记录

- 保存确认日志和关键审稿行为。
- 确认记录默认不可随意删除。
- 后续可扩展为确认单、PDF 导出或电子签名。

### 门店与员工管理

- 支持门店、员工、角色和权限管理。
- MVP 阶段优先保证门店内部协作闭环。
- 后续可扩展套餐、用量、账单和多租户能力。

## 默认技术栈

| 层级 | 技术 |
| --- | --- |
| 后端 | JDK 17、Spring Boot 3.5.x |
| 后端分层 | Controller、Service、Mapper、Domain/DTO |
| 数据访问 | MyBatis-Plus，复杂查询可补充 MyBatis XML |
| 前端 | Vue 3、Vite、TypeScript |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis |
| 对象存储 | MinIO |
| 部署 | Docker、Docker Compose |
| API 风格 | REST JSON |

## 架构与配置建议

### 版本选择

- JDK 固定使用 17。它是 Spring Boot 3.x 的稳定基线，也满足后续接入 LangChain4j 等 AI 能力的运行要求。
- Spring Boot 使用 3.5.x 最新补丁版本。该版本线兼顾长期维护、生态兼容和 AI 组件接入，不建议 MVP 阶段直接使用 Spring Boot 4.x。
- MySQL 固定使用 8.0。开发、测试和部署环境尽量保持同一主版本，避免 SQL、排序规则和时间字段行为不一致。
- 后续如接入 LangChain4j、Spring AI 或其他大模型问答能力，优先以独立业务模块接入，不影响审稿主流程。

### 后端分层

后端默认采用 Spring Boot 分层架构：

```text
Controller
→ Service
→ Mapper
→ MySQL
```

- Controller 只处理 HTTP 入参、权限边界和响应。
- Service 承载业务流程、事务和跨资源编排。
- Mapper 负责数据库访问，默认使用 MyBatis-Plus。
- 复杂统计、列表筛选、关联查询可使用 MyBatis XML 补充。
- DTO/VO 与数据库实体分离，避免直接暴露表结构。

### 推荐后端依赖

初始化 Spring Boot 工程时，建议优先加入以下依赖：

```text
spring-boot-starter-web
spring-boot-starter-validation
spring-boot-starter-security
mybatis-plus-spring-boot3-starter
mysql-connector-j
spring-boot-starter-data-redis
minio
langchain4j-spring-boot-starter（后续 AI 功能需要时再引入）
springdoc-openapi-starter-webmvc-ui
lombok
mapstruct
spring-boot-starter-test
```

MVP 阶段可以先弱化 `spring-boot-starter-security` 的复杂权限模型。`langchain4j-spring-boot-starter` 不在首版强制引入，等 AI 问答、智能审稿建议、知识库检索等功能进入开发时再加入。

### 环境配置

配置文件建议按环境拆分：

```text
application.yml
application-dev.yml
application-test.yml
application-prod.yml
```

- `application.yml` 放通用配置和默认值。
- `application-dev.yml` 面向本地 Docker Compose 依赖。
- `application-test.yml` 面向自动化测试。
- `application-prod.yml` 面向真实门店或平台部署。
- 数据库、Redis、MinIO、JWT、文件访问域名等敏感配置必须支持环境变量覆盖。

示例：

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/proofly}
    username: ${DB_USERNAME:proofly}
    password: ${DB_PASSWORD:proofly123}
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}

minio:
  endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
  access-key: ${MINIO_ACCESS_KEY:minioadmin}
  secret-key: ${MINIO_SECRET_KEY:minioadmin}
  bucket: ${MINIO_BUCKET:proofly}

proofly:
  deployment-mode: ${PROOFLY_DEPLOYMENT_MODE:single-store}
  default-store-id: ${PROOFLY_DEFAULT_STORE_ID:}
```

### 多门店部署预留

审稿宝后续可能部署到多家打印店或设计室，开发阶段需要预留两种模式：

| 模式 | 说明 |
| --- | --- |
| `single-store` | 单店私有部署，一家门店独立使用一套系统 |
| `multi-tenant` | 平台化部署，多家门店共用一套系统 |

从第一版数据模型开始，核心业务表建议包含 `store_id`：

- `user`
- `project`
- `project_version`
- `annotation`
- `confirmation_record`
- `file_object`

即使 MVP 阶段只服务一家店，也不要省略 `store_id`。这样后续扩展到多门店、连锁店或 SaaS 平台时，不需要大规模重构数据结构。

## 推荐目录结构

```text
Proofly/
├── backend/        # Spring Boot 后端工程
├── frontend/       # Vue 3 前端工程
├── docs/           # 产品、接口、数据库和部署文档
├── docker/         # Docker Compose、镜像和环境配置
├── scripts/        # 本地开发、构建、部署辅助脚本
├── AGENT.md        # Codex 开发协作规范
└── README.md       # 项目说明文档
```

## 本地开发

当前仓库处于项目启动阶段，前后端工程尚未初始化。初始化后建议补充以下命令：

```bash
# 启动基础依赖
docker compose -f docker/docker-compose.dev.yml up -d

# 启动后端
cd backend
./mvnw spring-boot:run

# 启动前端
cd frontend
npm install
npm run dev
```

## 部署方向

MVP 阶段优先使用 Docker Compose 管理运行环境：

- `backend`：Spring Boot API 服务。
- `frontend`：Vue 3 构建后的静态资源，可由 Nginx 托管。
- `mysql`：业务数据持久化。
- `redis`：缓存、会话、短链状态和轻量任务状态。
- `minio`：设计稿文件、预览文件和导出文件存储。

MinIO 对象路径建议从一开始带门店、项目和版本维度：

```text
stores/{storeId}/projects/{projectId}/versions/{versionId}/{fileId}-{filename}
```

## 开发原则

- 先完成“上传设计稿 → 客户标注 → 上传新版 → 客户确认 → 留痕”的闭环。
- 设计稿版本不可覆盖，确认记录不可随意删除。
- 业务流程优先清晰可靠，复杂协作、计费、电子签名等能力后续迭代。
- 默认按多门店、多角色、多版本的方向设计，但 MVP 实现保持轻量。
- 配置从开发阶段就支持环境变量覆盖，避免为不同门店部署时修改代码。

## 当前状态

- [x] 创建项目基础目录。
- [x] 生成 README 和 AGENT 开发规范。
- [ ] 初始化 Spring Boot 后端工程。
- [ ] 初始化 Vue 3 前端工程。
- [ ] 编写 Docker Compose 本地依赖配置。
- [ ] 设计核心数据模型和接口。
