# M12 部署、运维与开放接口文档

本文档记录 M12「部署、运维与开放接口」模块的 REST API。每次完成本模块的新接口、修改接口或废弃接口时，必须同步更新本文档。

文件命名规则：`docs/api-m12-deployment-ops-openapi.md`。

## 基础约定

- 基础地址：`http://localhost:8080`
- 接口前缀：`/api`
- 请求体默认：`application/json`
- 响应体默认：`application/json`

统一响应结构：

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "timestamp": "2026-05-08T10:30:00+08:00"
}
```

## 接口总览

| 方法 | 路径 | 状态 | 认证 | 说明 |
| --- | --- | --- | --- | --- |
| `GET` | `/api/health` | 已实现 | 无需登录 | 后端健康检查 |

## GET `/api/health`

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

## 后续补充方向

后续如果新增运维、版本信息、构建信息、OpenAPI 聚合、存储连通性检查等接口，继续在本文档中补充。

新增接口时至少记录：

- 接口路径和 HTTP 方法。
- 是否需要登录。
- 请求参数和响应数据。
- 健康检查口径。
- 是否会暴露敏感配置。
