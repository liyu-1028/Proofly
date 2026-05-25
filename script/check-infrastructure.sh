#!/bin/bash

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}开始检查后端基础设施依赖...${NC}"

# 初始化错误标志
FAILED=0

# 1. 检查 Redis (默认端口 6379)
if nc -z localhost 6379 >/dev/null 2>&1; then
    echo -e "${GREEN}✔ Redis 服务可用 (localhost:6379)${NC}"
else
    echo -e "${RED}✘ Redis 服务不可用 (localhost:6379)${NC}"
    FAILED=1
fi

# 2. 检查 MinIO (默认端口 9000, 访问健康检查接口)
if curl -s -f http://localhost:9000/minio/health/live >/dev/null 2>&1; then
    echo -e "${GREEN}✔ MinIO 服务可用 (http://localhost:9000)${NC}"
else
    echo -e "${RED}✘ MinIO 服务不可用 (http://localhost:9000)${NC}"
    FAILED=1
fi

# 3. 检查 MySQL (默认端口 3306)
if nc -z localhost 3306 >/dev/null 2>&1; then
    echo -e "${GREEN}✔ MySQL 服务可用 (localhost:3306)${NC}"
else
    echo -e "${RED}✘ MySQL 服务不可用 (localhost:3306)${NC}"
    FAILED=1
fi

# 判断最终结果
if [ $FAILED -eq 1 ]; then
    echo -e "\n${RED}错误: 基础设施依赖检查未通过。${NC}"
    echo -e "${YELLOW}请确保已启动 Docker 容器: docker compose -f docker/docker-compose.dev.yml up -d${NC}"
    exit 1
else
    echo -e "\n${GREEN}恭喜: 所有基础设施依赖已就绪。${NC}"
    exit 0
fi
