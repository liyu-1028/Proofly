# M05 客户审稿链接接口文档

本文档记录 M05「客户审稿链接」模块的 REST API。

## 基础约定

- 基础地址：`http://localhost:8080`
- 接口前缀：`/api`
- 后台认证方式：`Authorization: Bearer <accessToken>`

## 接口总览

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/api/admin/projects/{projectId}/review-links` | 后台登录 | 获取项目的所有审稿链接列表 |
| `POST` | `/api/admin/projects/{projectId}/review-links` | 后台登录 | 为项目生成新的审稿链接 |
| `PATCH` | `/api/admin/review-links/{linkId}/disable` | 后台登录 | 禁用审稿链接 |
| `PATCH` | `/api/admin/review-links/{linkId}/enable` | 后台登录 | 启用审稿链接 |
| `DELETE` | `/api/admin/review-links/{linkId}` | 后台登录 | 删除审稿链接 |

## GET `/api/admin/projects/{projectId}/review-links`

用途：查询指定项目下的所有审稿链接历史。

响应数据：`ReviewLinkResponse[]`。

`ReviewLinkResponse` 字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | number | 链接 ID |
| `projectId` | number | 项目 ID |
| `currentVersionId` | number | 链接生成时指向的版本 ID |
| `status` | string | 链接状态：`active`, `disabled`, `expired` |
| `expiresAt` | string | 过期时间 (ISO 8601) |
| `maxAccessCount` | number | 最大访问次数 |
| `accessCount` | number | 已访问次数 |
| `lastAccessAt` | string | 最近访问时间 |
| `url` | string | 完整的审稿链接 (仅在创建时返回，后续不返回完整 token) |
| `createdAt` | string | 创建时间 |

## POST `/api/admin/projects/{projectId}/review-links`

用途：为项目生成一个新的、不可伪造的审稿链接。生成的 token 会经过 SHA-256 哈希后存储。

请求体：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `expiresAt` | string | 否 | 过期时间 |
| `maxAccessCount` | number | 否 | 最大访问次数限制 |

响应数据：`ReviewLinkResponse` (含 `token` 和 `url` 字段)。

**注意**：`token` 和 `url` 仅在此创建接口的响应中返回一次，后台不存储明文 token。

## PATCH `/api/admin/review-links/{linkId}/disable`

用途：禁用指定的审稿链接，使其暂时不可访问。

## PATCH `/api/admin/review-links/{linkId}/enable`

用途：重新启用被禁用的审稿链接。

## DELETE `/api/admin/review-links/{linkId}`

用途：逻辑删除指定的审稿链接。

## 前端调用位置

M05 前端已接入本模块接口：

| 前端文件 | 说明 |
| --- | --- |
| `frontend/src/api/review-links.ts` | 审稿链接接口封装 |
| `frontend/src/views/admin/projects/ProjectDetailView.vue` | 项目详情页中的审稿链接面板 |

前端实现说明：

- 项目详情页支持查看历史审稿链接。
- 支持生成新链接，并在创建成功后立即展示和复制完整 `url`。
- 支持禁用、启用和删除审稿链接。
- 历史链接不会展示完整 URL；若用户丢失链接，应重新生成。
