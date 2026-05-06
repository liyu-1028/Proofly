# 审稿宝数据存储设计

本文档设计审稿宝 MVP 阶段需要的 MySQL 数据库结构、Redis 必要缓存键，以及 MinIO 对象文件路径命名。

设计目标：

- 支撑“创建项目 → 上传版本 → 客户审稿 → 在线标注 → 上传新版 → 客户确认 → 留痕”的主流程。
- 从第一版开始保留 `store_id`，兼容单店私有部署和后续多门店平台化部署。
- 设计稿版本不可覆盖，确认记录、访问日志、审计日志不可随意物理删除。
- Redis 只保存会话、热点摘要、短期状态和轻量控制数据，不保存最终业务事实。
- MinIO 对象 key 从第一版开始包含门店、项目、版本维度。

## 通用约定

### MySQL 基线

- 数据库版本：MySQL 8.0。
- 默认字符集：`utf8mb4`。
- 默认排序规则：`utf8mb4_0900_ai_ci`。
- 主键类型：`BIGINT`，由雪花 ID 或等价全局 ID 生成。
- 时间字段：`DATETIME(3)`。
- 状态字段：`VARCHAR(32)`。
- 金额、用量等后续字段不在 MVP 表中提前设计。
- 公开访问 token 不使用连续 ID，不直接暴露数据库主键。

### 通用字段

核心业务表默认包含：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | 是 | 主键，全局唯一 ID |
| `store_id` | `BIGINT` | 是 | 门店 ID |
| `created_at` | `DATETIME(3)` | 是 | 创建时间 |
| `created_by` | `BIGINT` | 否 | 创建人，客户公开操作可为空 |
| `updated_at` | `DATETIME(3)` | 是 | 更新时间 |
| `updated_by` | `BIGINT` | 否 | 更新人，客户公开操作可为空 |
| `deleted` | `TINYINT(1)` | 是 | 逻辑删除标记，默认 `0` |

日志和留痕表可以不包含 `updated_at`、`updated_by`、`deleted`，因为它们默认追加写入，不做常规更新和删除。

### 命名约定

- 表名使用单数名词：`project`、`project_version`、`file_object`。
- 外键字段使用 `{table}_id`：`project_id`、`version_id`、`file_id`。
- 客户公开访问相关接口使用 `review` 命名，后台用户使用 `user` 命名。
- 枚举值使用小写英文和下划线。

### 删除约定

- 项目、用户、配置等管理类数据使用逻辑删除。
- 设计稿版本、文件元数据、确认记录、访问日志、审计日志默认不物理删除。
- 已确认版本关联的 `project_version`、`file_object`、`confirmation_record` 不允许物理删除。
- 如后续需要撤销确认，新增作废状态或作废记录，不删除原确认记录。

## MySQL 表结构

### `store`

门店信息表。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | 是 | 门店 ID |
| `name` | `VARCHAR(100)` | 是 | 门店名称 |
| `contact_name` | `VARCHAR(50)` | 否 | 联系人 |
| `contact_phone` | `VARCHAR(30)` | 否 | 联系电话 |
| `status` | `VARCHAR(32)` | 是 | 门店状态 |
| `deployment_mode` | `VARCHAR(32)` | 是 | 部署模式 |
| `created_at` | `DATETIME(3)` | 是 | 创建时间 |
| `created_by` | `BIGINT` | 否 | 创建人 |
| `updated_at` | `DATETIME(3)` | 是 | 更新时间 |
| `updated_by` | `BIGINT` | 否 | 更新人 |
| `deleted` | `TINYINT(1)` | 是 | 逻辑删除 |

状态：

| 值 | 说明 |
| --- | --- |
| `active` | 正常 |
| `disabled` | 停用 |

部署模式：

| 值 | 说明 |
| --- | --- |
| `single-store` | 单店部署 |
| `multi-tenant` | 多门店平台部署 |

建议索引：

| 索引 | 字段 | 说明 |
| --- | --- | --- |
| `idx_store_status` | `status` | 按状态筛选 |

### `user`

后台用户表，包含门店老板、设计师、管理员等后台账号。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | 是 | 用户 ID |
| `store_id` | `BIGINT` | 是 | 所属门店 |
| `username` | `VARCHAR(64)` | 是 | 登录用户名 |
| `nickname` | `VARCHAR(64)` | 是 | 昵称 |
| `phone` | `VARCHAR(30)` | 否 | 手机号 |
| `email` | `VARCHAR(128)` | 否 | 邮箱 |
| `password_hash` | `VARCHAR(255)` | 是 | 密码哈希 |
| `status` | `VARCHAR(32)` | 是 | 用户状态 |
| `last_login_at` | `DATETIME(3)` | 否 | 最后登录时间 |
| `created_at` | `DATETIME(3)` | 是 | 创建时间 |
| `created_by` | `BIGINT` | 否 | 创建人 |
| `updated_at` | `DATETIME(3)` | 是 | 更新时间 |
| `updated_by` | `BIGINT` | 否 | 更新人 |
| `deleted` | `TINYINT(1)` | 是 | 逻辑删除 |

状态：

| 值 | 说明 |
| --- | --- |
| `active` | 正常 |
| `disabled` | 停用 |
| `locked` | 锁定 |

建议索引：

| 索引 | 字段 | 说明 |
| --- | --- | --- |
| `uk_user_store_username` | `store_id, username, deleted` | 同门店用户名唯一 |
| `uk_user_store_phone` | `store_id, phone, deleted` | 同门店手机号唯一，手机号为空时由业务处理 |
| `idx_user_store_status` | `store_id, status` | 员工列表筛选 |

### `role`

角色定义表。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | 是 | 角色 ID |
| `store_id` | `BIGINT` | 否 | 门店 ID，系统内置角色可为空 |
| `code` | `VARCHAR(32)` | 是 | 角色编码 |
| `name` | `VARCHAR(64)` | 是 | 角色名称 |
| `description` | `VARCHAR(255)` | 否 | 角色说明 |
| `created_at` | `DATETIME(3)` | 是 | 创建时间 |
| `created_by` | `BIGINT` | 否 | 创建人 |
| `updated_at` | `DATETIME(3)` | 是 | 更新时间 |
| `updated_by` | `BIGINT` | 否 | 更新人 |
| `deleted` | `TINYINT(1)` | 是 | 逻辑删除 |

预置角色：

| 编码 | 名称 | 说明 |
| --- | --- | --- |
| `owner` | 门店老板 | 管理门店项目和员工 |
| `designer` | 设计师 | 创建项目、上传版本、处理标注 |
| `admin` | 管理员 | 平台或系统管理员 |

建议索引：

| 索引 | 字段 | 说明 |
| --- | --- | --- |
| `uk_role_store_code` | `store_id, code, deleted` | 同门店角色编码唯一 |

### `user_role`

用户角色关系表。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | 是 | 关系 ID |
| `store_id` | `BIGINT` | 是 | 门店 ID |
| `user_id` | `BIGINT` | 是 | 用户 ID |
| `role_id` | `BIGINT` | 是 | 角色 ID |
| `created_at` | `DATETIME(3)` | 是 | 创建时间 |
| `created_by` | `BIGINT` | 否 | 创建人 |

建议索引：

| 索引 | 字段 | 说明 |
| --- | --- | --- |
| `uk_user_role` | `user_id, role_id` | 避免重复授权 |
| `idx_user_role_store_user` | `store_id, user_id` | 查询用户角色 |

### `project`

审稿项目主表。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | 是 | 项目 ID |
| `store_id` | `BIGINT` | 是 | 门店 ID |
| `name` | `VARCHAR(150)` | 是 | 项目名称 |
| `customer_name` | `VARCHAR(80)` | 否 | 客户名称 |
| `customer_contact` | `VARCHAR(120)` | 否 | 客户联系方式 |
| `owner_user_id` | `BIGINT` | 是 | 项目负责人 |
| `status` | `VARCHAR(32)` | 是 | 项目状态 |
| `current_version_id` | `BIGINT` | 否 | 当前展示版本 |
| `confirmed_version_id` | `BIGINT` | 否 | 最终确认版本 |
| `remark` | `VARCHAR(1000)` | 否 | 项目备注 |
| `archived_at` | `DATETIME(3)` | 否 | 归档时间 |
| `created_at` | `DATETIME(3)` | 是 | 创建时间 |
| `created_by` | `BIGINT` | 否 | 创建人 |
| `updated_at` | `DATETIME(3)` | 是 | 更新时间 |
| `updated_by` | `BIGINT` | 否 | 更新人 |
| `deleted` | `TINYINT(1)` | 是 | 逻辑删除 |

状态：

| 值 | 说明 |
| --- | --- |
| `draft` | 草稿 |
| `waiting_feedback` | 等待客户反馈 |
| `change_requested` | 客户已提交修改意见 |
| `waiting_confirm` | 等待客户确认 |
| `confirmed` | 已确认定稿 |
| `archived` | 已归档 |

建议索引：

| 索引 | 字段 | 说明 |
| --- | --- | --- |
| `idx_project_store_status` | `store_id, status, deleted` | 项目状态筛选 |
| `idx_project_store_owner` | `store_id, owner_user_id, deleted` | 我的项目 |
| `idx_project_customer` | `store_id, customer_name, deleted` | 客户搜索 |

### `project_status_log`

项目状态流转日志。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | 是 | 日志 ID |
| `store_id` | `BIGINT` | 是 | 门店 ID |
| `project_id` | `BIGINT` | 是 | 项目 ID |
| `from_status` | `VARCHAR(32)` | 否 | 变更前状态 |
| `to_status` | `VARCHAR(32)` | 是 | 变更后状态 |
| `action` | `VARCHAR(64)` | 是 | 触发动作 |
| `operator_type` | `VARCHAR(32)` | 是 | 操作人类型 |
| `operator_id` | `BIGINT` | 否 | 后台用户 ID |
| `operator_name` | `VARCHAR(80)` | 否 | 操作人名称 |
| `created_at` | `DATETIME(3)` | 是 | 创建时间 |

操作人类型：

| 值 | 说明 |
| --- | --- |
| `user` | 后台用户 |
| `customer` | 客户 |
| `system` | 系统 |

建议索引：

| 索引 | 字段 | 说明 |
| --- | --- | --- |
| `idx_project_status_log_project` | `store_id, project_id, created_at` | 项目时间线 |

### `project_version`

设计稿版本表。每次上传设计稿都创建新版本，不覆盖旧版本。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | 是 | 版本 ID |
| `store_id` | `BIGINT` | 是 | 门店 ID |
| `project_id` | `BIGINT` | 是 | 项目 ID |
| `version_no` | `INT` | 是 | 项目内递增版本号 |
| `version_name` | `VARCHAR(32)` | 是 | 展示名称，如 `V1` |
| `file_id` | `BIGINT` | 是 | 主文件 ID |
| `uploaded_by` | `BIGINT` | 否 | 上传人 |
| `description` | `VARCHAR(1000)` | 否 | 版本说明 |
| `is_current` | `TINYINT(1)` | 是 | 是否当前版本 |
| `is_confirmed` | `TINYINT(1)` | 是 | 是否已确认 |
| `confirmed_at` | `DATETIME(3)` | 否 | 确认时间 |
| `created_at` | `DATETIME(3)` | 是 | 创建时间 |
| `created_by` | `BIGINT` | 否 | 创建人 |
| `updated_at` | `DATETIME(3)` | 是 | 更新时间 |
| `updated_by` | `BIGINT` | 否 | 更新人 |
| `deleted` | `TINYINT(1)` | 是 | 逻辑删除 |

建议索引：

| 索引 | 字段 | 说明 |
| --- | --- | --- |
| `uk_project_version_no` | `project_id, version_no` | 项目内版本号唯一 |
| `idx_project_version_project` | `store_id, project_id, created_at` | 版本列表 |
| `idx_project_version_current` | `project_id, is_current` | 当前版本查询 |

### `file_object`

文件元数据表。MinIO 保存文件内容，MySQL 只保存对象 key 和业务关系。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | 是 | 文件 ID |
| `store_id` | `BIGINT` | 是 | 门店 ID |
| `project_id` | `BIGINT` | 否 | 项目 ID |
| `version_id` | `BIGINT` | 否 | 版本 ID |
| `object_key` | `VARCHAR(512)` | 是 | MinIO 对象 key |
| `bucket` | `VARCHAR(100)` | 是 | MinIO bucket |
| `original_filename` | `VARCHAR(255)` | 是 | 原始文件名 |
| `safe_filename` | `VARCHAR(255)` | 是 | 安全文件名 |
| `file_ext` | `VARCHAR(20)` | 否 | 文件扩展名 |
| `mime_type` | `VARCHAR(100)` | 否 | MIME 类型 |
| `file_size` | `BIGINT` | 是 | 文件大小，单位字节 |
| `sha256` | `CHAR(64)` | 否 | 文件 SHA-256 |
| `file_role` | `VARCHAR(32)` | 是 | 文件角色 |
| `created_at` | `DATETIME(3)` | 是 | 创建时间 |
| `created_by` | `BIGINT` | 否 | 上传人 |
| `updated_at` | `DATETIME(3)` | 是 | 更新时间 |
| `updated_by` | `BIGINT` | 否 | 更新人 |
| `deleted` | `TINYINT(1)` | 是 | 逻辑删除 |

文件角色：

| 值 | 说明 |
| --- | --- |
| `original` | 原始设计稿 |
| `preview` | 预览图或预览 PDF |
| `attachment` | 附件 |
| `confirmation` | 确认单导出文件 |
| `temp` | 临时上传文件 |

建议索引：

| 索引 | 字段 | 说明 |
| --- | --- | --- |
| `uk_file_object_key` | `bucket, object_key` | 对象唯一 |
| `idx_file_project_version` | `store_id, project_id, version_id` | 按版本查询文件 |
| `idx_file_sha256` | `sha256` | 文件去重或审计 |

### `review_link`

客户审稿链接表。用于客户免登录访问项目审稿页。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | 是 | 链接 ID |
| `store_id` | `BIGINT` | 是 | 门店 ID |
| `project_id` | `BIGINT` | 是 | 项目 ID |
| `current_version_id` | `BIGINT` | 否 | 链接当前指向版本 |
| `token_hash` | `CHAR(64)` | 是 | 访问 token 哈希 |
| `status` | `VARCHAR(32)` | 是 | 链接状态 |
| `expires_at` | `DATETIME(3)` | 否 | 过期时间 |
| `max_access_count` | `INT` | 否 | 最大访问次数 |
| `access_count` | `INT` | 是 | 已访问次数 |
| `last_access_at` | `DATETIME(3)` | 否 | 最近访问时间 |
| `created_at` | `DATETIME(3)` | 是 | 创建时间 |
| `created_by` | `BIGINT` | 否 | 创建人 |
| `updated_at` | `DATETIME(3)` | 是 | 更新时间 |
| `updated_by` | `BIGINT` | 否 | 更新人 |
| `deleted` | `TINYINT(1)` | 是 | 逻辑删除 |

状态：

| 值 | 说明 |
| --- | --- |
| `active` | 正常 |
| `disabled` | 停用 |
| `expired` | 已过期 |

建议索引：

| 索引 | 字段 | 说明 |
| --- | --- | --- |
| `uk_review_link_token_hash` | `token_hash` | 令牌唯一 |
| `idx_review_link_project` | `store_id, project_id, status` | 项目链接查询 |

### `review_access_log`

客户访问日志表。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | 是 | 日志 ID |
| `store_id` | `BIGINT` | 是 | 门店 ID |
| `review_link_id` | `BIGINT` | 是 | 审稿链接 ID |
| `project_id` | `BIGINT` | 是 | 项目 ID |
| `version_id` | `BIGINT` | 否 | 访问时版本 ID |
| `ip` | `VARCHAR(64)` | 否 | 访问 IP |
| `user_agent` | `VARCHAR(512)` | 否 | User-Agent |
| `referer` | `VARCHAR(512)` | 否 | 来源页面 |
| `accessed_at` | `DATETIME(3)` | 是 | 访问时间 |

建议索引：

| 索引 | 字段 | 说明 |
| --- | --- | --- |
| `idx_review_access_project` | `store_id, project_id, accessed_at` | 项目访问记录 |
| `idx_review_access_link` | `review_link_id, accessed_at` | 链接访问记录 |

### `annotation`

标注意见表。标注必须绑定具体项目和具体版本。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | 是 | 标注 ID |
| `store_id` | `BIGINT` | 是 | 门店 ID |
| `project_id` | `BIGINT` | 是 | 项目 ID |
| `version_id` | `BIGINT` | 是 | 版本 ID |
| `review_link_id` | `BIGINT` | 否 | 来源审稿链接 |
| `type` | `VARCHAR(32)` | 是 | 标注类型 |
| `x_ratio` | `DECIMAL(10,6)` | 否 | X 相对坐标 |
| `y_ratio` | `DECIMAL(10,6)` | 否 | Y 相对坐标 |
| `width_ratio` | `DECIMAL(10,6)` | 否 | 宽度相对比例 |
| `height_ratio` | `DECIMAL(10,6)` | 否 | 高度相对比例 |
| `content` | `VARCHAR(2000)` | 是 | 修改意见 |
| `customer_name` | `VARCHAR(80)` | 否 | 客户名称 |
| `customer_contact` | `VARCHAR(120)` | 否 | 客户联系方式 |
| `status` | `VARCHAR(32)` | 是 | 标注状态 |
| `resolved_by` | `BIGINT` | 否 | 处理人 |
| `resolved_at` | `DATETIME(3)` | 否 | 处理时间 |
| `created_at` | `DATETIME(3)` | 是 | 创建时间 |
| `created_by` | `BIGINT` | 否 | 创建人，客户提交可为空 |
| `updated_at` | `DATETIME(3)` | 是 | 更新时间 |
| `updated_by` | `BIGINT` | 否 | 更新人 |
| `deleted` | `TINYINT(1)` | 是 | 逻辑删除 |

标注类型：

| 值 | 说明 |
| --- | --- |
| `point` | 点位标注 |
| `rect` | 矩形区域标注 |
| `text` | 纯文字意见 |

标注状态：

| 值 | 说明 |
| --- | --- |
| `open` | 待处理 |
| `resolved` | 已处理 |
| `ignored` | 已忽略 |

建议索引：

| 索引 | 字段 | 说明 |
| --- | --- | --- |
| `idx_annotation_version` | `store_id, project_id, version_id, status` | 版本标注列表 |
| `idx_annotation_project` | `store_id, project_id, created_at` | 项目标注列表 |

### `annotation_comment`

标注回复表。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | 是 | 回复 ID |
| `store_id` | `BIGINT` | 是 | 门店 ID |
| `annotation_id` | `BIGINT` | 是 | 标注 ID |
| `project_id` | `BIGINT` | 是 | 项目 ID |
| `version_id` | `BIGINT` | 是 | 版本 ID |
| `reply_type` | `VARCHAR(32)` | 是 | 回复人类型 |
| `reply_user_id` | `BIGINT` | 否 | 后台用户 ID |
| `reply_name` | `VARCHAR(80)` | 否 | 回复人名称 |
| `content` | `VARCHAR(2000)` | 是 | 回复内容 |
| `created_at` | `DATETIME(3)` | 是 | 创建时间 |

回复人类型：

| 值 | 说明 |
| --- | --- |
| `user` | 后台用户 |
| `customer` | 客户 |
| `system` | 系统 |

建议索引：

| 索引 | 字段 | 说明 |
| --- | --- | --- |
| `idx_annotation_comment` | `annotation_id, created_at` | 标注回复列表 |

### `confirmation_record`

客户确认记录表。确认动作必须绑定具体版本。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | 是 | 确认记录 ID |
| `store_id` | `BIGINT` | 是 | 门店 ID |
| `project_id` | `BIGINT` | 是 | 项目 ID |
| `version_id` | `BIGINT` | 是 | 确认版本 ID |
| `review_link_id` | `BIGINT` | 否 | 审稿链接 ID |
| `customer_name` | `VARCHAR(80)` | 否 | 确认人名称 |
| `customer_contact` | `VARCHAR(120)` | 否 | 确认人联系方式 |
| `confirmed_at` | `DATETIME(3)` | 是 | 确认时间 |
| `ip` | `VARCHAR(64)` | 否 | 确认 IP |
| `user_agent` | `VARCHAR(512)` | 否 | User-Agent |
| `status` | `VARCHAR(32)` | 是 | 确认记录状态 |
| `voided_at` | `DATETIME(3)` | 否 | 作废时间 |
| `void_reason` | `VARCHAR(500)` | 否 | 作废原因 |
| `created_at` | `DATETIME(3)` | 是 | 创建时间 |

状态：

| 值 | 说明 |
| --- | --- |
| `valid` | 有效 |
| `voided` | 已作废 |

建议索引：

| 索引 | 字段 | 说明 |
| --- | --- | --- |
| `uk_confirmation_project_valid` | `project_id, status` | 一个项目默认只允许一条有效确认记录，由业务保证 `valid` 唯一 |
| `idx_confirmation_version` | `store_id, project_id, version_id` | 按版本查询确认 |
| `idx_confirmation_time` | `store_id, confirmed_at` | 确认记录列表 |

### `audit_log`

通用审计日志表。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | 是 | 日志 ID |
| `store_id` | `BIGINT` | 是 | 门店 ID |
| `action` | `VARCHAR(64)` | 是 | 动作类型 |
| `target_type` | `VARCHAR(64)` | 是 | 业务对象类型 |
| `target_id` | `BIGINT` | 否 | 业务对象 ID |
| `operator_type` | `VARCHAR(32)` | 是 | 操作人类型 |
| `operator_id` | `BIGINT` | 否 | 后台用户 ID |
| `operator_name` | `VARCHAR(80)` | 否 | 操作人名称 |
| `summary` | `VARCHAR(500)` | 是 | 日志摘要 |
| `extra_json` | `JSON` | 否 | 扩展信息 |
| `created_at` | `DATETIME(3)` | 是 | 创建时间 |

常见动作：

| 值 | 说明 |
| --- | --- |
| `project_created` | 创建项目 |
| `version_uploaded` | 上传版本 |
| `review_link_created` | 创建审稿链接 |
| `annotation_created` | 客户提交标注 |
| `annotation_resolved` | 处理标注 |
| `project_confirmed` | 客户确认定稿 |

建议索引：

| 索引 | 字段 | 说明 |
| --- | --- | --- |
| `idx_audit_target` | `store_id, target_type, target_id, created_at` | 业务对象时间线 |
| `idx_audit_action` | `store_id, action, created_at` | 动作查询 |

### `notification`

站内通知表。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | 是 | 通知 ID |
| `store_id` | `BIGINT` | 是 | 门店 ID |
| `receiver_user_id` | `BIGINT` | 是 | 接收人 |
| `project_id` | `BIGINT` | 否 | 关联项目 |
| `type` | `VARCHAR(64)` | 是 | 通知类型 |
| `title` | `VARCHAR(150)` | 是 | 标题 |
| `content` | `VARCHAR(1000)` | 否 | 内容 |
| `read_at` | `DATETIME(3)` | 否 | 已读时间 |
| `created_at` | `DATETIME(3)` | 是 | 创建时间 |
| `created_by` | `BIGINT` | 否 | 创建人 |
| `updated_at` | `DATETIME(3)` | 是 | 更新时间 |
| `updated_by` | `BIGINT` | 否 | 更新人 |
| `deleted` | `TINYINT(1)` | 是 | 逻辑删除 |

建议索引：

| 索引 | 字段 | 说明 |
| --- | --- | --- |
| `idx_notification_receiver` | `store_id, receiver_user_id, read_at, created_at` | 用户通知列表 |

### `system_config`

业务配置表。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | 是 | 配置 ID |
| `store_id` | `BIGINT` | 否 | 门店 ID，系统默认配置可为空 |
| `config_key` | `VARCHAR(100)` | 是 | 配置键 |
| `config_value` | `VARCHAR(1000)` | 是 | 配置值 |
| `value_type` | `VARCHAR(32)` | 是 | 值类型 |
| `description` | `VARCHAR(255)` | 否 | 配置说明 |
| `created_at` | `DATETIME(3)` | 是 | 创建时间 |
| `created_by` | `BIGINT` | 否 | 创建人 |
| `updated_at` | `DATETIME(3)` | 是 | 更新时间 |
| `updated_by` | `BIGINT` | 否 | 更新人 |
| `deleted` | `TINYINT(1)` | 是 | 逻辑删除 |

建议配置键：

| 配置键 | 示例值 | 说明 |
| --- | --- | --- |
| `upload.max-file-size-mb` | `100` | 最大上传文件大小 |
| `upload.allowed-exts` | `jpg,jpeg,png,webp,pdf,ai,psd,cdr` | 允许上传扩展名 |
| `review-link.default-ttl-days` | `30` | 审稿链接默认有效期 |

建议索引：

| 索引 | 字段 | 说明 |
| --- | --- | --- |
| `uk_system_config_key` | `store_id, config_key, deleted` | 配置键唯一 |

## Redis 缓存键设计

Redis 只保存会话、短期状态、热点用户信息和轻量控制数据，不保存最终确认记录、审计日志、访问日志等关键业务事实。

统一前缀：

```text
proofly:{env}:...
```

`{env}` 取值建议为 `dev`、`test`、`prod`。

### 后台访问令牌

```text
proofly:{env}:auth:access:{tokenId}
```

| 项 | 说明 |
| --- | --- |
| 类型 | `String`，JSON |
| TTL | 2 小时 |
| 用途 | 后台用户 access token 会话 |
| 值 | `userId`、`storeId`、`roles`、`loginAt`、`expiresAt` |

示例值：

```json
{
  "userId": 10001,
  "storeId": 20001,
  "roles": ["owner", "designer"],
  "loginAt": "2026-05-06T10:00:00+08:00",
  "expiresAt": "2026-05-06T12:00:00+08:00"
}
```

### 后台刷新令牌

```text
proofly:{env}:auth:refresh:{tokenId}
```

| 项 | 说明 |
| --- | --- |
| 类型 | `String`，JSON |
| TTL | 7 天 |
| 用途 | 刷新后台 access token |
| 值 | `userId`、`storeId`、`deviceId`、`userAgent`、`loginAt` |

### 当前用户摘要

```text
proofly:{env}:user:profile:{userId}
```

| 项 | 说明 |
| --- | --- |
| 类型 | `String`，JSON |
| TTL | 30 分钟 |
| 用途 | 缓存当前用户基础信息，减少频繁查库 |
| 值 | `userId`、`storeId`、`nickname`、`phone`、`status`、`roles` |

用户资料、状态或角色变更后，需要删除该 key。

### 用户角色权限

```text
proofly:{env}:user:roles:{userId}
```

| 项 | 说明 |
| --- | --- |
| 类型 | `Set` 或 `String` JSON 数组 |
| TTL | 30 分钟 |
| 用途 | 缓存用户角色编码 |
| 值 | `owner`、`designer`、`admin` 等角色编码 |

### 登出黑名单

```text
proofly:{env}:auth:blacklist:{tokenId}
```

| 项 | 说明 |
| --- | --- |
| 类型 | `String` |
| TTL | 到原 access token 过期 |
| 用途 | 登出或强制失效 token |
| 值 | `logout`、`disabled`、`reset_password` 等原因 |

### 客户审稿 token

```text
proofly:{env}:review:token:{token}
```

| 项 | 说明 |
| --- | --- |
| 类型 | `String`，JSON |
| TTL | 与 `review_link.expires_at` 对齐；无过期时间时可设置 24 小时短缓存 |
| 用途 | 加速客户公开审稿链接校验 |
| 值 | `reviewLinkId`、`storeId`、`projectId`、`currentVersionId`、`status`、`expiresAt` |

示例值：

```json
{
  "reviewLinkId": 30001,
  "storeId": 20001,
  "projectId": 40001,
  "currentVersionId": 50002,
  "status": "active",
  "expiresAt": "2026-06-05T23:59:59+08:00"
}
```

项目当前版本变化、链接停用、链接重新生成时，需要删除或刷新该 key。

### 客户审稿访问限流

```text
proofly:{env}:rate:review:{token}:{ip}
```

| 项 | 说明 |
| --- | --- |
| 类型 | `String` 或 `Counter` |
| TTL | 1 分钟 |
| 用途 | 客户公开审稿链接防刷 |
| 值 | 当前窗口访问次数 |

### 登录失败计数

```text
proofly:{env}:auth:login-fail:{usernameOrPhone}
```

| 项 | 说明 |
| --- | --- |
| 类型 | `String` 或 `Counter` |
| TTL | 15 分钟 |
| 用途 | 登录失败计数和临时锁定判断 |
| 值 | 当前窗口失败次数 |

### Redis 使用约束

- 不把 `confirmation_record`、`audit_log`、`review_access_log` 只写入 Redis。
- Redis 缓存命中后仍需遵守门店隔离和权限判断。
- 关键业务状态变更后，优先删除缓存而不是尝试局部修改复杂缓存。
- 客户审稿 token 的 Redis key 可保存明文 token，但 MySQL 只保存 `token_hash`。

## MinIO 对象路径设计

Bucket 默认使用配置：

```text
MINIO_BUCKET=proofly
```

业务隔离主要依赖对象 key 中的 `storeId`，而不是为每个门店创建独立 bucket。

### 路径规则

设计稿原文件：

```text
stores/{storeId}/projects/{projectId}/versions/{versionId}/original/{fileId}-{safeFilename}
```

预览文件：

```text
stores/{storeId}/projects/{projectId}/versions/{versionId}/preview/{fileId}-{pageOrSize}.{ext}
```

版本附件：

```text
stores/{storeId}/projects/{projectId}/versions/{versionId}/attachment/{fileId}-{safeFilename}
```

确认单导出文件：

```text
stores/{storeId}/projects/{projectId}/confirmations/{confirmationId}/confirmation-{confirmationId}.pdf
```

临时上传文件：

```text
stores/{storeId}/temp/uploads/{yyyyMMdd}/{uploadId}/{safeFilename}
```

### 路径变量

| 变量 | 说明 |
| --- | --- |
| `storeId` | 门店 ID |
| `projectId` | 项目 ID |
| `versionId` | 版本 ID |
| `fileId` | 文件元数据 ID |
| `confirmationId` | 确认记录 ID |
| `uploadId` | 临时上传 ID |
| `yyyyMMdd` | 上传日期 |
| `safeFilename` | 安全文件名 |
| `pageOrSize` | PDF 页码、图片尺寸或预览规格，如 `page-1`、`thumb-800` |
| `ext` | 文件扩展名 |

### 文件名约定

- `safeFilename` 只保留字母、数字、点、下划线和短横线。
- 中文文件名和原始文件名保存到 MySQL `file_object.original_filename`。
- MinIO object key 中必须包含 `fileId`，不能只依赖原文件名。
- 同一个版本的原文件、预览文件、附件分目录保存，避免覆盖。
- 临时上传文件只允许在 `temp/uploads` 路径下清理。

### 文件删除约定

- 未关联项目版本的临时文件可以按 TTL 清理。
- 已关联 `project_version` 的原文件和预览文件默认不删除。
- 已确认版本关联文件不允许物理删除。
- 确认单导出文件不允许覆盖；重新导出应生成新的 `file_object` 记录或保留导出版本。

## 后续扩展

MVP 阶段暂不设计套餐、账单、AI 相关表。后续进入 SaaS 化或 AI 能力开发时，再追加：

- `subscription`：门店订阅。
- `usage_record`：项目数、存储空间、员工数等用量。
- `billing_record`：账单记录。
- `ai_task`：AI 总结、智能建议等异步任务。
- `knowledge_document`：门店知识库文档。
