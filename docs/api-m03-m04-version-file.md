# M03 & M04 设计稿版本与文件上传接口文档

本文档记录 M03「设计稿版本」和 M04「文件上传、存储与预览」模块的 REST API。

## 基础约定

- 基础地址：`http://localhost:8080`
- 接口前缀：`/api`
- 后台认证方式：`Authorization: Bearer <accessToken>`

## 接口总览

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/api/admin/projects/{projectId}/versions` | 后台登录 | 获取项目的所有设计稿版本列表 |
| `POST` | `/api/admin/projects/{projectId}/versions` | 后台登录 | 上传新的设计稿版本（含文件） |

## GET `/api/admin/projects/{projectId}/versions`

用途：查询指定项目下的所有版本历史。

响应数据：`ProjectVersionResponse[]`。

`ProjectVersionResponse` 字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | number | 版本 ID |
| `storeId` | number | 门店 ID |
| `projectId` | number | 项目 ID |
| `versionNo` | number | 项目内递增版本号 (1, 2, ...) |
| `versionName` | string | 展示名称 (如 V1, V2) |
| `fileId` | number | 关联的文件对象 ID |
| `originalFilename` | string | 原始文件名 |
| `fileExt` | string | 文件扩展名 |
| `fileSize` | number | 文件大小 (字节) |
| `previewUrl` | string | 有效期内的预签名预览 URL |
| `description` | string | 版本说明 |
| `isCurrent` | boolean | 是否为当前展示版本 |
| `isConfirmed` | boolean | 是否已被确认定稿 |
| `confirmedAt` | string | 确认时间 |
| `createdAt` | string | 创建时间 |
| `uploadedBy` | number | 上传人 ID |
| `uploaderNickname` | string | 上传人昵称 |

## POST `/api/admin/projects/{projectId}/versions`

用途：上传新版本。该操作会：
1. 将文件存储至 MinIO。
2. 在 `file_object` 表记录文件元数据。
3. 在 `project_version` 表创建新版本记录。
4. 将该版本设为项目的 `current_version_id`。
5. 如果项目处于 `draft` 状态，自动变更为 `waiting_feedback`。

请求格式：`multipart/form-data`

请求参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `file` | file | 是 | 设计稿文件 (支持图片、PDF) |
| `description` | string | 否 | 版本说明 |

响应数据：`ProjectVersionResponse` (新创建的版本详情)。

可能错误：
- `400`: 项目已归档或已确认，不允许上传新版本。
- `404`: 项目不存在。
- `500`: MinIO 存储服务异常。
