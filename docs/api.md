# 审稿宝 API 接口文档

本文档记录审稿宝后端 REST API 约定。每次新增、修改或删除接口时，必须同步更新本文档。

维护要求：

- 新增接口时，补充接口分组、路径、认证方式、请求参数、响应数据、错误码和实现状态。
- 修改接口入参、出参、业务规则或权限边界时，同步修改对应章节。
- 删除或废弃接口时，不要直接抹掉历史说明，应先标记为“已废弃”，并说明替代接口。
- 接口实现完成后，将状态从“计划中”或“进行中”改为“已实现”。
- 如接口完成会影响模块状态，还需要同步更新 `docs/system-module-list.md` 和 `docs/module-completion.md`。

## 基础约定

### 基础地址

本地开发默认地址：

```text
http://localhost:8080
```

接口统一以 `/api` 开头。

### 数据格式

- 请求体默认使用 `application/json`。
- 响应体默认使用 `application/json`。
- 文件上传接口后续使用 `multipart/form-data`，具体格式在文件模块实现时补充。
- 时间字段使用 ISO-8601 字符串，例如 `2026-05-08T10:30:00+08:00`。
- ID 字段后端使用 `Long`，前端 TypeScript 建议按 `number` 处理；如后续出现大整数精度问题，再统一改为字符串。

### 统一响应结构

所有业务接口默认返回统一结构：

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "timestamp": "2026-05-08T10:30:00+08:00"
}
```

字段说明：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | number | 业务码，`0` 表示成功 |
| `message` | string | 响应消息 |
| `data` | object / array / null | 业务数据 |
| `timestamp` | string | 服务端响应时间 |

错误响应示例：

```json
{
  "code": 401,
  "message": "缺少访问令牌",
  "data": null,
  "timestamp": "2026-05-08T10:30:00+08:00"
}
```

### 常用错误码

| HTTP 状态 | 业务码 | 说明 |
| --- | --- | --- |
| 400 | 400 | 请求参数错误 |
| 401 | 401 | 未登录、Token 缺失或 Token 无效 |
| 403 | 403 | 已登录但无权限访问 |
| 404 | 404 | 资源不存在，后续模块实现时补充 |
| 409 | 409 | 业务状态冲突，后续模块实现时补充 |
| 500 | 500 | 服务器内部错误 |

### 认证方式

后台接口使用 Bearer Token：

```http
Authorization: Bearer <accessToken>
```

客户公开审稿接口使用审稿链接 token，路径形态暂定为：

```text
/api/public/reviews/{token}
```

后台登录态和客户审稿 token 是两套权限边界，不能混用。

### 接口状态

| 状态 | 含义 |
| --- | --- |
| 已实现 | 后端已有可运行 Controller 和业务逻辑 |
| 进行中 | 已有部分代码或约定，但能力不完整 |
| 计划中 | 仅规划接口，尚无业务实现 |
| 已废弃 | 不建议继续使用，保留历史说明 |

## 接口总览

| 模块 | 接口分组 | 状态 | 说明 |
| --- | --- | --- | --- |
| M12 部署、运维与开放接口 | `/api/health` | 已实现 | 健康检查 |
| M01 账号、门店与权限 | `/api/auth/**` | 已实现 | 登录、刷新 Token、登出、当前用户 |
| M01 账号、门店与权限 | `/api/admin/stores/**` | 计划中 | 门店管理 |
| M01 账号、门店与权限 | `/api/admin/users/**` | 计划中 | 员工管理 |
| M02 审稿项目 | `/api/admin/projects/**` | 计划中 | 后台项目管理 |
| M03 设计稿版本 | `/api/admin/projects/{projectId}/versions/**` | 计划中 | 版本管理 |
| M04 文件上传、存储与预览 | `/api/admin/files/**` | 计划中 | 后台文件上传和管理 |
| M05 客户审稿链接 | `/api/admin/review-links/**` | 计划中 | 审稿链接管理 |
| M05 客户审稿链接 | `/api/public/reviews/**` | 计划中 | 客户公开审稿访问 |
| M06 在线标注评论 | `/api/admin/annotations/**` | 计划中 | 后台标注处理 |
| M06 在线标注评论 | `/api/public/reviews/{token}/annotations/**` | 计划中 | 客户提交标注 |
| M07 客户确认定稿 | `/api/public/reviews/{token}/confirmations/**` | 计划中 | 客户确认定稿 |
| M08 审稿行为与确认记录 | `/api/admin/confirmations/**` | 计划中 | 确认记录查询 |
| M09 后台工作台与状态看板 | `/api/admin/dashboard/**` | 计划中 | 工作台统计 |
| M10 通知与提醒 | `/api/admin/notifications/**` | 计划中 | 通知提醒 |
| M11 系统配置与基础数据 | `/api/admin/settings/**` | 计划中 | 系统配置 |

## M12 健康检查

### GET `/api/health`

状态：已实现。

认证：无需登录。

用途：检查后端服务是否启动。

请求参数：无。

响应数据：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `application` | string | 应用名称 |
| `status` | string | 服务状态，当前为 `UP` |
| `profiles` | string[] | 当前启用的 Spring profile |

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "application": "proofly-backend",
    "status": "UP",
    "profiles": ["dev"]
  },
  "timestamp": "2026-05-08T10:30:00+08:00"
}
```

## M01 认证接口

### POST `/api/auth/login`

状态：已实现。

认证：无需登录。

用途：后台用户登录，返回 access token、refresh token 和当前用户摘要。

请求体：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `account` | string | 是 | 登录账号，当前实现对应后台用户名或账号字段 |
| `password` | string | 是 | 登录密码 |

请求示例：

```json
{
  "account": "admin",
  "password": "admin123"
}
```

响应数据：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `accessToken` | string | 后台访问令牌 |
| `refreshToken` | string | 刷新令牌 |
| `accessTokenExpiresAt` | string | access token 过期时间 |
| `refreshTokenExpiresAt` | string | refresh token 过期时间 |
| `user` | object | 当前用户摘要 |

`user` 字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `userId` | number | 用户 ID |
| `storeId` | number | 所属门店 ID |
| `username` | string | 用户名 |
| `nickname` | string | 昵称 |
| `phone` | string | 手机号 |
| `status` | string | 用户状态 |
| `roles` | string[] | 角色编码列表 |

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOi...",
    "refreshToken": "eyJhbGciOi...",
    "accessTokenExpiresAt": "2026-05-08T12:30:00+08:00",
    "refreshTokenExpiresAt": "2026-05-15T10:30:00+08:00",
    "user": {
      "userId": 1,
      "storeId": 1,
      "username": "admin",
      "nickname": "管理员",
      "phone": "13800000000",
      "status": "enabled",
      "roles": ["admin"]
    }
  },
  "timestamp": "2026-05-08T10:30:00+08:00"
}
```

可能错误：

| HTTP 状态 | 业务码 | 场景 |
| --- | --- | --- |
| 400 | 400 | `account` 或 `password` 为空 |
| 401 | 401 | 账号或密码错误、用户停用、门店停用 |

### POST `/api/auth/refresh`

状态：已实现。

认证：无需 access token；需要有效 refresh token。

用途：使用 refresh token 换取新的 access token 和 refresh token。

请求体：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `refreshToken` | string | 是 | 登录或上次刷新得到的 refresh token |

请求示例：

```json
{
  "refreshToken": "eyJhbGciOi..."
}
```

响应数据：同 `POST /api/auth/login`。

可能错误：

| HTTP 状态 | 业务码 | 场景 |
| --- | --- | --- |
| 400 | 400 | `refreshToken` 为空 |
| 401 | 401 | refresh token 无效、过期或会话失效 |

### POST `/api/auth/logout`

状态：已实现。

认证：需要后台登录。

用途：退出当前登录态，使当前 access token 失效。

请求头：

```http
Authorization: Bearer <accessToken>
```

请求体：无。

响应数据：`null`。

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": null,
  "timestamp": "2026-05-08T10:30:00+08:00"
}
```

可能错误：

| HTTP 状态 | 业务码 | 场景 |
| --- | --- | --- |
| 401 | 401 | 缺少 access token、token 无效或已过期 |

### GET `/api/auth/me`

状态：已实现。

认证：需要后台登录。

用途：获取当前后台登录用户信息。

请求头：

```http
Authorization: Bearer <accessToken>
```

请求参数：无。

响应数据：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `userId` | number | 用户 ID |
| `storeId` | number | 所属门店 ID |
| `username` | string | 用户名 |
| `nickname` | string | 昵称 |
| `phone` | string | 手机号 |
| `status` | string | 用户状态 |
| `roles` | string[] | 角色编码列表 |

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "userId": 1,
    "storeId": 1,
    "username": "admin",
    "nickname": "管理员",
    "phone": "13800000000",
    "status": "enabled",
    "roles": ["admin"]
  },
  "timestamp": "2026-05-08T10:30:00+08:00"
}
```

可能错误：

| HTTP 状态 | 业务码 | 场景 |
| --- | --- | --- |
| 401 | 401 | 缺少 access token、token 无效或已过期 |

## M01 门店管理接口

状态：计划中。

建议接口：

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/api/admin/stores/current` | 后台登录 | 获取当前门店信息 |
| `PUT` | `/api/admin/stores/current` | 后台登录 | 更新当前门店信息 |

实现门店管理接口时，在本章节补充请求体、响应体、权限规则和错误码。

## M01 员工管理接口

状态：计划中。

建议接口：

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/api/admin/users` | 后台登录 | 员工列表 |
| `POST` | `/api/admin/users` | 后台登录 | 创建员工 |
| `GET` | `/api/admin/users/{userId}` | 后台登录 | 员工详情 |
| `PUT` | `/api/admin/users/{userId}` | 后台登录 | 更新员工 |
| `PATCH` | `/api/admin/users/{userId}/status` | 后台登录 | 启用或停用员工 |
| `POST` | `/api/admin/users/{userId}/reset-password` | 后台登录 | 重置员工密码 |

实现员工管理接口时，在本章节补充请求体、响应体、权限规则和错误码。

## M02 审稿项目接口

状态：计划中。

建议接口：

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/api/admin/projects` | 后台登录 | 项目列表 |
| `POST` | `/api/admin/projects` | 后台登录 | 创建项目 |
| `GET` | `/api/admin/projects/{projectId}` | 后台登录 | 项目详情 |
| `PUT` | `/api/admin/projects/{projectId}` | 后台登录 | 更新项目 |
| `POST` | `/api/admin/projects/{projectId}/archive` | 后台登录 | 归档项目 |
| `POST` | `/api/admin/projects/{projectId}/restore` | 后台登录 | 恢复项目 |
| `GET` | `/api/admin/projects/{projectId}/timeline` | 后台登录 | 项目时间线 |

实现项目接口时，需要明确项目状态流转规则和门店隔离校验。

## M03 设计稿版本接口

状态：计划中。

建议接口：

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/api/admin/projects/{projectId}/versions` | 后台登录 | 版本列表 |
| `POST` | `/api/admin/projects/{projectId}/versions` | 后台登录 | 上传新版本 |
| `GET` | `/api/admin/projects/{projectId}/versions/{versionId}` | 后台登录 | 版本详情 |
| `POST` | `/api/admin/projects/{projectId}/versions/{versionId}/current` | 后台登录 | 设置当前版本 |

实现版本接口时，需要说明上传参数、版本号规则、当前版本规则和已确认版本保护规则。

## M04 文件上传、存储与预览接口

状态：计划中。

建议接口：

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| `POST` | `/api/admin/files` | 后台登录 | 上传文件 |
| `GET` | `/api/admin/files/{fileId}` | 后台登录 | 文件元数据 |
| `GET` | `/api/admin/files/{fileId}/preview` | 后台登录 | 后台预览文件 |
| `GET` | `/api/public/reviews/{token}/files/{fileId}/preview` | 审稿 token | 客户预览文件 |

实现文件接口时，需要明确 `multipart/form-data` 字段、文件大小限制、文件类型限制、MinIO object key 和访问控制。

## M05 客户审稿链接接口

状态：计划中。

建议接口：

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| `POST` | `/api/admin/projects/{projectId}/review-link` | 后台登录 | 生成或获取审稿链接 |
| `POST` | `/api/admin/projects/{projectId}/review-link/regenerate` | 后台登录 | 重新生成审稿链接 |
| `POST` | `/api/admin/projects/{projectId}/review-link/disable` | 后台登录 | 停用审稿链接 |
| `GET` | `/api/public/reviews/{token}` | 审稿 token | 获取客户审稿页数据 |

实现审稿链接接口时，需要明确 token 明文只返回一次还是可重复获取，以及 MySQL 中只保存 `token_hash` 的规则。

## M06 在线标注评论接口

状态：计划中。

建议接口：

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/api/admin/projects/{projectId}/versions/{versionId}/annotations` | 后台登录 | 后台查看版本标注 |
| `PATCH` | `/api/admin/annotations/{annotationId}/status` | 后台登录 | 更新标注处理状态 |
| `POST` | `/api/admin/annotations/{annotationId}/comments` | 后台登录 | 设计师回复标注 |
| `POST` | `/api/public/reviews/{token}/annotations` | 审稿 token | 客户提交标注 |

实现标注接口时，需要明确坐标比例字段、标注类型、标注状态和版本绑定关系。

## M07 客户确认定稿接口

状态：计划中。

建议接口：

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| `POST` | `/api/public/reviews/{token}/confirmations` | 审稿 token | 客户确认当前版本 |
| `GET` | `/api/admin/projects/{projectId}/confirmation` | 后台登录 | 查看项目确认记录 |
| `GET` | `/api/admin/confirmations/{confirmationId}` | 后台登录 | 确认记录详情 |

实现确认接口时，需要明确幂等规则、确认人信息、确认版本、IP、User-Agent 和确认后项目状态变化。

## M08 审稿行为与确认记录接口

状态：计划中。

建议接口：

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/api/admin/projects/{projectId}/timeline` | 后台登录 | 项目审稿时间线 |
| `GET` | `/api/admin/confirmations` | 后台登录 | 确认记录列表 |
| `GET` | `/api/admin/confirmations/{confirmationId}/export` | 后台登录 | 导出确认单，后续实现 |

实现审计和确认记录接口时，需要明确哪些记录不可物理删除。

## M09 后台工作台接口

状态：计划中。

建议接口：

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/api/admin/dashboard/summary` | 后台登录 | 工作台汇总 |
| `GET` | `/api/admin/dashboard/todos` | 后台登录 | 我的待办 |
| `GET` | `/api/admin/dashboard/recent-activities` | 后台登录 | 近期动态 |

实现工作台接口时，需要明确统计口径和门店隔离规则。

## M10 通知与提醒接口

状态：计划中。

建议接口：

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/api/admin/notifications` | 后台登录 | 通知列表 |
| `POST` | `/api/admin/notifications/{notificationId}/read` | 后台登录 | 标记已读 |
| `POST` | `/api/admin/projects/{projectId}/reminder-text` | 后台登录 | 生成客户提醒文案 |

实现通知接口时，需要明确站内通知和第三方通知渠道的边界。

## M11 系统配置接口

状态：计划中。

建议接口：

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/api/admin/settings` | 后台登录 | 获取系统设置 |
| `PUT` | `/api/admin/settings` | 后台登录 | 更新系统设置 |
| `GET` | `/api/admin/settings/upload-policy` | 后台登录 | 获取上传策略 |
| `PUT` | `/api/admin/settings/upload-policy` | 后台登录 | 更新上传策略 |

实现配置接口时，需要区分环境变量配置和业务配置，敏感配置不能返回给前端。

## 文档更新检查清单

每次写完接口后，按下面清单检查：

1. `docs/api.md` 是否补充或更新了接口说明。
2. 请求体、响应体、认证方式、错误码是否与代码一致。
3. Controller 路径和 HTTP 方法是否与文档一致。
4. 前端 `src/api/` 调用是否与文档一致。
5. 涉及新模块能力时，是否更新 `docs/system-module-list.md` 和 `docs/module-completion.md`。
6. 涉及数据结构变更时，是否更新 `docs/database.md` 和 `docs/mysql-schema.sql`。
7. 涉及环境变量时，是否更新 `.env.example`、README 或部署文档。
