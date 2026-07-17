# ===== 阶段 1：构建 =====
FROM node:20-alpine AS build
WORKDIR /workspace

# 优先拷贝 lock 文件以利用 Docker 层缓存
COPY package.json package-lock.json* ./
RUN npm install --no-audit --no-fund

COPY . .
RUN npm run build

# ===== 阶段 2：运行时（Nginx 静态服务） =====
FROM nginx:1.27-alpine
WORKDIR /usr/share/nginx/html

# Nginx 配置：SPA fallback + 后端代理
COPY --from=build /workspace/dist ./
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --retries=5 \
  CMD wget -qO- http://localhost/ >/dev/null || exit 1