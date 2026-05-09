# M08 审稿行为与确认记录接口文档

本文档记录 M08「审稿行为与确认记录」模块的 REST API。该模块主要用于展示项目的时间线。

## 基础约定

- 基础地址：`http://localhost:8080/api/admin`
- 认证方式：`Authorization: Bearer <accessToken>`

## 接口总览

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/admin/projects/{projectId}/timeline` | 获取项目的全生命周期操作日志 |

## GET `/api/admin/projects/{projectId}/timeline`

用途：获取项目从创建、上传版本、客户访问到最终确认的所有关键节点。

响应数据：`AuditLog[]`。

`AuditLog` 字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | number | 日志 ID |
| `action` | string | 动作类型 (如 CREATE, SUBMIT_ANNOTATION, CONFIRM_PROJECT) |
| `operatorType` | string | 操作人类型：`user`, `customer`, `system` |
| `operatorName` | string | 操作人名称 |
| `summary` | string | 日志描述文本 |
| `createdAt` | string | 发生时间 |
