# M06 在线标注评论接口文档

本文档记录 M06「在线标注评论」模块的 REST API。包含面向客户的公开接口和面向设计师的后台接口。

## 基础约定

- 后台基础地址：`http://localhost:8080/api/admin`
- 公开基础地址：`http://localhost:8080/api/public/reviews/{token}`
- 认证方式：
  - 后台接口：`Authorization: Bearer <accessToken>`
  - 公开接口：通过 URL 中的 `{token}` 进行校验。

## 接口总览

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| `POST` | `/api/public/reviews/{token}/annotations` | 公开 Token | 客户提交新的标注意见 |
| `GET` | `/api/admin/projects/{projectId}/versions/{versionId}/annotations` | 后台登录 | 获取指定版本的标注列表 |
| `PATCH` | `/api/admin/projects/{projectId}/versions/{versionId}/annotations/{annotationId}/status` | 后台登录 | 更新标注状态 (resolve/ignore) |

## POST `/api/public/reviews/{token}/annotations`

用途：客户在设计稿上提交位置化的修改意见。提交后项目状态自动变更为 `change_requested`。

请求体：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `type` | string | 是 | 标注类型：`point`, `rect` |
| `xRatio` | number | 是 | X 轴相对比例 (0-1) |
| `yRatio` | number | 是 | Y 轴相对比例 (0-1) |
| `widthRatio` | number | 否 | 矩形宽度比例 |
| `heightRatio` | number | 否 | 矩形高度比例 |
| `content` | string | 是 | 修改意见内容 |
| `customerName` | string | 否 | 客户署名 |

响应数据：`AnnotationResponse`。

## GET `/api/admin/projects/{projectId}/versions/{versionId}/annotations`

用途：设计师在后台按版本查看所有标注。

响应数据：`AnnotationResponse[]`。

`AnnotationResponse` 字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | string | 标注 ID |
| `type` | string | 标注类型 |
| `xRatio` | number | X 相对比例 |
| `yRatio` | number | Y 相对比例 |
| `widthRatio` | number | 宽度比例 |
| `heightRatio` | number | 高度比例 |
| `content` | string | 意见内容 |
| `customerName` | string | 客户名称 |
| `status` | string | 状态：`open`, `resolved`, `ignored` |
| `createdAt` | string | 创建时间 |
| `resolvedByNickname` | string | 处理人昵称 |
| `resolvedAt` | string | 处理时间 |

## PATCH `/api/admin/projects/{projectId}/versions/{versionId}/annotations/{annotationId}/status`

用途：设计师处理完意见后，将标注标记为已解决或忽略。

查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `status` | string | 是 | 目标状态：`resolved` 或 `ignored` |

响应数据：`null`。

## 前端接入

- API 封装：`frontend/src/api/annotations.ts`。
- 客户端入口：`frontend/src/views/public/review/ReviewView.vue`，使用 `POST /api/public/reviews/{token}/annotations` 提交点位标注。
- 管理端入口：`frontend/src/views/admin/projects/ProjectDetailView.vue`，使用 `GET /api/admin/projects/{projectId}/versions/{versionId}/annotations` 按当前版本加载标注，并使用 `PATCH .../status` 处理标注状态。
