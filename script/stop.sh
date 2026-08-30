#!/bin/bash
#
# 审稿宝本地开发一键停止脚本
#
# 用法:
#   ./stop.sh            # 停止前端 + 后端（默认）
#   ./stop.sh backend    # 只停止后端
#   ./stop.sh frontend   # 只停止前端

set -u

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

RUN_DIR="$ROOT_DIR/.run"

BACKEND_PORT="${SERVER_PORT:-8080}"
FRONTEND_PORT=5173

BACKEND_PID_FILE="$RUN_DIR/backend.pid"
FRONTEND_PID_FILE="$RUN_DIR/frontend.pid"

log_info()  { echo -e "${GREEN}[INFO]${NC} $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

port_in_use() {
    lsof -iTCP:"$1" -sTCP:LISTEN >/dev/null 2>&1
}

# 停止单个服务: 先按 PID 文件优雅退出，再清理端口残留进程
stop_service() {
    local name=$1 pidfile=$2 port=$3
    local stopped=0

    if [ -f "$pidfile" ]; then
        local pid
        pid="$(cat "$pidfile" 2>/dev/null || true)"
        if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
            log_info "停止 $name (PID $pid)..."
            kill "$pid" 2>/dev/null || true
            # 最多等待 10 秒优雅退出
            local i=0
            while kill -0 "$pid" 2>/dev/null && [ "$i" -lt 10 ]; do
                sleep 1
                i=$((i + 1))
            done
            if kill -0 "$pid" 2>/dev/null; then
                log_warn "$name 未响应 SIGTERM，强制结束"
                kill -9 "$pid" 2>/dev/null || true
            fi
            stopped=1
        fi
        rm -f "$pidfile"
    fi

    # 兜底清理：PID 文件丢失或 mvn fork 出的子 JVM 可能仍占用端口
    if port_in_use "$port"; then
        local pids
        pids="$(lsof -ti tcp:"$port" -sTCP:LISTEN 2>/dev/null || true)"
        if [ -n "$pids" ]; then
            log_warn "清理端口 $port 上的残留进程: $(echo "$pids" | tr '\n' ' ')"
            # shellcheck disable=SC2086
            kill $pids 2>/dev/null || true
            sleep 2
            if port_in_use "$port"; then
                # shellcheck disable=SC2086
                kill -9 $pids 2>/dev/null || true
            fi
            stopped=1
        fi
    fi

    if [ "$stopped" -eq 1 ]; then
        log_info "$name 已停止 (端口 $port)"
    else
        log_warn "$name 未在运行 (端口 $port)"
    fi
}

target="${1:-all}"

case "$target" in
    backend)
        stop_service "后端" "$BACKEND_PID_FILE" "$BACKEND_PORT"
        ;;
    frontend)
        stop_service "前端" "$FRONTEND_PID_FILE" "$FRONTEND_PORT"
        ;;
    all)
        stop_service "后端" "$BACKEND_PID_FILE" "$BACKEND_PORT"
        stop_service "前端" "$FRONTEND_PID_FILE" "$FRONTEND_PORT"
        ;;
    *)
        echo "用法: $0 [all|backend|frontend]"
        exit 1
        ;;
esac
