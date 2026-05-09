# 审稿宝模块完成说明

本文档记录每个系统模块的完成状态、已完成内容、未完成内容和交接说明。它面向所有开发智能体使用，不偏向前端或后端。

完成状态以 `docs/system-module-list.md` 为准。每完成一个模块，需要同步更新：

- `docs/system-module-list.md` 中对应模块的完成状态和完成记录。
- 本文档中对应模块的完成说明。
- 如涉及接口、数据结构、环境变量或运行方式，还需要同步更新相关 `docs/` 文档；每次新增、修改或删除接口时，必须同步更新对应模块接口文档。接口文档文件名使用 `docs/api-mXX-module-name.md` 格式，每完成一个新模块功能时创建或更新该模块的独立接口文档。

## 状态定义

| 状态 | 含义 |
| --- | --- |
| 未开始 | 尚无可运行的业务实现，可能只有规划、数据结构或占位代码 |
| 进行中 | 已完成部分能力，但还没有达到模块验收标准 |
| 已完成 | 已达到模块验收标准，可以作为后续模块依赖 |

## 当前模块状态总览

| 模块 | 状态 | 当前结论 |
| --- | --- | --- |
| M01 账号、门店与权限 | 已完成 | 登录、基础鉴权、后端管理接口、前端管理功能及基于角色的访问控制已全部完成 |
| M02 审稿项目 | 已完成 | 项目生命周期管理的后端接口和前端页面已全部实现，包括创建、编辑、列表、详情及归档/恢复 |
| M03 设计稿版本 | 已完成 | 版本上传、递增管理及前端预览切换功能已全部完成 |
| M04 文件上传、存储与预览 | 已完成 | 已集成 MinIO 存储，完成文件元数据持久化及受控预览 URL 生成功能 |
| M05 客户审稿链接 | 已完成 | 审稿链接生成、哈希存储及管理（禁用/恢复/删除）后端接口已全部完成 |
| M06 在线标注评论 | 已完成 | 客户提交标注、设计师查看处理标注、预览图定位展示及状态处理前后端已全部完成 |
| M07 客户确认定稿 | 已完成 | 客户确认定稿幂等接口、项目状态自动变更、确认记录持久化及前端确认入口已全部完成 |
| M08 审稿行为与确认记录 | 已完成 | 通用审计日志写入服务、项目全生命周期时间线查询后端及项目详情时间线展示已全部完成 |
| M09 后台工作台与状态看板 | 已完成 | 项目状态统计、最近项目及近期动态展示的前后端代码已全部完成 |
| M10 通知与提醒 | 未开始 | 已有数据设计，暂无业务实现 |
| M11 系统配置与基础数据 | 未开始 | 已有数据设计，暂无业务实现 |
| M12 部署、运维与开放接口 | 进行中 | 后端骨架、健康检查、模块接口文档和数据文档已完成，部署文档仍不完整 |
| M13 套餐、用量与账单 | 未开始 | MVP 暂不实现 |
| M14 AI 辅助能力 | 未开始 | 未开始 |

## M01 账号、门店与权限

状态：已完成。

模块目标：

- 建立后台用户、门店、角色和权限边界。
- 支持门店后台用户登录、退出、刷新 Token、获取当前用户。
- 为后续项目、文件、标注、确认等后台接口提供统一登录态和 `store_id` 隔离依据。
- 实现基于角色的前端访问控制（RBAC）。

已完成：

- 后台登录接口：`POST /api/auth/login`。
- 后台刷新 Token 接口：`POST /api/auth/refresh`。
- 后台登出接口：`POST /api/auth/logout`。
- 当前用户接口：`GET /api/auth/me`。
- 当前门店接口：`GET /api/admin/stores/current`。
- 更新当前门店接口：`PUT /api/admin/stores/current`。
- 员工列表接口：`GET /api/admin/users`。
- 创建员工接口：`POST /api/admin/users`。
- 员工详情接口：`GET /api/admin/users/{userId}`。
- 更新员工接口：`PUT /api/admin/users/{userId}`。
- 更新员工状态接口：`PATCH /api/admin/users/{userId}/status`。
- 重置员工密码接口：`POST /api/admin/users/{userId}/reset-password`。
- 前端登录页：`frontend/src/views/auth/LoginView.vue`。
- 前端认证 API 封装：`frontend/src/api/auth.ts`。
- 前端后台管理 API 封装：`frontend/src/api/admin.ts`。
- 前端会话状态：`frontend/src/stores/session.ts`，支持 token 持久化、刷新、权限判定（`isAdmin`, `canManageStaff`）。
- 前端后台路由守卫：`frontend/src/router/index.ts`，支持基于角色的路由拦截（如：只有 `admin` 或 `owner` 可访问员工管理）。
- 前端导航菜单：`frontend/src/App.vue`，根据用户角色动态显示/隐藏菜单项。
- 前端员工管理页：`frontend/src/views/admin/staff/StaffView.vue`，支持 CRUD 操作。
- 前端系统设置页：`frontend/src/views/admin/settings/SettingsView.vue`，支持门店资料编辑。
- JWT + Redis 的 access token 和 refresh token 会话机制.
- Redis 黑名单，用于登出后使 access token 失效。
- Spring Security 基础鉴权过滤器.
- `/api/health`、`/api/public/**`、OpenAPI 文档路径放行.
- `CurrentUser` 上下文，包含 `userId`、`storeId`、`roles`、`tokenId`。
- `user`、`role`、`user_role`、`store` 的实体和数据访问 Mapper.
- `docs/auth-seed.sql`，提供本地默认门店、角色和 `admin` 账号。
- 基础异常响应和 JSON 格式 `401/403`。

未完成：

- 平台级门店创建、停用等跨门店管理能力（非 MVP 核心）。
- 修改密码、忘记密码、验证码、登录失败锁定策略的完整实现。
- 客户审稿链接的访问令牌校验。

交接说明：

- 后续任何后台模块都应通过当前登录态获取 `storeId`，不能由前端直接传入可信 `store_id`。
- 后台请求需要使用 `Authorization: Bearer <accessToken>`。
- 只有 `admin` 和 `owner` 角色拥有“员工管理”权限。
- 前端新增受限页面时，应在 `router/index.ts` 的 `meta.requiredRoles` 中定义准入角色。
- 本地开发前需要执行 `docs/mysql-schema.sql` 和 `docs/auth-seed.sql`。

完成判定：

- 门店员工可登录后台。
- 角色权限生效：`admin` 和 `owner` 可管理员工，`designer` 无法看到且无法进入员工管理页面。
- 后台接口能识别当前用户和门店并进行权限校验（403）。
- 用户停用或门店停用时不能继续访问。

## M02 审稿项目

状态：已完成。

模块目标：

- 管理审稿项目生命周期。
- 为版本、文件、标注、确认记录提供业务主容器。

已完成：

- 创建项目接口：`POST /api/admin/projects`。
- 更新项目接口：`PUT /api/admin/projects/{projectId}`。
- 项目列表接口：`GET /api/admin/projects`。
- 项目详情接口：`GET /api/admin/projects/{projectId}`。
- 归档项目接口：`PATCH /api/admin/projects/{projectId}/archive`。
- 恢复项目接口：`PATCH /api/admin/projects/{projectId}/restore`。
- 前端项目接口封装：`frontend/src/api/projects.ts`。
- 前端项目列表页：`frontend/src/views/admin/projects/ProjectsView.vue`。
- 前端项目详情页：`frontend/src/views/admin/projects/ProjectDetailView.vue`。
- 前端项目类型定义：`frontend/src/types/project.ts`。
- 前端路由：`/admin/projects` 和 `/admin/projects/:projectId`。
- 项目状态流转日志记录：`project_status_log`。
- 基于 `store_id` 的数据隔离和多租户支持.
- 项目负责人校验逻辑。

未完成：

- 项目状态随版本、确认等业务动作自动流转的逻辑（将在后续模块中实现）。

交接说明：

- 每次创建或关键状态变更都会记录到 `project_status_log`。
- 项目列表支持按关键字、状态和负责人筛选.
- 前端负责人下拉使用 M01 员工列表接口提供选项；如果员工接口不可用，会回退显示当前登录用户。
- 归档后的项目不允许编辑。

完成判定：

- 登录用户能创建和查看自己门店下的项目。
- 项目详情能展示客户信息、负责人、状态、当前版本和确认状态。
- 项目状态流转有记录。

## M03 设计稿版本

状态：已完成。

模块目标：

- 管理项目下的设计稿版本。
- 确保每次上传都是独立版本，历史版本不可覆盖。
- 自动维护版本号递增。

已完成：

- 版本上传接口：`POST /api/admin/projects/{projectId}/versions`。
- 版本列表接口：`GET /api/admin/projects/{projectId}/versions`。
- 版本元数据与文件元数据的事务性绑定.
- 前端版本列表展示与预览切换。
- 上传新版时自动更新项目 `current_version_id`。
- 项目状态自动流转逻辑：上传第一版后 `draft` 变更为 `waiting_feedback`。

未完成：

- 锁定已确认版本，防止在此基础上继续上传（需 M07 确认定稿模块配合）。

交接说明：

- `version_no` 在项目内自动递增，前端无需传递。
- 只有未归档、未定稿的项目允许上传新版本。

完成判定：

- 同一项目可以有多个版本.
- 切换历史版本时，文件、标注和确认状态跟随版本变化.
- 已确认版本有明确标识并受保护.

## M04 文件上传、存储与预览

状态：已完成。

模块目标：

- 上传设计稿文件.
- 保存文件元数据。
- 将文件内容存入 MinIO.
- 提供在线预览所需的数据和访问能力。

已完成：

- MinIO 客户端集成与 Spring Bean 配置.
- `putObject` 流式上传功能封装.
- 预签名预览 URL 生成（有效期 1 小时）。
- 文件元数据表 `file_object` 持久化.
- 结构化存储路径：`stores/{storeId}/projects/{projectId}/versions/{versionId}/{fileId}-{filename}`。
- 前端 `http.ts` 适配 `FormData` 格式，支持多部分文件上传.

未完成：

- 文件上传后的病毒扫描或高级格式校验（MVP 可选）。
- 生成图片预览图（缩略图），目前直接使用原图预览。

交接说明：

- 预览 URL 由后端实时生成，不应存储在数据库中以防过期。
- 存储路径严格遵循门店和项目隔离规范.

完成判定：

- 可上传图片并保存文件元数据.
- 可通过授权路径预览文件.
- 文件和版本关系正确。

## M05 客户审稿链接

状态：已完成.

模块目标：

- 生成客户免登录访问链接.
- 控制客户只能访问被授权项目。
- 记录客户访问行为。

已完成：

- 创建审稿链接接口：`POST /api/admin/projects/{projectId}/review-links`。
- 审稿链接列表接口：`GET /api/admin/projects/{projectId}/review-links`。
- 禁用链接接口：`PATCH /api/admin/review-links/{linkId}/disable`。
- 启用链接接口：`PATCH /api/admin/review-links/{linkId}/enable`。
- 删除链接接口：`DELETE /api/admin/review-links/{linkId}`。
- 安全策略：采用随机 32 位 Token，数据库仅存储 SHA-256 哈希值，确保即使数据库泄露也无法还原明文 Token。
- 实体与 Mapper：`ReviewLinkEntity`, `ReviewLinkMapper`。
- 访问日志准备：`ReviewAccessLogEntity`, `ReviewAccessLogMapper`（待公开接口模块调用）。

未完成：

- 前端生成与管理审稿链接的界面。
- 审稿链接的公开访问校验逻辑（将在 M06/M07 公开 API 部分实现）。

交接说明：

- `ReviewLinkService.generateLink` 在响应中返回一次明文 `token` 和完整 `url`，前端需提示用户立即复制。
- 完整的审稿链接基准地址在 `ProoflyProperties` 中配置。

## M06 在线标注评论

状态：已完成.

模块目标：

- 让客户在设计稿上提交位置化修改意见.
- 让设计师查看、回复和处理标注。

已完成：

- 客户提交标注接口：`POST /api/public/reviews/{token}/annotations`。
- 管理端查询标注接口：`GET /api/admin/projects/{projectId}/versions/{versionId}/annotations`。
- 管理端解决/忽略标注接口：`PATCH .../status`。
- 标注回复逻辑：`AnnotationCommentEntity` 及对应 Mapper。
- 状态联动：提交标注后，项目状态自动变更为 `change_requested`。
- 审计日志：提交和处理标注均自动记录到 `audit_log`。
- 前端 API 封装：`frontend/src/api/annotations.ts`。
- 客户审稿页：`frontend/src/views/public/review/ReviewView.vue` 支持图片点击点位标注、填写意见并提交。
- 管理端项目详情页：`frontend/src/views/admin/projects/ProjectDetailView.vue` 支持按当前版本加载标注、在预览图上定位展示，并将标注标记为已处理或忽略。

未完成：

- 标注回复的 Controller 接口（MVP 暂通过 resolve 接口覆盖核心流转）。

## M07 客户确认定稿

状态：已完成.

模块目标：

- 让客户确认指定版本为最终定稿.
- 保存可追溯的确认记录。

已完成：

- 客户确认接口：`POST /api/public/reviews/{token}/confirmations`。
- 管理端查询确认详情接口：`GET /api/admin/projects/{projectId}/confirmation`。
- 业务校验：防止重复确认，防止对归档项目确认。
- 状态流转：确认成功后，项目状态变更为 `confirmed`，版本标记为 `is_confirmed`。
- 留痕记录：记录确认人的 IP、User-Agent、时间及来源链接。
- 前端 API 封装：`frontend/src/api/confirmations.ts`。
- 客户审稿页确认入口：`frontend/src/views/public/review/ReviewView.vue`。
- 管理端项目详情页确认记录区：`frontend/src/views/admin/projects/ProjectDetailView.vue`。

未完成：

- 确认单导出。

## M08 审稿行为与确认记录

状态：已完成.

模块目标：

- 保存审稿关键行为.
- 为项目追踪和争议处理提供依据。

已完成：

- 通用审计服务：`AuditLogService`。
- 项目时间线查询接口：`GET /api/admin/projects/{projectId}/timeline`。
- 自动埋点：项目创建、上传版本、提交标注、处理标注、客户确认等动作均已接入自动日志记录。
- 数据模型：`AuditLogEntity`、`AuditLogMapper`。
- 前端 API 封装：`frontend/src/api/audit.ts`。
- 管理端项目详情页时间线展示：`frontend/src/views/admin/projects/ProjectDetailView.vue`。

未完成：

- 确认单导出。

## M09 后台工作台与状态看板

状态：已完成。

模块目标：

- 汇总门店项目状态、待办和近期活动。

已完成：

- 工作台统计接口：`GET /api/admin/dashboard/stats`，包含状态计数、最近项目和审计日志。
- 前端工作台页面：`frontend/src/views/admin/dashboard/DashboardView.vue`，展示关键指标、最近项目列表和近期动态。
- 前端统计接口封装：`frontend/src/api/dashboard.ts`。
- 后端统计服务：`DashboardService`，高效聚合门店数据。

未完成：

- 我的待处理项目（MVP 暂通过状态统计覆盖）。
- 更详细的报表统计（如：按设计师维度的统计）。

交接说明：

- 工作台数据实时从 MySQL 聚合，暂未引入 Redis 缓存。
- 最近项目显示最近更新的前 5 个项目。
- 近期动态显示最近产生的 10 条审计日志。

完成判定：

- 设计师可看到自己负责的待处理项目。
- 门店老板可看到门店整体审稿状态。

## M10 通知与提醒

状态：未开始.

模块目标：

- 在客户反馈、上传新版、客户确认等节点产生提醒。

已完成：

- 已在 `docs/database.md` 和 `docs/mysql-schema.sql` 中设计 `notification`。

未完成：

- 站内通知生成.
- 未读通知查询。
- 提醒文案生成.
- 第三方通知渠道适配.

交接说明：

- MVP 可先实现站内通知.
- 第三方渠道需要独立适配器，不应侵入主业务流程.

完成判定：

- 关键动作能生成通知.
- 用户能查看未读和已读通知.

## M11 系统配置与基础数据

状态：未开始.

模块目标：

- 管理文件大小、允许文件类型、审稿链接有效期等业务配置.

已完成：

- 已在 `docs/database.md` 和 `docs/mysql-schema.sql` 中设计 `system_config`。
- 后端配置文件已支持数据库、Redis、MinIO、鉴权相关环境变量覆盖.

未完成：

- 配置查询和更新接口.
- 配置读取服务.
- 文件上传规则和审稿链接有效期等业务参数落库使用。

交接说明：

- 环境级配置继续放在 `application-*.yml` 和环境变量中。
- 业务级配置可从 `system_config` 读取。

完成判定：

- 管理员可调整常用业务参数.
- 业务模块能读取并应用配置.

## M12 部署、运维与开放接口

状态：进行中.

模块目标：

- 支撑本地开发、测试、部署、健康检查和接口协作。

已完成：

- 后端 Spring Boot 工程骨架。
- Maven 使用约定：`backend/MAVEN.md`。
- 健康检查接口：`GET /api/health`。
- OpenAPI 依赖和基础路径配置.
- MySQL 数据设计文档：`docs/database.md`。
- MySQL 建表 SQL：`docs/mysql-schema.sql`。
- 认证基础数据 SQL：`docs/auth-seed.sql`。
- M01 模块接口文档：`docs/api-m01-account-store-permission.md`。
- M12 模块接口文档：`docs/api-m12-deployment-ops-openapi.md`。
- README 和 AGENT 中已明确 Redis、MinIO 推荐版本。

未完成：

- Docker Compose 开发环境文件.
- `.env.example`。
- 随业务模块继续完善具体接口文档.
- 部署说明和生产配置样例.
- 自动化数据库迁移工具集成.

交接说明：

- 当前本地依赖需要开发者自行启动 MySQL、Redis、MinIO。
- 后续若引入 Flyway 或 Liquibase，需要明确迁移文件目录和执行顺序.
- OpenAPI 已具备依赖基础，但业务接口文档仍需要随模块完善。

完成判定：

- 新开发者能按文档一键启动基础依赖和后端.
- API 文档能展示主要接口.
- 数据库初始化和环境变量配置有明确流程。

## M13 套餐、用量与账单

状态：未开始.

模块目标：

- 为后续 SaaS 化和商业化预留.

已完成：

- MVP 数据模型保留 `store_id`，便于后续按门店统计用量。

未完成：

- 套餐、订阅、用量、账单等所有业务能力。

交接说明：

- MVP 主流程不应写死套餐限制.
- 后续如增加用量限制，应通过独立策略服务判断.

完成判定：

- 可管理套餐和订阅.
- 可统计门店用量并生成账单记录。

## M14 AI 辅助能力

状态：未开始.

模块目标：

- 在审稿主流程稳定后，提供智能总结、沟通文案、智能建议等能力。

已完成：

- README 和 AGENT 已说明 AI 依赖不提前引入，后续作为独立模块接入.

未完成：

- AI 总结.
- 智能审稿建议.
- 知识库问答.
- AI 任务记录和人工确认流程.

交接说明：

- AI 模块不得影响上传、标注、确认主流程的可靠性.
- AI 生成内容必须保留人工确认入口.

完成判定：

- 至少一个 AI 辅助能力可独立使用.
- AI 输出不直接修改确认记录和版本规则.
