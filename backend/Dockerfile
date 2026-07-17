# ===== 阶段 1：构建 =====
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# 先拷贝 POM，利用 Docker 层缓存
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# 再拷贝源码并打包（跳过测试，部署前在 CI 中独立跑测试）
COPY src ./src
RUN mvn -B -q -DskipTests package

# ===== 阶段 2：运行时 =====
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 时区与中文环境
RUN apk add --no-cache tzdata curl \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone \
    && apk del tzdata
ENV LANG=zh_CN.UTF-8 \
    LC_ALL=zh_CN.UTF-8 \
    TZ=Asia/Shanghai \
    JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75 -Dfile.encoding=UTF-8"

# 从构建阶段复制产物
COPY --from=build /workspace/target/*.jar /app/app.jar

EXPOSE 8080

# 启动前等待数据库就绪
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
  CMD curl -fsS http://localhost:8080/api/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]