#!/bin/bash
#
# 审稿宝本地开发一键启动脚本（仅本机使用）
#
# 用法:
#   ./start.sh            # 启动前端 + 后端（默认）
#   ./start.sh backend    # 只启动后端
#   ./start.sh frontend   # 只启动前端
#
# 日志与 PID 文件保存在项目根目录 .run/ 下。

set -u

# ============ 本机环境路径（仅本机使用，写死） ============
# JDK 17（Spring Boot 3.5 要求，本机默认 java 是 1.8）
JAVA17_HOME="/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home"
# Homebrew 安装的本地 Maven
MVN_CMD="/opt/homebrew/bin/mvn"
# 前端使用的 Node 版本（nvm 管理，需 >= 20.19.0）
# 允许环境变量覆盖，并去掉可能存在的 v 前缀
NODE_VERSION="${NODE_VERSION:-22.22.0}"
NODE_VERSION="${NODE_VERSION#v}"
# ==========================================================

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

BACKEND_DIR="$ROOT_DIR/backend"
FRONTEND_DIR="$ROOT_DIR/frontend"
RUN_DIR="$ROOT_DIR/.run"

BACKEND_PORT="${SERVER_PORT:-8080}"
FRONTEND_PORT=5173

BACKEND_LOG="$RUN_DIR/backend.log"
FRONTEND_LOG="$RUN_DIR/frontend.log"
BACKEND_PID_FILE="$RUN_DIR/backend.pid"
FRONTEND_PID_FILE="$RUN_DIR/frontend.pid"

mkdir -p "$RUN_DIR"

log_info()  { echo -e "${GREEN}[INFO]${NC} $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

# 判断端口是否有进程监听
port_in_use() {
    lsof -iTCP:"$1" -sTCP:LISTEN >/dev/null 2>&1
}

# 获取端口监听进程的 "PID 命令行" 信息（无监听则输出空）
listener_info() {
    local pid
    pid="$(lsof -ti tcp:"$1" -sTCP:LISTEN 2>/dev/null | head -n1)"
    if [ -n "$pid" ]; then
        echo "$pid $(ps -p "$pid" -o command= 2>/dev/null | cut -c1-120)"
    fi
}

# 等待服务就绪: 端口监听 + 进程持续存活
# 防止"应用先绑定端口、随后健康检查失败退出"导致的误判。
# 参数: 端口 进程PID 超时秒数；失败返回 1
wait_for_service() {
    local port=$1 pid=$2 timeout=$3 elapsed=0
    while [ "$elapsed" -lt "$timeout" ]; do
        if ! kill -0 "$pid" 2>/dev/null; then
            return 1
        fi
        if port_in_use "$port"; then
            sleep 3
            kill -0 "$pid" 2>/dev/null || return 1
            return 0
        fi
        sleep 2
        elapsed=$((elapsed + 2))
    done
    return 1
}

# ------------------------------------------------------------
# 后端
# ------------------------------------------------------------
start_backend() {
    if port_in_use "$BACKEND_PORT"; then
        local info
        info="$(listener_info "$BACKEND_PORT")"
        if echo "$info" | grep -qiE "spring-boot|proofly"; then
            log_warn "后端已在运行 (端口 $BACKEND_PORT)，跳过启动"
            return 0
        fi
        log_error "端口 $BACKEND_PORT 已被其他程序占用: $info"
        log_error "请先停止占用程序，或换端口启动: SERVER_PORT=8081 $SCRIPT_DIR/start.sh backend"
        return 1
    fi

    if [ ! -d "$BACKEND_DIR" ]; then
        log_error "未找到后端目录: $BACKEND_DIR"
        return 1
    fi

    if [ ! -x "$JAVA17_HOME/bin/java" ]; then
        log_error "未找到 JDK 17: $JAVA17_HOME，请检查路径"
        return 1
    fi
    export JAVA_HOME="$JAVA17_HOME"

    if [ ! -x "$MVN_CMD" ]; then
        log_error "未找到 Maven: $MVN_CMD，请检查路径"
        return 1
    fi

    # 注入本地 RSA 密钥对：application.yml 中私钥默认为空，不注入则登录时报"密码解析失败"
    local pub_pem="$BACKEND_DIR/keys/public.pem" priv_pem="$BACKEND_DIR/keys/private.pem"
    if [ ! -f "$pub_pem" ] || [ ! -f "$priv_pem" ]; then
        log_error "未找到 RSA 密钥文件: $BACKEND_DIR/keys/{public,private}.pem"
        return 1
    fi
    export PROOFLY_AUTH_RSA_PUBLIC_KEY="$(grep -v '^-----' "$pub_pem" | tr -d '\n')"
    export PROOFLY_AUTH_RSA_PRIVATE_KEY="$(grep -v '^-----' "$priv_pem" | tr -d '\n')"

    log_info "使用 JDK 17: $JAVA_HOME"
    log_info "启动后端 (端口 $BACKEND_PORT)，日志: $BACKEND_LOG"
    (
        cd "$BACKEND_DIR" || exit 1
        nohup "$MVN_CMD" spring-boot:run > "$BACKEND_LOG" 2>&1 &
        echo $! > "$BACKEND_PID_FILE"
    )

    local mvn_pid
    mvn_pid="$(cat "$BACKEND_PID_FILE")"
    log_info "等待后端就绪（首次启动需编译，可能较慢）..."
    if wait_for_service "$BACKEND_PORT" "$mvn_pid" 180; then
        log_info "后端启动成功: http://localhost:$BACKEND_PORT"
    else
        if kill -0 "$mvn_pid" 2>/dev/null; then
            log_error "后端在 180 秒内未就绪，请查看日志: tail -f $BACKEND_LOG"
        else
            log_error "后端启动失败（进程已退出），最近日志:"
            tail -n 15 "$BACKEND_LOG"
        fi
        return 1
    fi
}

# ------------------------------------------------------------
# 前端
# ------------------------------------------------------------
start_frontend() {
    if port_in_use "$FRONTEND_PORT"; then
        local info
        info="$(listener_info "$FRONTEND_PORT")"
        if echo "$info" | grep -qiE "vite|npm|proofly"; then
            log_warn "前端已在运行 (端口 $FRONTEND_PORT)，跳过启动"
            return 0
        fi
        log_error "端口 $FRONTEND_PORT 已被其他程序占用: $info"
        return 1
    fi

    if [ ! -d "$FRONTEND_DIR" ]; then
        log_error "未找到前端目录: $FRONTEND_DIR"
        return 1
    fi

    # 加载 nvm 并切换到指定 Node 版本
    if [ ! -s "$HOME/.nvm/nvm.sh" ]; then
        log_error "未找到 nvm: ~/.nvm/nvm.sh"
        return 1
    fi
    # shellcheck disable=SC1090
    . "$HOME/.nvm/nvm.sh" >/dev/null 2>&1
    if [ ! -d "$HOME/.nvm/versions/node/v${NODE_VERSION}" ]; then
        log_error "nvm 中未安装 Node v${NODE_VERSION}，可先执行: nvm install ${NODE_VERSION}"
        log_info "已安装版本: $(ls "$HOME/.nvm/versions/node" | tr '\n' ' ')"
        return 1
    fi
    nvm use "${NODE_VERSION}" >/dev/null 2>&1
    log_info "使用 Node $(node -v) / npm $(npm -v)"

    if [ ! -d "$FRONTEND_DIR/node_modules" ]; then
        log_info "未检测到 node_modules，先执行 npm install..."
        (cd "$FRONTEND_DIR" && npm install) || { log_error "npm install 失败"; return 1; }
    fi

    log_info "启动前端 (端口 $FRONTEND_PORT)，日志: $FRONTEND_LOG"
    (
        cd "$FRONTEND_DIR" || exit 1
        nohup npm run dev > "$FRONTEND_LOG" 2>&1 &
        echo $! > "$FRONTEND_PID_FILE"
    )

    local fe_pid
    fe_pid="$(cat "$FRONTEND_PID_FILE")"
    log_info "等待前端就绪..."
    if wait_for_service "$FRONTEND_PORT" "$fe_pid" 60; then
        log_info "前端启动成功: http://localhost:$FRONTEND_PORT"
    else
        if kill -0 "$fe_pid" 2>/dev/null; then
            log_error "前端在 60 秒内未就绪，请查看日志: tail -f $FRONTEND_LOG"
        else
            log_error "前端启动失败（进程已退出），最近日志:"
            tail -n 15 "$FRONTEND_LOG"
        fi
        return 1
    fi
}

# ------------------------------------------------------------
# 入口
# ------------------------------------------------------------
target="${1:-all}"
FAILED=0

case "$target" in
    backend)
        start_backend || FAILED=1
        ;;
    frontend)
        start_frontend || FAILED=1
        ;;
    all)
        start_backend || FAILED=1
        start_frontend || FAILED=1
        ;;
    *)
        echo "用法: $0 [all|backend|frontend]"
        exit 1
        ;;
esac

echo ""
if [ "$FAILED" -eq 0 ]; then
    log_info "全部完成 ✔"
    echo "  后端日志: $BACKEND_LOG"
    echo "  前端日志: $FRONTEND_LOG"
    echo "  停止服务: $SCRIPT_DIR/stop.sh"
else
    log_error "部分服务启动失败，请根据上方提示查看日志排查"
    exit 1
fi
