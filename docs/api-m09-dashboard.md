# M09 后台工作台与状态看板接口文档

本文档记录 M09「后台工作台与状态看板」模块的 REST API。

## 基础约定

- 基础地址：`http://localhost:8080`
- 接口前缀：`/api/admin/dashboard`
- 后台认证方式：`Authorization: Bearer <accessToken>`

## 接口总览

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/stats` | 后台登录 | 获取工作台统计数据、最近项目及近期动态 |

## GET `/stats`

用途：获取当前门店的工作台概览数据，包括各状态项目计数、最近创建的项目以及最近的操作日志。

响应数据：`DashboardStatsResponse`。

`DashboardStatsResponse` 字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `totalProjects` | number | 项目总数 (非归档/删除) |
| `statusCounts` | object | 各状态项目的计数，Key 为状态编码 (如 `confirmed`), Value 为数量 |
| `recentProjects` | array | 最近更新的 5 个项目列表，结构同 `ProjectResponse` |
| `recentActivities` | array | 最近的 10 条操作日志列表 |

`AuditLogResponse` 字段 (用于 `recentActivities`)：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | number | 日志 ID |
| `action` | string | 动作编码 |
| `targetType` | string | 目标类型 (如 `project`) |
| `targetId` | number | 目标 ID |
| `operatorType` | string | 操作人类型 (`user` 或 `customer`) |
| `operatorName` | string | 操作人名称 |
| `summary` | string | 动作摘要描述 |
| `createdAt` | string | 发生时间 |
