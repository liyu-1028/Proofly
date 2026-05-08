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
| `GET` | `/api/admin/stores/current` | 计划中 | 后台登录 | 当前门店信息 |
| `PUT` | `/api/admin/stores/current` | 计划中 | 后台登录 | 更新当前门店 |
| `GET` | `/api/admin/users` | 计划中 | 后台登录 | 员工列表 |
| `POST` | `/api/admin/users` | 计划中 | 后台登录 | 创建员工 |
| `GET` | `/api/admin/users/{userId}` | 计划中 | 后台登录 | 员工详情 |
| `PUT` | `/api/admin/users/{userId}` | 计划中 | 后台登录 | 更新员工 |
| `PATCH` | `/api/admin/users/{userId}/status` | 计划中 | 后台登录 | 启用或停用员工 |
| `POST` | `/api/admin/users/{userId}/reset-password` | 计划中 | 后台登录 | 重置员工密码 |

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

## 计划接口补充规则

后续完成门店管理、员工管理、角色权限接口时，在本文档中新增对应接口详情，至少包含：

- 接口路径和 HTTP 方法。
- 认证方式和角色权限。
- 请求参数和响应数据。
- 门店隔离规则。
- 常见错误码。
- 前端调用位置。
