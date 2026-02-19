#!/bin/bash
# 使用方法: ./start.sh

echo "🚀 启动电商物流管理系统..."

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 1. 启动后端（后台运行）
echo "⚙️  启动后端服务..."
cd "$SCRIPT_DIR/backend" || exit 1
mvn spring-boot:run > ../logs/backend.log 2>&1 &
BACKEND_PID=$!
echo "✅ 后端已启动 (PID: $BACKEND_PID, 日志: logs/backend.log)"

# 等待后端启动
sleep 5

# 2. 启动前端（前台运行）
echo "🎨 启动前端服务..."
cd "$SCRIPT_DIR/frontend" || exit 1
pnpm install
pnpm dev

# 如果前端退出，清理后端进程
kill $BACKEND_PID 2>/dev/null
echo "🛑 系统已停止"