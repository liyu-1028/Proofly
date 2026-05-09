# M07 客户确认定稿接口文档

本文档记录 M07「客户确认定稿」模块的 REST API。

## 基础约定

- 公开基础地址：`http://localhost:8080/api/public/reviews/{token}`
- 后台基础地址：`http://localhost:8080/api/admin`

## 接口总览

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| `POST` | `/api/public/reviews/{token}/confirmations` | 公开 Token | 客户确认当前版本为最终定稿 |
| `GET` | `/api/admin/projects/{projectId}/confirmation` | 后台登录 | 获取项目的最终确认记录 |

## POST `/api/public/reviews/{token}/confirmations`

用途：客户点击“确认定稿”。该操作会：
1. 更新项目状态为 `confirmed`。
2. 记录确认人的 IP、User-Agent。
3. 产生不可篡改的 `confirmation_record`。

请求体：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `customerName` | string | 是 | 确认人姓名 |
| `customerContact` | string | 否 | 联系方式 |

响应数据：`ConfirmationRecordResponse`。

## GET `/api/admin/projects/{projectId}/confirmation`

用途：查看项目是否已确认，以及确认详情。

响应数据：`ConfirmationRecordResponse` (如果未确认则返回 `data: null`)。

`ConfirmationRecordResponse` 字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | string | 记录 ID |
| `versionId` | string | 被确认的版本 ID |
| `customerName` | string | 确认人 |
| `confirmedAt` | string | 确认时间 |

## 前端接入

- API 封装：`frontend/src/api/confirmations.ts`。
- 客户端入口：`frontend/src/views/public/review/ReviewView.vue`，客户填写确认人和联系方式后调用 `POST /api/public/reviews/{token}/confirmations`。
- 管理端入口：`frontend/src/views/admin/projects/ProjectDetailView.vue`，调用 `GET /api/admin/projects/{projectId}/confirmation` 展示确认人、确认时间和确认版本。
