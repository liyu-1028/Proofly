# AGENT.md

本文档是 Codex 在 `Proofly` 项目中进行全栈开发时的协作规范。任何开发任务开始前，先阅读 `README.md` 和本文档，再检查当前目录结构与已有实现。

## 项目目标

产品中文名：审稿宝。

审稿宝是面向广告制作店、图文打印店、快印店、包装印刷店和设计工作室的在线审稿确认系统。MVP 的核心闭环是：

```text
设计师创建项目
→ 上传设计稿
→ 生成客户审稿链接
→ 客户在线查看并标注修改意见
→ 设计师上传新版
→ 客户确认定稿
→ 系统保存确认记录
```

开发时优先保证审稿、标注、版本、确认、留痕主流程可靠。

## 默认技术约定

| 方向 | 默认方案 |
| --- | --- |
| 后端 | JDK 17、Spring Boot 3.5.x |
| 前端 | Vue 3、Vite、TypeScript |
| API | REST JSON |
| 数据访问 | MyBatis-Plus Mapper，复杂查询可使用 MyBatis XML |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis |
| 对象存储 | MinIO |
| 部署 | Docker、Docker Compose |

除非用户明确要求变更技术栈，否则按以上方案推进。

版本策略：

- JDK 固定使用 17，不使用更低版本。
- Spring Boot 固定使用 3.5.x 最新补丁版本，初始化工程时不要直接选择 Spring Boot 4.x。
- MySQL 固定使用 8.0，开发、测试、生产保持同一主版本。
- 后续如接入 LangChain4j、Spring AI 或其他 AI 问答能力，优先作为独立模块接入，不侵入审稿、标注、确认主流程。

## Codex 工作规则

- 开始任务前先阅读 `README.md`、`AGENT.md` 和相关源码。
- 涉及 Figma 原型或前端页面实现时，必须同时阅读 `docs/system-module-list.md` 和 `docs/figma-design-system-rules.md`。
- 修改前先确认当前文件是否已有用户改动，不要覆盖无关变更。
- 优先实现 MVP 需要的功能，不做过度抽象。
- 不随意引入大型依赖；确需引入时说明用途，并优先选择生态成熟、维护活跃的库。
- 保持目录、命名、接口和错误处理风格一致。
- 涉及业务规则时，以“设计稿版本不可覆盖、确认记录不可随意删除”为硬约束。
- 涉及配置时，优先使用环境变量覆盖，不把门店私有配置写死到代码或提交到仓库。
- 完成代码变更后，尽量运行相关测试、构建或静态检查；如果无法运行，需要在最终说明中写明原因。

## 后端约定

后端默认使用 Spring Boot 分层结构，数据访问层统一命名为 Mapper：

```text
backend/
├── src/main/java/
│   └── .../
│       ├── controller/    # HTTP API
│       ├── service/       # 业务逻辑
│       ├── mapper/        # MyBatis-Plus / MyBatis 数据访问
│       ├── domain/        # 实体和值对象
│       ├── dto/           # 请求和响应 DTO
│       ├── config/        # 配置
│       └── common/        # 通用返回、异常、工具
├── src/main/resources/
│   ├── mapper/            # 复杂 SQL 的 MyBatis XML
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-test.yml
│   └── application-prod.yml
└── src/test/java/
```

默认调用链：

```text
Controller -> Service -> Mapper -> MySQL
```

Controller 不直接调用 Mapper。事务、版本变更、确认留痕、文件与数据库的一致性处理都放在 Service 层。

### 后端依赖基线

初始化后端工程时，优先使用以下依赖：

```text
spring-boot-starter-web
spring-boot-starter-validation
spring-boot-starter-security
mybatis-plus-spring-boot3-starter
mysql-connector-j
spring-boot-starter-data-redis
minio
springdoc-openapi-starter-webmvc-ui
lombok
mapstruct
spring-boot-starter-test
```

复杂权限可以后续逐步增强，但安全依赖和接口边界应尽早保留。DTO 转换可用 MapStruct，也可以在早期保持手写转换；不要为了少量字段引入复杂映射层。

AI 相关依赖不要在 MVP 基线中提前强行加入。只有在实现智能问答、智能审稿建议、知识库检索、自动总结等功能时，再引入 LangChain4j 或 Spring AI，并将相关代码放在独立包中，例如 `ai/`、`assistant/` 或 `knowledge/`。

### API 与错误处理

- API 默认使用 REST JSON。
- 请求对象和响应对象使用 DTO/VO，不直接暴露数据库实体。
- 使用统一响应结构和统一异常处理。
- 参数校验放在请求 DTO 和业务服务层中。
- 需要兼顾客户公开访问链接和门店后台登录访问，两类接口边界要清晰。

### 配置文件约定

- `application.yml` 放通用配置和默认值。
- `application-dev.yml` 面向本地开发，默认连接 Docker Compose 中的 MySQL、Redis、MinIO。
- `application-test.yml` 面向自动化测试。
- `application-prod.yml` 面向真实部署，不提交真实密钥。
- 数据库、Redis、MinIO、JWT、文件访问域名、部署模式等都必须支持环境变量覆盖。

配置命名建议：

```yaml
proofly:
  deployment-mode: ${PROOFLY_DEPLOYMENT_MODE:single-store}
  default-store-id: ${PROOFLY_DEFAULT_STORE_ID:}
```

`deployment-mode` 预留两种值：

| 值 | 用途 |
| --- | --- |
| `single-store` | 单家打印店或设计室私有部署 |
| `multi-tenant` | 多门店共用一套平台服务 |

### 数据与业务规则

- 审稿项目属于门店，核心业务表从第一版开始保留 `store_id`。
- `user`、`project`、`project_version`、`annotation`、`confirmation_record`、`file_object` 等表都要能按门店隔离。
- 设计稿每次上传都创建新版本，不覆盖旧版本。
- 标注评论必须绑定具体项目和具体设计稿版本。
- 客户确认必须绑定具体版本，并记录确认时间、确认人信息、访问来源等关键留痕。
- 确认记录默认不可物理删除；如需删除，优先设计为作废或隐藏。
- 单店部署也使用 `store_id`，可由 `proofly.default-store-id` 或初始化数据提供默认门店。

### Redis 使用方向

- 登录会话或 token 状态。
- 客户审稿短链或访问令牌状态。
- 临时验证码、限流、轻量任务状态。
- 不把关键业务确认记录只放在 Redis，必须落库。

### MinIO 使用方向

- 存储设计稿原文件、预览图、导出确认单等对象。
- 对象路径建议包含门店、项目、版本维度，例如：

```text
stores/{storeId}/projects/{projectId}/versions/{versionId}/{filename}
```

- MySQL 只保存对象 key、文件名、大小、类型、校验信息和业务关系。
- 删除文件前必须确认业务规则，避免破坏历史版本和确认留痕。
- 对象 bucket 可以按部署环境区分，业务隔离主要依赖对象 key 中的 `storeId`。

## 前端约定

前端默认使用 Vue 3、Vite、TypeScript。

推荐结构：

```text
frontend/
├── src/
│   ├── api/          # API client
│   ├── assets/       # 静态资源
│   ├── components/   # 通用组件
│   ├── composables/  # 组合式逻辑
│   ├── router/       # 路由
│   ├── stores/       # 状态管理
│   ├── views/        # 页面
│   └── types/        # TypeScript 类型
└── tests/
```

### 页面与交互

- 门店后台和客户审稿页要区分路由与布局。
- 上传、预览、标注、版本切换、确认按钮是核心交互，优先保证稳定可用。
- 客户审稿页应尽量降低操作复杂度，让客户能直接看图、标注、提交、确认。
- 设计师后台应突出项目状态、版本列表、标注列表和客户确认状态。

### API Client

- API 请求集中在 `src/api/`。
- TypeScript 类型集中复用，避免页面内重复定义接口结构。
- 错误提示由统一请求层或页面边界处理，不在各处重复拼接。

### UI 设计原则

- 面向门店高频工作流，界面应清晰、克制、易扫描。
- 不做营销型首页，优先构建可操作的后台和审稿页面。
- 关键状态要明确：待客户反馈、需修改、待确认、已确认、已归档。

## Docker 与环境

本地开发优先使用 Docker Compose 管理基础依赖：

- MySQL：业务数据。
- Redis：缓存和临时状态。
- MinIO：设计稿和导出文件。

建议在 `docker/` 下维护开发环境配置，并在 README 中补充启动命令。敏感配置不要提交真实密钥，可使用 `.env.example` 提供示例。

建议保留这些文件：

```text
docker/docker-compose.dev.yml
docker/docker-compose.prod.example.yml
.env.example
```

开发阶段后端和前端可以本机启动，MySQL、Redis、MinIO 由 Docker Compose 启动。生产部署再决定是否把后端、前端也纳入 Compose。

## 测试与验收

优先覆盖核心业务路径：

- 设计师创建审稿项目。
- 上传设计稿并生成版本。
- 客户通过链接访问审稿页。
- 客户提交标注评论。
- 设计师上传新版本。
- 客户确认指定版本。
- 系统保存并展示确认记录。

后端优先补充 service 层测试和关键接口测试。前端优先补充核心页面组件、API client 和关键交互测试。

## 文档维护

- 新增重要模块时，同步更新 README 或 `docs/`。
- 新增环境变量时，同步更新 `.env.example` 和部署说明。
- 新增接口约定时，优先放入 `docs/api.md` 或对应模块文档。
- 产品规则发生变化时，先更新文档，再实现代码。
