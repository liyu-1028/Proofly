# M02 审稿项目接口文档

本文档记录 M02「审稿项目」模块的 REST API。每次完成本模块的新接口、修改接口或废弃接口时，必须同步更新本文档。

文件命名规则：`docs/api-m02-project.md`。

## 基础约定

- 基础地址：`http://localhost:8080`
- 接口前缀：`/api`
- 后台认证方式：`Authorization: Bearer <accessToken>`

## 接口总览

| 方法 | 路径 | 状态 | 认证 | 说明 |
| --- | --- | --- | --- | --- |
| `GET` | `/api/admin/projects` | 已实现 | 后台登录 | 审稿项目列表 |
| `POST` | `/api/admin/projects` | 已实现 | 后台登录 | 创建审稿项目 |
| `GET` | `/api/admin/projects/{projectId}` | 已实现 | 后台登录 | 审稿项目详情 |
| `PUT` | `/api/admin/projects/{projectId}` | 已实现 | 后台登录 | 更新审稿项目 |
| `PATCH` | `/api/admin/projects/{projectId}/archive` | 已实现 | 后台登录 | 归档项目 |
| `PATCH` | `/api/admin/projects/{projectId}/restore` | 已实现 | 后台登录 | 恢复项目 |

## GET `/api/admin/projects`

用途：查询当前门店审稿项目列表。

查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `keyword` | string | 否 | 按项目名称、客户名称模糊搜索 |
| `status` | string | 否 | 按项目状态筛选 |
| `ownerUserId` | number | 否 | 按项目负责设计师 ID 筛选 |

响应数据：`ProjectResponse[]`。

`ProjectResponse` 字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | number | 项目 ID |
| `storeId` | number | 门店 ID |
| `name` | string | 项目名称 |
| `customerName` | string | 客户名称 |
| `customerContact` | string | 客户联系方式 |
| `ownerUserId` | number | 项目负责设计师 ID |
| `ownerNickname` | string | 项目负责人昵称 |
| `status` | string | 项目状态：`draft`, `waiting_feedback`, `change_requested`, `waiting_confirm`, `confirmed`, `archived` |
| `currentVersionId` | number | 当前展示版本 ID |
| `confirmedVersionId` | number | 最终确认版本 ID |
| `remark` | string | 项目备注 |
| `archivedAt` | string | 归档时间 |
| `createdAt` | string | 创建时间 |
| `createdByNickname` | string | 创建人昵称 |
| `updatedAt` | string | 更新时间 |

## POST `/api/admin/projects`

用途：创建新审稿项目。

请求体：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `name` | string | 是 | 项目名称 |
| `customerName` | string | 否 | 客户名称 |
| `customerContact` | string | 否 | 客户联系方式 |
| `ownerUserId` | number | 是 | 项目负责设计师 ID，必须是当前门店下状态为 `active` 且拥有 `designer` 角色的用户 |
| `remark` | string | 否 | 备注 |

响应数据：`ProjectResponse`。

## GET `/api/admin/projects/{projectId}`

用途：查看审稿项目详情。

响应数据：`ProjectResponse`。

## PUT `/api/admin/projects/{projectId}`

用途：更新项目基础资料。

请求体：同 `POST /api/admin/projects`。

响应数据：`ProjectResponse`。

## PATCH `/api/admin/projects/{projectId}/archive`

用途：将项目状态变更为 `archived` 并记录归档时间。

响应数据：`ProjectResponse`。

## PATCH `/api/admin/projects/{projectId}/restore`

用途：恢复已归档项目，状态将根据是否已确认版本回退。

响应数据：`ProjectResponse`。

## 前端调用位置

M02 前端已接入本模块接口：

| 前端文件 | 说明 |
| --- | --- |
| `frontend/src/api/projects.ts` | M02 项目接口封装 |
| `frontend/src/api/users.ts` | 获取负责人下拉选项 |
| `frontend/src/types/project.ts` | 项目状态、响应和请求类型 |
| `frontend/src/views/admin/projects/ProjectsView.vue` | 项目列表、筛选、创建、编辑、归档和恢复 |
| `frontend/src/views/admin/projects/ProjectDetailView.vue` | 项目详情、编辑、归档和恢复 |
| `frontend/src/router/index.ts` | `/admin/projects` 和 `/admin/projects/:projectId` 路由 |

前端实现说明：

- 项目列表支持关键字、状态、负责人筛选。
- 创建和编辑项目时，`ownerUserId` 表示项目负责设计师的 `userId`，不是门店老板 ID。
- 归档项目不能在列表和详情页继续编辑，只能恢复后编辑。
- 当前版本、确认版本、标注数量等能力将在后续 M03-M08 模块接入。
