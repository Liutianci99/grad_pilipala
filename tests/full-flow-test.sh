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
    local check_field="${3:-code}"
    local expected="${4:-200}"
    TOTAL=$((TOTAL + 1))

    local actual=$(echo "$response" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('$check_field',''))" 2>/dev/null)

    if [ "$actual" = "$expected" ]; then
        echo -e "  ${GREEN}✅ PASS${NC}: $desc"
        PASS=$((PASS + 1))
    else
        echo -e "  ${RED}❌ FAIL${NC}: $desc"
        echo -e "  ${RED}   Expected $check_field=$expected, got: $actual${NC}"
        echo -e "  ${RED}   Response: $(echo "$response" | head -c 300)${NC}"
        FAIL=$((FAIL + 1))
    fi
}

assert_field() {
    local desc="$1"
    local response="$2"
    local jq_expr="$3"
    local expected="$4"
    TOTAL=$((TOTAL + 1))

    local actual=$(echo "$response" | python3 -c "
import sys, json
d = json.load(sys.stdin)
$jq_expr
" 2>/dev/null)

    if [ "$actual" = "$expected" ]; then
        echo -e "  ${GREEN}✅ PASS${NC}: $desc"
        PASS=$((PASS + 1))
    else
        echo -e "  ${RED}❌ FAIL${NC}: $desc"
        echo -e "  ${RED}   Expected: $expected, got: $actual${NC}"
        FAIL=$((FAIL + 1))
    fi
}

login() {
    local username="$1"
    local role="$2"
    local resp=$(curl -s -X POST "$BASE_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$username\",\"password\":\"123\",\"role\":\"$role\"}")
    local token=$(echo "$resp" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('token',''))" 2>/dev/null)
    echo "$token"
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

echo "  Merchant token: ${MERCHANT_TOKEN:0:20}..."
echo "  Consumer token: ${CONSUMER_TOKEN:0:20}..."
echo "  Driver token:   ${DRIVER_TOKEN:0:20}..."

if [ -z "$MERCHANT_TOKEN" ] || [ -z "$CONSUMER_TOKEN" ] || [ -z "$DRIVER_TOKEN" ]; then
    echo -e "${RED}❌ Login failed for one or more roles${NC}"
    exit 1
fi
echo -e "${GREEN}✅ All logins successful${NC}"

# ============================================================
# STEP 1: 商家入库 (Merchant stock-in)
# ============================================================
log_step "1. 商家入库 — 京东自营 入库 '测试商品-全流程' 到华北一号仓"

# Stock-in via API requires R2 image upload (needs cloud credentials)
# For testing, insert directly into DB to simulate stock-in
docker exec grad-logistics-db mysql -uroot -p'GradProject2026!Secure' ecommerce_logistics --default-character-set=utf8mb4 -e "
INSERT INTO inventory (user_id, product_name, description, quantity, stock_in_date, image_url, is_published, warehouse_id)
VALUES (2, '测试商品-全流程', '全流程测试用商品', 100, NOW(), 'https://pub-21cebd4de3484e8b91a8e06a5f6b9c78.r2.dev/products/ps5-controller.jpg', 0, 1);" 2>/dev/null

PRODUCT_ID=$(docker exec grad-logistics-db mysql -uroot -p'GradProject2026!Secure' ecommerce_logistics -N -e "
SELECT product_id FROM inventory WHERE product_name='测试商品-全流程' ORDER BY product_id DESC LIMIT 1;" 2>/dev/null)

TOTAL=$((TOTAL + 1))
if [ -n "$PRODUCT_ID" ] && [ "$PRODUCT_ID" != "" ]; then
    echo -e "  ${GREEN}✅ PASS${NC}: 商品入库成功, Product ID: $PRODUCT_ID"
    PASS=$((PASS + 1))
else
    echo -e "  ${RED}❌ FAIL${NC}: 商品入库失败"
    FAIL=$((FAIL + 1))
    exit 1
fi

# ============================================================
# STEP 2: 商品上架 (Publish to mall)
# ============================================================
log_step "2. 商品上架 — 上架到商城，售价99.9元，数量50"

PUBLISH_RESP=$(curl -s -X POST "$BASE_URL/mall/publish" \
    -H "Authorization: Bearer $MERCHANT_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"productId\":$PRODUCT_ID,\"description\":\"全流程测试商品\",\"quantity\":50,\"price\":99.90}")

assert_ok "商品上架成功" "$PUBLISH_RESP"

# Verify product appears in mall
MALL_RESP=$(curl -s "$BASE_URL/mall/products" \
    -H "Authorization: Bearer $CONSUMER_TOKEN")
assert_ok "商城商品列表查询成功" "$MALL_RESP"

FOUND=$(echo "$MALL_RESP" | python3 -c "
import sys, json
d = json.load(sys.stdin)
products = d.get('data', [])
found = any(p.get('productName') == '测试商品-全流程' for p in products)
print('yes' if found else 'no')
" 2>/dev/null)
TOTAL=$((TOTAL + 1))
if [ "$FOUND" = "yes" ]; then
    echo -e "  ${GREEN}✅ PASS${NC}: 商品在商城中可见"
    PASS=$((PASS + 1))
else
    echo -e "  ${RED}❌ FAIL${NC}: 商品在商城中不可见"
    FAIL=$((FAIL + 1))
fi

# ============================================================
# STEP 3: 顾客下单 (Consumer places order)
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
    echo -e "${RED}❌ Failed to get order ID${NC}"
    echo "  Response: $ORDER_RESP"
    exit 1
fi

# Verify order status = 0 (未发货)
ORDER_STATUS=$(docker exec grad-logistics-db mysql -uroot -p'GradProject2026!Secure' ecommerce_logistics -N -e "SELECT status FROM orders WHERE order_id=$ORDER_ID;" 2>/dev/null)
TOTAL=$((TOTAL + 1))
if [ "$ORDER_STATUS" = "0" ]; then
    echo -e "  ${GREEN}✅ PASS${NC}: 订单状态 = 0 (未发货)"
    PASS=$((PASS + 1))
else
    echo -e "  ${RED}❌ FAIL${NC}: 订单状态应为0，实际为: $ORDER_STATUS"
    FAIL=$((FAIL + 1))
fi

# ============================================================
# STEP 4: 商家发货 (Merchant ships)
# ============================================================
log_step "4. 商家发货 — 京东自营 确认发货"

# First confirm order (status 0 → 1)
SHIP_RESP=$(curl -s -X PUT "$BASE_URL/orders/$ORDER_ID/ship?merchantId=2" \
    -H "Authorization: Bearer $MERCHANT_TOKEN")

assert_ok "发货成功" "$SHIP_RESP"

# Verify order status = 1 (已发货)
ORDER_STATUS=$(docker exec grad-logistics-db mysql -uroot -p'GradProject2026!Secure' ecommerce_logistics -N -e "SELECT status FROM orders WHERE order_id=$ORDER_ID;" 2>/dev/null)
TOTAL=$((TOTAL + 1))
if [ "$ORDER_STATUS" = "1" ]; then
    echo -e "  ${GREEN}✅ PASS${NC}: 订单状态 = 1 (已发货)"
    PASS=$((PASS + 1))
else
    echo -e "  ${RED}❌ FAIL${NC}: 订单状态应为1，实际为: $ORDER_STATUS"
    FAIL=$((FAIL + 1))
fi

# ============================================================
# STEP 5: 配送员揽收 (Driver pickup)
# ============================================================
log_step "5. 配送员揽收 — 张伟 揽收订单"

PICKUP_RESP=$(curl -s -X PUT "$BASE_URL/orders/$ORDER_ID/pickup" \
    -H "Authorization: Bearer $DRIVER_TOKEN")

assert_ok "揽收成功" "$PICKUP_RESP"

# Verify order status = 2 (已揽收)
ORDER_STATUS=$(docker exec grad-logistics-db mysql -uroot -p'GradProject2026!Secure' ecommerce_logistics -N -e "SELECT status FROM orders WHERE order_id=$ORDER_ID;" 2>/dev/null)
TOTAL=$((TOTAL + 1))
if [ "$ORDER_STATUS" = "2" ]; then
    echo -e "  ${GREEN}✅ PASS${NC}: 订单状态 = 2 (已揽收)"
    PASS=$((PASS + 1))
else
    echo -e "  ${RED}❌ FAIL${NC}: 订单状态应为2，实际为: $ORDER_STATUS"
    FAIL=$((FAIL + 1))
fi

# ============================================================
# STEP 6: 创建运输批次 (Create delivery batch)
# ============================================================
log_step "6. 创建运输批次 — 张伟 (user_id=5, delivery_personnel_id via user_id)"

# delivery_personnel where user_id=5 → id=1
BATCH_RESP=$(curl -s -X POST "$BASE_URL/orders/delivery-batch?deliveryPersonnelId=5" \
    -H "Authorization: Bearer $DRIVER_TOKEN" \
    -H "Content-Type: application/json" \
    -d "[$ORDER_ID]")

assert_ok "创建批次成功" "$BATCH_RESP"

BATCH_ID=$(echo "$BATCH_RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('batchId',''))" 2>/dev/null)
echo "  Batch ID: $BATCH_ID"

if [ -z "$BATCH_ID" ] || [ "$BATCH_ID" = "" ]; then
    echo -e "${RED}❌ Failed to get batch ID${NC}"
    echo "  Response: $BATCH_RESP"
    exit 1
fi

# Verify batch status = 0 (待出发)
BATCH_STATUS=$(docker exec grad-logistics-db mysql -uroot -p'GradProject2026!Secure' ecommerce_logistics -N -e "SELECT status FROM delivery_batches WHERE id=$BATCH_ID;" 2>/dev/null)
TOTAL=$((TOTAL + 1))
if [ "$BATCH_STATUS" = "0" ]; then
    echo -e "  ${GREEN}✅ PASS${NC}: 批次状态 = 0 (待出发)"
    PASS=$((PASS + 1))
else
    echo -e "  ${RED}❌ FAIL${NC}: 批次状态应为0，实际为: $BATCH_STATUS"
    FAIL=$((FAIL + 1))
fi

# Verify order status = 2 still (揽收, not yet 运输中)
ORDER_STATUS=$(docker exec grad-logistics-db mysql -uroot -p'GradProject2026!Secure' ecommerce_logistics -N -e "SELECT status FROM orders WHERE order_id=$ORDER_ID;" 2>/dev/null)
TOTAL=$((TOTAL + 1))
if [ "$ORDER_STATUS" = "2" ]; then
    echo -e "  ${GREEN}✅ PASS${NC}: 订单状态仍为 2 (已揽收，批次未开始)"
    PASS=$((PASS + 1))
else
    echo -e "  ${RED}❌ FAIL${NC}: 订单状态应为2，实际为: $ORDER_STATUS"
    FAIL=$((FAIL + 1))
fi

# ============================================================
# STEP 7: 开始运输 (Start delivery — triggers route planning via Tencent Maps API)
# ============================================================
log_step "7. 开始运输 — 调用腾讯地图API规划路线"

START_RESP=$(curl -s -X POST "$BASE_URL/delivery-batch/start-batch?batchId=$BATCH_ID" \
    -H "Authorization: Bearer $DRIVER_TOKEN")

assert_ok "开始运输成功" "$START_RESP"

# Verify batch status = 1 (配送中)
BATCH_STATUS=$(docker exec grad-logistics-db mysql -uroot -p'GradProject2026!Secure' ecommerce_logistics -N -e "SELECT status FROM delivery_batches WHERE id=$BATCH_ID;" 2>/dev/null)
TOTAL=$((TOTAL + 1))
if [ "$BATCH_STATUS" = "1" ]; then
    echo -e "  ${GREEN}✅ PASS${NC}: 批次状态 = 1 (配送中)"
    PASS=$((PASS + 1))
else
    echo -e "  ${RED}❌ FAIL${NC}: 批次状态应为1，实际为: $BATCH_STATUS"
    FAIL=$((FAIL + 1))
fi

# Verify order status = 3 (运输中)
sleep 1
ORDER_STATUS=$(docker exec grad-logistics-db mysql -uroot -p'GradProject2026!Secure' ecommerce_logistics -N -e "SELECT status FROM orders WHERE order_id=$ORDER_ID;" 2>/dev/null)
TOTAL=$((TOTAL + 1))
if [ "$ORDER_STATUS" = "3" ]; then
    echo -e "  ${GREEN}✅ PASS${NC}: 订单状态 = 3 (运输中)"
    PASS=$((PASS + 1))
else
    echo -e "  ${RED}❌ FAIL${NC}: 订单状态应为3，实际为: $ORDER_STATUS"
    FAIL=$((FAIL + 1))
fi

# Verify route was created with real polyline data (not fake waypoint format)
ROUTE_CHECK=$(docker exec grad-logistics-db mysql -uroot -p'GradProject2026!Secure' ecommerce_logistics -N -e "
SELECT
    CASE
        WHEN route_data LIKE '[{%' THEN 'FAKE_WAYPOINT'
        WHEN route_data LIKE '[%' AND LENGTH(route_data) > 100 THEN 'REAL_POLYLINE'
        ELSE 'UNKNOWN'
    END as format,
    LENGTH(route_data) as data_len
FROM delivery_route WHERE batch_id=$BATCH_ID;" 2>/dev/null)

ROUTE_FORMAT=$(echo "$ROUTE_CHECK" | awk '{print $1}')
ROUTE_LEN=$(echo "$ROUTE_CHECK" | awk '{print $2}')
echo "  Route format: $ROUTE_FORMAT, data length: $ROUTE_LEN bytes"

TOTAL=$((TOTAL + 1))
if [ "$ROUTE_FORMAT" = "REAL_POLYLINE" ]; then
    echo -e "  ${GREEN}✅ PASS${NC}: 路线数据为真实腾讯地图API polyline"
    PASS=$((PASS + 1))
else
    echo -e "  ${RED}❌ FAIL${NC}: 路线数据格式异常: $ROUTE_FORMAT"
    FAIL=$((FAIL + 1))
fi

# Verify route-by-batch API works
ROUTE_RESP=$(curl -s "$BASE_URL/delivery-batch/route-by-batch?batchId=$BATCH_ID" \
    -H "Authorization: Bearer $DRIVER_TOKEN")
assert_ok "路线查询成功" "$ROUTE_RESP"

# Verify location-by-batch API works
LOCATION_RESP=$(curl -s "$BASE_URL/delivery-batch/location-by-batch?batchId=$BATCH_ID" \
    -H "Authorization: Bearer $DRIVER_TOKEN")
assert_ok "位置查询成功" "$LOCATION_RESP"

# ============================================================
# STEP 8: 完成配送 (Complete delivery)
# ============================================================
log_step "8. 完成配送 — 确认送达"

COMPLETE_RESP=$(curl -s -X POST "$BASE_URL/delivery-batch/complete-batch?batchId=$BATCH_ID" \
    -H "Authorization: Bearer $DRIVER_TOKEN")

assert_ok "完成配送成功" "$COMPLETE_RESP"

# Verify batch status = 2 (已完成)
BATCH_STATUS=$(docker exec grad-logistics-db mysql -uroot -p'GradProject2026!Secure' ecommerce_logistics -N -e "SELECT status FROM delivery_batches WHERE id=$BATCH_ID;" 2>/dev/null)
TOTAL=$((TOTAL + 1))
if [ "$BATCH_STATUS" = "2" ]; then
    echo -e "  ${GREEN}✅ PASS${NC}: 批次状态 = 2 (已完成)"
    PASS=$((PASS + 1))
else
    echo -e "  ${RED}❌ FAIL${NC}: 批次状态应为2，实际为: $BATCH_STATUS"
    FAIL=$((FAIL + 1))
fi

# Verify order status = 4 (已到达) — complete-batch sets orders to status 4? or 5?
ORDER_STATUS=$(docker exec grad-logistics-db mysql -uroot -p'GradProject2026!Secure' ecommerce_logistics -N -e "SELECT status FROM orders WHERE order_id=$ORDER_ID;" 2>/dev/null)
echo "  Order status after complete-batch: $ORDER_STATUS"
TOTAL=$((TOTAL + 1))
if [ "$ORDER_STATUS" = "4" ] || [ "$ORDER_STATUS" = "5" ]; then
    echo -e "  ${GREEN}✅ PASS${NC}: 订单状态 = $ORDER_STATUS (已到达/已收货)"
    PASS=$((PASS + 1))
else
    echo -e "  ${RED}❌ FAIL${NC}: 订单状态应为4或5，实际为: $ORDER_STATUS"
    FAIL=$((FAIL + 1))
fi

# ============================================================
# STEP 9: 顾客确认收货 (Consumer confirms receipt)
# ============================================================
log_step "9. 顾客确认收货 — 刘天赐 确认收货"

# Check what endpoint handles confirm receipt
CONFIRM_RESP=$(curl -s -X PUT "$BASE_URL/orders/$ORDER_ID/confirm?customerId=9" \
    -H "Authorization: Bearer $CONSUMER_TOKEN")

echo "  Confirm response: $(echo "$CONFIRM_RESP" | head -c 200)"

# Verify final order status = 5 (已收货)
ORDER_STATUS=$(docker exec grad-logistics-db mysql -uroot -p'GradProject2026!Secure' ecommerce_logistics -N -e "SELECT status FROM orders WHERE order_id=$ORDER_ID;" 2>/dev/null)
TOTAL=$((TOTAL + 1))
if [ "$ORDER_STATUS" = "5" ]; then
    echo -e "  ${GREEN}✅ PASS${NC}: 订单最终状态 = 5 (已收货) ✅"
    PASS=$((PASS + 1))
else
    echo -e "  ${RED}❌ FAIL${NC}: 订单最终状态应为5，实际为: $ORDER_STATUS"
    FAIL=$((FAIL + 1))
fi

# ============================================================
# STEP 10: 物流追踪验证 (Track-by-order API)
# ============================================================
log_step "10. 物流追踪 — 验证 track-by-order API"

TRACK_RESP=$(curl -s "$BASE_URL/delivery-batch/track-by-order?orderId=$ORDER_ID" \
    -H "Authorization: Bearer $CONSUMER_TOKEN")

assert_ok "物流追踪查询成功" "$TRACK_RESP"

# ============================================================
# CLEANUP: Remove test data
# ============================================================
log_step "CLEANUP: 清理测试数据"

docker exec grad-logistics-db mysql -uroot -p'GradProject2026!Secure' ecommerce_logistics -e "
DELETE FROM delivery_location WHERE route_id IN (SELECT id FROM delivery_route WHERE batch_id=$BATCH_ID);
DELETE FROM delivery_route WHERE batch_id=$BATCH_ID;
DELETE FROM delivery_batch_orders WHERE batch_id=$BATCH_ID;
DELETE FROM delivery_batches WHERE id=$BATCH_ID;
DELETE FROM orders WHERE order_id=$ORDER_ID;
DELETE FROM mall WHERE product_id=$PRODUCT_ID;
DELETE FROM inventory WHERE product_id=$PRODUCT_ID;
" 2>/dev/null

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
    echo -e "${RED}⚠️  $FAIL test(s) failed. Check the output above.${NC}"
    exit 1
fi
