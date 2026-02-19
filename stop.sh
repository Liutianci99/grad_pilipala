#!/bin/bash

echo "🛑 停止电商物流管理系统..."

# 停止后端服务
echo "⏹️  停止后端服务..."
pkill -f "spring-boot:run"
if [ $? -eq 0 ]; then
    echo "✅ 后端已停止"
else
    echo "ℹ️  后端未运行"
fi

# 停止前端服务
echo "⏹️  停止前端服务..."
pkill -f "vite"
if [ $? -eq 0 ]; then
    echo "✅ 前端已停止"
else
    echo "ℹ️  前端未运行"
fi

echo "✅ 系统已完全停止"
