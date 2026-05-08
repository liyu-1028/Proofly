# M01 账号、门店与权限接口文档

本文档记录 M01「账号、门店与权限」模块的 REST API。每次完成本模块的新接口、修改接口或废弃接口时，必须同步更新本文档。

文件命名规则：`docs/api-m01-account-store-permission.md`。

## 基础约定

- 基础地址：`http://localhost:8080`
- 接口前缀：`/api`
- 请求体默认：`application/json`
- 响应体默认：`application/json`
- 后台认证方式：`Authorization: Bearer <accessToken>`

统一响应结构：

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "timestamp": "2026-05-08T10:30:00+08:00"
}
```

常用错误码：

| HTTP 状态 | 业务码 | 说明 |
| --- | --- | --- |
| 400 | 400 | 请求参数错误 |
| 401 | 401 | 未登录、Token 缺失或 Token 无效 |
| 403 | 403 | 已登录但无权限访问 |
| 500 | 500 | 服务器内部错误 |

## 接口总览

| 方法 | 路径 | 状态 | 认证 | 说明 |
| --- | --- | --- | --- | --- |
| `POST` | `/api/auth/login` | 已实现 | 无需登录 | 后台用户登录 |
| `POST` | `/api/auth/refresh` | 已实现 | refresh token | 刷新访问令牌 |
| `POST` | `/api/auth/logout` | 已实现 | 后台登录 | 退出登录 |
| `GET` | `/api/auth/me` | 已实现 | 后台登录 | 当前用户信息 |
| `GET` | `/api/admin/stores/current` | 已实现 | 后台登录 | 当前门店信息 |
| `PUT` | `/api/admin/stores/current` | 已实现 | `owner` 或 `admin` | 更新当前门店 |
| `GET` | `/api/admin/users` | 已实现 | `owner` 或 `admin` | 员工列表 |
| `POST` | `/api/admin/users` | 已实现 | `owner` 或 `admin` | 创建员工 |
| `GET` | `/api/admin/users/{userId}` | 已实现 | `owner` 或 `admin` | 员工详情 |
| `PUT` | `/api/admin/users/{userId}` | 已实现 | `owner` 或 `admin` | 更新员工 |
| `PATCH` | `/api/admin/users/{userId}/status` | 已实现 | `owner` 或 `admin` | 启用、停用或锁定员工 |
| `POST` | `/api/admin/users/{userId}/reset-password` | 已实现 | `owner` 或 `admin` | 重置员工密码 |

## POST `/api/auth/login`

状态：已实现。

认证：无需登录。

用途：后台用户登录，返回 access token、refresh token 和当前用户摘要。

请求体：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `account` | string | 是 | 登录账号 |
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
      "status": "active",
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

前端调用位置：

- `frontend/src/api/auth.ts`
- `frontend/src/stores/session.ts`
- `frontend/src/views/auth/LoginView.vue`

## POST `/api/auth/refresh`

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

前端调用位置：

- `frontend/src/api/auth.ts`
- `frontend/src/stores/session.ts`
- `frontend/src/router/index.ts`

## POST `/api/auth/logout`

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

前端调用位置：

- `frontend/src/api/auth.ts`
- `frontend/src/stores/session.ts`
- `frontend/src/App.vue`

## GET `/api/auth/me`

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
    "status": "active",
    "roles": ["admin"]
  },
  "timestamp": "2026-05-08T10:30:00+08:00"
}
```

可能错误：

| HTTP 状态 | 业务码 | 场景 |
| --- | --- | --- |
| 401 | 401 | 缺少 access token、token 无效或已过期 |

前端调用位置：

- `frontend/src/api/auth.ts`
- `frontend/src/stores/session.ts`
- `frontend/src/router/index.ts`
- `frontend/src/views/admin/staff/StaffView.vue`
- `frontend/src/views/admin/settings/SettingsView.vue`

## GET `/api/admin/stores/current`

状态：已实现。

认证：需要后台登录。

用途：获取当前登录用户所属门店信息。

响应数据：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | number | 门店 ID |
| `name` | string | 门店名称 |
| `contactName` | string | 联系人 |
| `contactPhone` | string | 联系电话 |
| `status` | string | 门店状态：`active`、`disabled` |
| `deploymentMode` | string | 部署模式 |
| `createdAt` | string | 创建时间 |
| `updatedAt` | string | 更新时间 |

## PUT `/api/admin/stores/current`

状态：已实现。

认证：需要后台登录，且当前用户角色包含 `owner` 或 `admin`。

用途：更新当前门店基础资料。

请求体：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `name` | string | 是 | 门店名称，最长 100 字符 |
| `contactName` | string | 否 | 联系人，最长 50 字符 |
| `contactPhone` | string | 否 | 联系电话，最长 30 字符 |

响应数据：同 `GET /api/admin/stores/current`。

可能错误：

| HTTP 状态 | 业务码 | 场景 |
| --- | --- | --- |
| 400 | 400 | 门店名称为空或字段超长 |
| 403 | 403 | 当前账号无门店管理权限 |
| 404 | 404 | 当前门店不存在 |

## GET `/api/admin/users`

状态：已实现。

认证：需要后台登录，且当前用户角色包含 `owner` 或 `admin`。

用途：查询当前门店员工列表。

查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `keyword` | string | 否 | 按用户名、昵称、手机号模糊搜索 |
| `status` | string | 否 | 按员工状态筛选 |

响应数据：`UserResponse[]`。

`UserResponse` 字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `userId` | number | 用户 ID |
| `storeId` | number | 所属门店 ID |
| `username` | string | 用户名 |
| `nickname` | string | 昵称 |
| `phone` | string | 手机号 |
| `email` | string | 邮箱 |
| `status` | string | 用户状态：`active`、`disabled`、`locked` |
| `roles` | string[] | 角色编码列表 |
| `lastLoginAt` | string | 最近登录时间 |
| `createdAt` | string | 创建时间 |
| `updatedAt` | string | 更新时间 |

可能错误：

| HTTP 状态 | 业务码 | 场景 |
| --- | --- | --- |
| 403 | 403 | 当前账号无员工管理权限 |

## POST `/api/admin/users`

状态：已实现。

认证：需要后台登录，且当前用户角色包含 `owner` 或 `admin`。

用途：创建当前门店员工账号。

请求体：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `username` | string | 是 | 登录用户名，门店内唯一，最长 64 字符 |
| `nickname` | string | 是 | 昵称，最长 64 字符 |
| `phone` | string | 否 | 手机号，门店内唯一，最长 30 字符 |
| `email` | string | 否 | 邮箱，最长 128 字符 |
| `password` | string | 是 | 初始密码，6-72 字符 |
| `roleCodes` | string[] | 否 | 角色编码；为空时默认 `designer` |

响应数据：`UserResponse`。

可能错误：

| HTTP 状态 | 业务码 | 场景 |
| --- | --- | --- |
| 400 | 400 | 参数缺失、字段超长、角色不存在 |
| 403 | 403 | 当前账号无员工管理权限 |
| 409 | 409 | 用户名或手机号已存在 |

## GET `/api/admin/users/{userId}`

状态：已实现。

认证：需要后台登录，且当前用户角色包含 `owner` 或 `admin`。

用途：查看当前门店员工详情。

响应数据：`UserResponse`。

可能错误：

| HTTP 状态 | 业务码 | 场景 |
| --- | --- | --- |
| 403 | 403 | 当前账号无员工管理权限 |
| 404 | 404 | 员工不存在或不属于当前门店 |

## PUT `/api/admin/users/{userId}`

状态：已实现。

认证：需要后台登录，且当前用户角色包含 `owner` 或 `admin`。

用途：更新当前门店员工资料和角色。

请求体：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `nickname` | string | 是 | 昵称，最长 64 字符 |
| `phone` | string | 否 | 手机号，门店内唯一，最长 30 字符 |
| `email` | string | 否 | 邮箱，最长 128 字符 |
| `roleCodes` | string[] | 否 | 角色编码；传入时覆盖员工原有角色 |

响应数据：`UserResponse`。

可能错误：

| HTTP 状态 | 业务码 | 场景 |
| --- | --- | --- |
| 400 | 400 | 参数缺失、字段超长、角色不存在 |
| 403 | 403 | 当前账号无员工管理权限 |
| 404 | 404 | 员工不存在或不属于当前门店 |
| 409 | 409 | 手机号已存在 |

## PATCH `/api/admin/users/{userId}/status`

状态：已实现。

认证：需要后台登录，且当前用户角色包含 `owner` 或 `admin`。

用途：更新员工状态。

请求体：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `status` | string | 是 | `active`、`disabled` 或 `locked` |

响应数据：`UserResponse`。

业务规则：

- 不能修改当前登录账号自己的状态，避免把自己停用后无法继续管理。

可能错误：

| HTTP 状态 | 业务码 | 场景 |
| --- | --- | --- |
| 400 | 400 | 状态不合法，或尝试修改当前登录账号状态 |
| 403 | 403 | 当前账号无员工管理权限 |
| 404 | 404 | 员工不存在或不属于当前门店 |

## POST `/api/admin/users/{userId}/reset-password`

状态：已实现。

认证：需要后台登录，且当前用户角色包含 `owner` 或 `admin`。

用途：重置当前门店员工密码。

请求体：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `password` | string | 是 | 新密码，6-72 字符 |

响应数据：`null`。

可能错误：

| HTTP 状态 | 业务码 | 场景 |
| --- | --- | --- |
| 400 | 400 | 密码为空或长度不合法 |
| 403 | 403 | 当前账号无员工管理权限 |
| 404 | 404 | 员工不存在或不属于当前门店 |
