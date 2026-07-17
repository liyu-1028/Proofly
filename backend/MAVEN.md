# Maven 构建约定

本文档说明审稿宝后端的 Maven 使用约定。

## 版本要求

| 工具 | 最低版本 |
|------|---------|
| JDK | 17（Spring Boot 3.5.x 要求） |
| Maven | 3.9+ |

## 环境变量

```bash
# Linux / macOS
export JAVA_HOME=/path/to/jdk-17

# 验证
java -version   # 应输出 17.x
mvn -v          # 应输出 Java version: 17.x
```

> 💡 如果本机默认 `mvn` 使用 Java 8，需要显式设置 `JAVA_HOME` 后再调用 `mvn`。

## 依赖仓库

- 默认使用 Maven 中央仓库。
- 建议配置国内镜像加速（如阿里云），编辑 `~/.m2/settings.xml`：

  ```xml
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <url>https://maven.aliyun.com/repository/public</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
  </mirrors>
  ```

- 本机私有 Maven 配置（如 `settings2.xml`）**不要**提交到仓库。

## 命令执行目录

所有 Maven 命令默认在 `backend/` 目录中执行：

```bash
cd backend
```

## 常用命令

### 编译

```bash
mvn -B -q -DskipTests package
```

### 运行测试

```bash
mvn test
```

### 启动后端

```bash
mvn spring-boot:run
```

启动后访问：

- 健康检查：<http://localhost:8080/api/health>
- API 文档：<http://localhost:8080/swagger-ui.html>

## 协作约定

- 不在 `pom.xml` 中写死个人镜像仓库、本地仓库路径或本机绝对路径。
- 后端团队成员可使用本机任意 Maven 安装方式（Homebrew、SDKMAN、手动安装），但提交的命令示例应使用通用 `mvn` 命令。
- CI 中使用固定 Maven 版本（推荐 3.9.x）。

## 故障排查

**Q: `mvn` 报 "java.lang.IllegalArgumentException: Unsupported class file major version"**
A: JDK 版本过低，必须使用 JDK 17+。

**Q: 依赖下载缓慢或超时**
A: 配置阿里云 Maven 镜像（见上文）。

**Q: 端口 8080 被占用**
A: 通过 `SERVER_PORT` 环境变量覆盖：
```bash
SERVER_PORT=9090 mvn spring-boot:run
```