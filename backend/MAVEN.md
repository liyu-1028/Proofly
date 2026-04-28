# Maven 文件约定

本文档记录审稿宝后端项目使用 Maven 的本地约定。该约定只用于开发协作和命令执行，不修改本机 Maven 安装目录，也不把本机私有配置文件提交到仓库。

## 本地 Maven 路径

| 项目 | 路径 |
| --- | --- |
| Maven 安装目录 | `/opt/homebrew/Cellar/maven/3.9.9/libexec` |
| Maven 可执行文件 | `/opt/homebrew/Cellar/maven/3.9.9/libexec/bin/mvn` |
| Maven 配置文件 | `/opt/homebrew/Cellar/maven/3.9.9/libexec/conf/settings2.xml` |
| JDK 17 JAVA_HOME | `/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home` |

## 依赖仓库

- 依赖仓库使用 Maven 默认本地仓库：`~/.m2/repository`。
- 项目内不指定自定义本地仓库路径。
- `pom.xml` 中不写死个人镜像仓库、本地仓库路径或本机绝对路径。
- 本机私有 Maven 配置文件 `settings2.xml` 不提交到仓库。

## 命令执行目录

所有 Maven 命令默认在 `backend/` 目录中执行。

```bash
cd backend
```

## 常用命令

### 编译

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home /opt/homebrew/Cellar/maven/3.9.9/libexec/bin/mvn -s /opt/homebrew/Cellar/maven/3.9.9/libexec/conf/settings2.xml -q -DskipTests compile
```

### 测试

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home /opt/homebrew/Cellar/maven/3.9.9/libexec/bin/mvn -s /opt/homebrew/Cellar/maven/3.9.9/libexec/conf/settings2.xml test
```

### 启动后端

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home /opt/homebrew/Cellar/maven/3.9.9/libexec/bin/mvn -s /opt/homebrew/Cellar/maven/3.9.9/libexec/conf/settings2.xml spring-boot:run
```

## 协作约定

- 后端项目初始化后，优先使用本文档中的 Maven 可执行文件和配置文件执行构建命令。
- Spring Boot 3.5.x 要求使用 JDK 17 或更高版本。本机 Maven 默认可能使用 Java 8，执行后端命令时需要显式设置上面的 `JAVA_HOME`。
- 如果本机 Maven 路径变化，只更新本文档或在本机 shell 中配置别名，不把个人环境路径写入业务代码。
- 后续如需要 Maven Wrapper，应单独讨论后再加入，避免和当前本机 Maven 约定混淆。
