# Proofly 快速启动指南

本指南帮助你在 **5 分钟内** 在本地把 Proofly 跑起来。

## 一、前置依赖

| 工具 | 最低版本 | 说明 |
|------|---------|------|
| Docker | 24+ | 用于运行 MySQL / Redis / MinIO |
| Docker Compose | v2+ | `docker compose` 命令 |
| JDK | 17 | 后端编译运行 |
| Node.js | 20+ | 前端构建 |
| Maven | 3.9+ | 构建后端；本项目当前未自带 Maven Wrapper，需本地安装 |

> 💡 如果你不打算本地编译后端，只用 Docker 启动后端，则不需要 JDK 和 Maven。

## 二、克隆并初始化

```bash
git clone https://github.com/liyu-1028/Proofly.git
cd Proofly

# 复制环境变量模板
cp .env.example .env
# （可选）按需修改 .env 中的密码、密钥等

# ⚠️ 关键提示：Spring 不会自动读取仓库根的 .env，
#    启动后端时需要让这些变量进入进程环境：
#      - IntelliJ IDEA：Run/Debug Configurations → Environment variables 中粘贴 .env 内容
#      - VS Code：launch.json 的 env 字段，或使用 dotenv 扩展
#      - 命令行：set -a && source .env && set +a  （bash/zsh）
#      - 或直接 export 每个变量
```

## 二点五、（可选）生成 RSA 密钥对

`PROOFLY_AUTH_RSA_PRIVATE_KEY` 必须显式注入，否则后端启动会失败：

```bash
mkdir -p backend/keys
openssl genpkey -algorithm RSA -out backend/keys/private.pem -pkeyopt rsa_keygen_bits:2048
openssl rsa -in backend/keys/private.pem -pubout -out backend/keys/public.pem

# 把 public.pem 内容（去掉头尾标记与换行，拼成单行）写入 .env 的 PROOFLY_AUTH_RSA_PUBLIC_KEY
# 把 private.pem 内容（同样处理）写入 PROOFLY_AUTH_RSA_PRIVATE_KEY

# 也可以让 application.yml 改为引用 classpath:keys/public.pem，详见 docs/security-todo.md（TODO）
```

> ⚠️ 不要把 `backend/keys/` 提交到仓库。已通过 `.gitignore` 中的 `*.pem`/本地约定忽略。
> 如需精确忽略，可加入 `backend/keys/` 到 `.gitignore`。

## 三、启动基础设施（MySQL / Redis / MinIO）

```bash
docker compose up -d mysql redis minio minio-init
```

启动后会自动创建：

- MySQL 8.0（默认账号 `root` / 密码 `proofly_dev`）
- Redis 7.4（无密码）
- MinIO（控制台 <http://localhost:9001>，默认 `minioadmin` / `minioadmin`）
- MinIO bucket `proofly`（自动创建）

检查状态：

```bash
docker compose ps
```

健康检查应全部显示 `healthy`。

## 四、初始化数据库

> 📌 **本步骤是唯一推荐的数据库初始化路径**。`docker-compose.yml` 没有挂载自动初始化脚本，
> 避免重复执行 `CREATE TABLE IF NOT EXISTS` 产生噪声日志，也方便后续升级时受控迁移。

等待 MySQL 健康检查通过后，从宿主机执行：

```bash
# 等待 MySQL 健康检查通过（约 10–30 秒）
until docker compose exec -T mysql mysqladmin ping -h localhost -uroot -pproofly_dev --silent; do
  sleep 2
done

# 1) 执行 DDL 建表（必须指定 utf8mb4，否则容器内 mysql 客户端默认 latin1，中文会乱码）
docker compose exec -T mysql mysql -uroot -pproofly_dev --default-character-set=utf8mb4 < docs/mysql-schema.sql

# 2) （可选）执行开发种子数据：默认门店 + admin 账号
docker compose exec -T mysql mysql -uroot -pproofly_dev --default-character-set=utf8mb4 proofly < docs/seed-dev.sql
```

> 种子数据插入后会创建：
> - 门店：`默认门店`
> - 用户名：`admin`，密码：`admin123`（⚠️ 登录后立即修改）

## 五、启动后端

### 方式 A：本地 Maven 启动

```bash
cd backend
./mvnw spring-boot:run
```

首次启动会自动下载依赖并编译，约 2-3 分钟。

### 方式 B：Docker 启动

```bash
docker compose up -d backend
```

镜像构建约 3-5 分钟。

后端启动后访问：

- 健康检查：<http://localhost:8080/api/health>
- API 文档：<http://localhost:8080/swagger-ui.html>

## 六、启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在 <http://localhost:5173>。

## 七、体验完整流程

1. **自助注册**：访问 <http://localhost:5173/register>，创建一个新门店账号
2. **创建项目**：登录后在工作台创建第一个项目
3. **上传设计稿**：项目详情页 → 上传新版本
4. **生成审稿链接**：把链接发给客户
5. **客户标注**：在公开审稿页提交标注（无需登录）
6. **客户确认**：客户点击"确认定稿"按钮
7. **套餐升级**：在设置页选择套餐 → 跳转模拟收银台 → 确认付款
8. **Webhook 自动续期**：付款完成后门店套餐自动延期

## 八、清理

```bash
# 停止所有服务
docker compose down

# 停止并清理数据卷（彻底重置）
docker compose down -v
```

## 九、生产部署要点

⚠️ **本指南仅供本地开发**。生产部署需要额外关注：

1. **所有密钥必须通过环境变量注入**：`PROOFLY_AUTH_JWT_SECRET`、`PROOFLY_AUTH_RSA_PRIVATE_KEY` 等默认值仅供开发。
2. **不要执行 seed-dev.sql**：由运营手动创建账号。
3. **数据库密码**：使用强密码并通过 `DB_PASSWORD` 环境变量覆盖。
4. **MinIO**：建议使用生产 MinIO 集群 + TLS，并配置访问控制。
5. **HTTPS**：在 Nginx / API Gateway 层终止 TLS，不要让后端直接暴露 80/443。

## 十、常见问题

**Q: 启动后端时报 "Access denied for user 'root'"**
A: 检查 `.env` 中的 `MYSQL_ROOT_PASSWORD` 和 `DB_PASSWORD` 是否一致，并与 docker-compose 中 MySQL 服务匹配。

**Q: MinIO bucket 不存在**
A: 等待 `minio-init` 容器执行完成，或手动登录 <http://localhost:9001> 创建 `proofly` bucket。

**Q: 端口冲突**
A: 修改 `docker-compose.yml` 中各服务的 `ports` 段（如 `3306:3306` → `3307:3306`），并在 `.env` 中同步更新对应连接字符串。

**Q: Maven Wrapper 下载慢**
A: 配置 Maven 镜像（如阿里云），编辑 `~/.m2/settings.xml`：
```xml
<mirrors>
  <mirror>
    <id>aliyun</id>
    <url>https://maven.aliyun.com/repository/public</url>
    <mirrorOf>central</mirrorOf>
  </mirror>
</mirrors>
```

---

如果遇到问题，欢迎在 GitHub Issues 中反馈。