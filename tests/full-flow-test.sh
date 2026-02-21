#!/bin/bash
# ============================================================
# 全流程测试脚本
# 商家入库 → 上架 → 顾客下单 → 商家发货 → 配送员揽收
# → 创建批次 → 开始运输 → 顾客收货
# ============================================================

BASE_URL="http://localhost:8080/api"
PASS=0
FAIL=0
TOTAL=0

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

# MySQL helper (always utf8mb4)
run_sql() {
    docker exec grad-logistics-db mysql -uroot -p'GradProject2026!Secure' ecommerce_logistics --default-character-set=utf8mb4 -N -e "$1" 2>/dev/null
}
run_sql_exec() {
    docker exec grad-logistics-db mysql -uroot -p'GradProject2026!Secure' ecommerce_logistics --default-character-set=utf8mb4 -e "$1" 2>/dev/null
}

# ── Helpers ──

log_step() {
    echo ""
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${CYAN}  STEP: $1${NC}"
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

assert_ok() {
    local desc="$1"
    local response="$2"
    TOTAL=$((TOTAL + 1))
    local code=$(echo "$response" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('code',''))" 2>/dev/null)
    if [ "$code" = "200" ]; then
        echo -e "  ${GREEN}✅ PASS${NC}: $desc"
        PASS=$((PASS + 1))
    else
        echo -e "  ${RED}❌ FAIL${NC}: $desc"
        echo -e "  ${RED}   code=$code | $(echo "$response" | head -c 300)${NC}"
        FAIL=$((FAIL + 1))
    fi
}

check_status() {
    local desc="$1"
    local actual="$2"
    local expected="$3"
    TOTAL=$((TOTAL + 1))
    actual=$(echo "$actual" | tr -d '[:space:]')
    if [ "$actual" = "$expected" ]; then
        echo -e "  ${GREEN}✅ PASS${NC}: $desc (=$expected)"
        PASS=$((PASS + 1))
    else
        echo -e "  ${RED}❌ FAIL${NC}: $desc — expected $expected, got '$actual'"
        FAIL=$((FAIL + 1))
    fi
}

login() {
    local username="$1"
    local role="$2"
    local resp=$(curl -s -X POST "$BASE_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$username\",\"password\":\"123\",\"role\":\"$role\"}")
    echo "$resp" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('token',''))" 2>/dev/null
}

# ── Check backend is alive ──
echo -e "${YELLOW}🔍 Checking backend health...${NC}"
HEALTH=$(curl -s "$BASE_URL/auth/test")
if [ "$HEALTH" != "Backend is running!" ]; then
    echo -e "${RED}❌ Backend is not running at $BASE_URL${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Backend is alive${NC}"

# ============================================================
# STEP 0: Login all roles
# ============================================================
log_step "0. 登录所有角色"

MERCHANT_TOKEN=$(login "京东自营" "merchant")
CONSUMER_TOKEN=$(login "刘天赐" "consumer")
DRIVER_TOKEN=$(login "张伟" "driver")

echo "  Merchant: ${MERCHANT_TOKEN:0:20}..."
echo "  Consumer: ${CONSUMER_TOKEN:0:20}..."
echo "  Driver:   ${DRIVER_TOKEN:0:20}..."

if [ -z "$MERCHANT_TOKEN" ] || [ -z "$CONSUMER_TOKEN" ] || [ -z "$DRIVER_TOKEN" ]; then
    echo -e "${RED}❌ Login failed${NC}"
    exit 1
fi
echo -e "${GREEN}✅ All logins OK${NC}"

# ============================================================
# STEP 1: 商家入库
# ============================================================
log_step "1. 商家入库 — 京东自营 入库 '测试商品-全流程' 到华北一号仓"

run_sql_exec "INSERT INTO inventory (user_id, product_name, description, quantity, stock_in_date, image_url, is_published, warehouse_id) VALUES (2, '测试商品-全流程', '全流程测试用商品', 100, NOW(), 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/ps5-controller.jpg', 0, 1);"

PRODUCT_ID=$(run_sql "SELECT product_id FROM inventory WHERE product_name='测试商品-全流程' ORDER BY product_id DESC LIMIT 1;")
PRODUCT_ID=$(echo "$PRODUCT_ID" | tr -d '[:space:]')

check_status "商品入库成功 (product_id=$PRODUCT_ID)" "$PRODUCT_ID" "$PRODUCT_ID"
if [ -z "$PRODUCT_ID" ]; then
    echo -e "${RED}❌ No product ID — aborting${NC}"
    exit 1
fi

# ============================================================
# STEP 2: 商品上架
# ============================================================
log_step "2. 商品上架 — 售价99.9元，数量50"

PUBLISH_RESP=$(curl -s -X POST "$BASE_URL/mall/publish" \
    -H "Authorization: Bearer $MERCHANT_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"productId\":$PRODUCT_ID,\"description\":\"全流程测试商品\",\"quantity\":50,\"price\":99.90}")
assert_ok "商品上架成功" "$PUBLISH_RESP"

# Verify in mall
MALL_RESP=$(curl -s "$BASE_URL/mall/products" -H "Authorization: Bearer $CONSUMER_TOKEN")
FOUND=$(echo "$MALL_RESP" | python3 -c "
import sys, json
d = json.load(sys.stdin)
found = any(p.get('productName') == '测试商品-全流程' for p in d.get('data', []))
print('yes' if found else 'no')
" 2>/dev/null)
check_status "商品在商城可见" "$FOUND" "yes"

# ============================================================
# STEP 3: 顾客下单
# ============================================================
log_step "3. 顾客下单 — 刘天赐 购买1件，收货地址: 三里屯"

ORDER_RESP=$(curl -s -X POST "$BASE_URL/orders/create" \
    -H "Authorization: Bearer $CONSUMER_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"customerId\":9,\"productId\":$PRODUCT_ID,\"quantity\":1,\"price\":99.90,\"addressId\":1}")
assert_ok "下单成功" "$ORDER_RESP"

ORDER_ID=$(echo "$ORDER_RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('orderId',''))" 2>/dev/null)
echo "  Order ID: $ORDER_ID"
if [ -z "$ORDER_ID" ] || [ "$ORDER_ID" = "" ]; then
    echo -e "${RED}❌ No order ID — aborting${NC}"
    echo "  Response: $ORDER_RESP"
    exit 1
fi

ORDER_STATUS=$(run_sql "SELECT status FROM orders WHERE order_id=$ORDER_ID;")
check_status "订单状态 = 0 (未发货)" "$ORDER_STATUS" "0"

# ============================================================
# STEP 4: 商家发货
# ============================================================
log_step "4. 商家发货"

SHIP_RESP=$(curl -s -X PUT "$BASE_URL/orders/$ORDER_ID/ship?merchantId=2" \
    -H "Authorization: Bearer $MERCHANT_TOKEN")
assert_ok "发货成功" "$SHIP_RESP"

ORDER_STATUS=$(run_sql "SELECT status FROM orders WHERE order_id=$ORDER_ID;")
check_status "订单状态 = 1 (已发货)" "$ORDER_STATUS" "1"

# ============================================================
# STEP 5: 配送员揽收
# ============================================================
log_step "5. 配送员揽收 — 张伟"

PICKUP_RESP=$(curl -s -X PUT "$BASE_URL/orders/$ORDER_ID/pickup" \
    -H "Authorization: Bearer $DRIVER_TOKEN")
assert_ok "揽收成功" "$PICKUP_RESP"

ORDER_STATUS=$(run_sql "SELECT status FROM orders WHERE order_id=$ORDER_ID;")
check_status "订单状态 = 2 (已揽收)" "$ORDER_STATUS" "2"

# ============================================================
# STEP 6: 创建运输批次
# ============================================================
log_step "6. 创建运输批次 — 张伟 (user_id=5)"

BATCH_RESP=$(curl -s -X POST "$BASE_URL/orders/delivery-batch?deliveryPersonnelId=5" \
    -H "Authorization: Bearer $DRIVER_TOKEN" \
    -H "Content-Type: application/json" \
    -d "[$ORDER_ID]")
assert_ok "创建批次成功" "$BATCH_RESP"

BATCH_ID=$(echo "$BATCH_RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('batchId',''))" 2>/dev/null)
echo "  Batch ID: $BATCH_ID"
if [ -z "$BATCH_ID" ] || [ "$BATCH_ID" = "" ]; then
    echo -e "${RED}❌ No batch ID — aborting${NC}"
    echo "  Response: $BATCH_RESP"
    exit 1
fi

BATCH_STATUS=$(run_sql "SELECT status FROM delivery_batches WHERE id=$BATCH_ID;")
check_status "批次状态 = 0 (待出发)" "$BATCH_STATUS" "0"

ORDER_STATUS=$(run_sql "SELECT status FROM orders WHERE order_id=$ORDER_ID;")
check_status "订单状态仍为 2 (已揽收，批次未开始)" "$ORDER_STATUS" "2"

# ============================================================
# STEP 7: 开始运输 (triggers Tencent Maps API route planning)
# ============================================================
log_step "7. 开始运输 — 调用腾讯地图API规划路线"

START_RESP=$(curl -s -X POST "$BASE_URL/delivery-batch/start-batch?batchId=$BATCH_ID" \
    -H "Authorization: Bearer $DRIVER_TOKEN")
assert_ok "开始运输成功" "$START_RESP"

sleep 2

BATCH_STATUS=$(run_sql "SELECT status FROM delivery_batches WHERE id=$BATCH_ID;")
check_status "批次状态 = 1 (配送中)" "$BATCH_STATUS" "1"

ORDER_STATUS=$(run_sql "SELECT status FROM orders WHERE order_id=$ORDER_ID;")
check_status "订单状态 = 3 (运输中)" "$ORDER_STATUS" "3"

# Check route data is real polyline (not fake waypoint format)
ROUTE_FORMAT=$(run_sql "
SELECT CASE
    WHEN route_data LIKE '[{%' THEN 'FAKE'
    WHEN route_data LIKE '[%' AND LENGTH(route_data) > 100 THEN 'REAL'
    ELSE 'UNKNOWN'
END FROM delivery_route WHERE batch_id=$BATCH_ID;")
ROUTE_FORMAT=$(echo "$ROUTE_FORMAT" | tr -d '[:space:]')
ROUTE_LEN=$(run_sql "SELECT LENGTH(route_data) FROM delivery_route WHERE batch_id=$BATCH_ID;")
echo "  Route format: $ROUTE_FORMAT, data length: ${ROUTE_LEN} bytes"
check_status "路线数据为真实腾讯地图API polyline" "$ROUTE_FORMAT" "REAL"

# Test route-by-batch API
ROUTE_RESP=$(curl -s "$BASE_URL/delivery-batch/route-by-batch?batchId=$BATCH_ID" \
    -H "Authorization: Bearer $DRIVER_TOKEN")
assert_ok "路线查询成功 (route-by-batch)" "$ROUTE_RESP"

# Test location-by-batch API
LOC_RESP=$(curl -s "$BASE_URL/delivery-batch/location-by-batch?batchId=$BATCH_ID" \
    -H "Authorization: Bearer $DRIVER_TOKEN")
assert_ok "位置查询成功 (location-by-batch)" "$LOC_RESP"

# ============================================================
# STEP 8: 完成配送
# ============================================================
log_step "8. 完成配送 — 确认送达"

COMPLETE_RESP=$(curl -s -X POST "$BASE_URL/delivery-batch/complete-batch?batchId=$BATCH_ID" \
    -H "Authorization: Bearer $DRIVER_TOKEN")
assert_ok "完成配送成功" "$COMPLETE_RESP"

BATCH_STATUS=$(run_sql "SELECT status FROM delivery_batches WHERE id=$BATCH_ID;")
check_status "批次状态 = 2 (已完成)" "$BATCH_STATUS" "2"

ORDER_STATUS=$(run_sql "SELECT status FROM orders WHERE order_id=$ORDER_ID;")
check_status "订单状态 = 4 (已到达)" "$ORDER_STATUS" "4"

# ============================================================
# STEP 9: 顾客确认收货
# ============================================================
log_step "9. 顾客确认收货 — 刘天赐"

CONFIRM_RESP=$(curl -s -X PUT "$BASE_URL/orders/$ORDER_ID/confirm?customerId=9" \
    -H "Authorization: Bearer $CONSUMER_TOKEN")
assert_ok "确认收货成功" "$CONFIRM_RESP"

ORDER_STATUS=$(run_sql "SELECT status FROM orders WHERE order_id=$ORDER_ID;")
check_status "订单最终状态 = 5 (已收货)" "$ORDER_STATUS" "5"

# ============================================================
# STEP 10: 物流追踪验证
# ============================================================
log_step "10. 物流追踪 — track-by-order API"

TRACK_RESP=$(curl -s "$BASE_URL/delivery-batch/track-by-order?orderId=$ORDER_ID" \
    -H "Authorization: Bearer $CONSUMER_TOKEN")
assert_ok "物流追踪查询成功" "$TRACK_RESP"

# ============================================================
# CLEANUP
# ============================================================
log_step "CLEANUP: 清理测试数据"

run_sql_exec "
DELETE FROM delivery_location WHERE route_id IN (SELECT id FROM delivery_route WHERE batch_id=$BATCH_ID);
DELETE FROM delivery_route WHERE batch_id=$BATCH_ID;
DELETE FROM delivery_batch_orders WHERE batch_id=$BATCH_ID;
DELETE FROM delivery_batches WHERE id=$BATCH_ID;
DELETE FROM orders WHERE order_id=$ORDER_ID;
DELETE FROM mall WHERE product_id=$PRODUCT_ID;
DELETE FROM inventory WHERE product_id=$PRODUCT_ID;"

echo -e "  ${GREEN}✅ 测试数据已清理${NC}"

# ============================================================
# SUMMARY
# ============================================================
echo ""
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${CYAN}  RESULTS${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "  Total:  $TOTAL"
echo -e "  ${GREEN}Passed: $PASS${NC}"
echo -e "  ${RED}Failed: $FAIL${NC}"
echo ""

if [ $FAIL -eq 0 ]; then
    echo -e "${GREEN}🎉 ALL TESTS PASSED! 全流程数据流转正常！${NC}"
    exit 0
else
    echo -e "${RED}⚠️  $FAIL test(s) failed. Check output above.${NC}"
    exit 1
fi
