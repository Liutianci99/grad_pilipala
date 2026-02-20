<template>
    <div class="detail-container">
        <div class="page-header">
            <h2>运输批次详情</h2>
            <div class="batch-info">
                <span>批次时间：{{ formatTime(batchInfo.deliveryTime) }}</span>
                <span>订单数量：{{ batchInfo.orderCount }}</span>
                <span class="status-badge" :class="batchStatusClass">{{ batchStatusText }}</span>
            </div>
        </div>

        <!-- 地图容器 -->
        <div class="map-section">
            <div id="map-container" class="map-container"></div>
            <div v-if="routeInfo" class="route-info">
                <div class="route-info-row">
                    <div class="info-item">
                        <span class="label">总距离：</span>
                        <span class="value">{{ routeInfo.distance }}</span>
                    </div>
                    <div class="info-item">
                        <span class="label">预计时间：</span>
                        <span class="value">{{ routeInfo.duration }}</span>
                    </div>
                </div>
                <div v-if="currentLocation.address && currentLocation.address !== '准备中...'" class="route-info-row">
                    <div class="info-item">
                        <span class="label">当前位置：</span>
                        <span class="value">{{ currentLocation.address }}</span>
                    </div>
                    <div v-if="currentLocation.remainingTime" class="info-item">
                        <span class="label">剩余时间：</span>
                        <span class="value">{{ formatRemainingTime(currentLocation.remainingTime) }}</span>
                    </div>
                </div>
            </div>

            <!-- 操作按钮 -->
            <div class="map-actions" v-if="batchStatus === 1">
                <button class="btn btn-primary" @click="handleCompleteBatch" :disabled="completing">
                    {{ completing ? '处理中...' : '确认送达' }}
                </button>
            </div>
        </div>

        <!-- 配送点列表 -->
        <div class="delivery-points">
            <h3>配送路线</h3>
            <div class="point-list">
                <div class="point-item start-point">
                    <div class="point-marker">起</div>
                    <div class="point-content">
                        <div class="point-title">{{ warehouseInfo.name }}</div>
                        <div class="point-address">{{ warehouseInfo.address }}</div>
                    </div>
                </div>

                <div v-for="(order, index) in orders" :key="order.orderId" class="point-item">
                    <div class="point-marker">{{ index + 1 }}</div>
                    <div class="point-content">
                        <div class="point-title">
                            订单号：{{ order.orderId }} - {{ order.customerName }}
                        </div>
                        <div class="point-address">
                            {{ order.address?.province }} {{ order.address?.city }}
                            {{ order.address?.district }} {{ order.address?.detailAddress }}
                        </div>
                        <div class="point-product">{{ order.productName }} × {{ order.quantity }}</div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
/* global TMap */
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()

// 批次ID（核心参数）
const batchId = ref(null)
const batchStatus = ref(0)
const completing = ref(false)

const batchInfo = ref({ deliveryTime: '', orderCount: 0 })
const warehouseInfo = ref({ id: null, name: '', address: '', longitude: null, latitude: null })
const orders = ref([])
const routeInfo = ref(null)
const currentLocation = ref({ address: '准备中...', lat: 0, lng: 0, remainingTime: 0 })

let map = null
let startMarker = null
let deliveryMarkers = []
let routePolyline = null
let deliveryTruckMarker = null
let pollingTimer = null

const batchStatusText = computed(() => {
    const m = { 0: '待出发', 1: '配送中', 2: '已完成' }
    return m[batchStatus.value] || '未知'
})
const batchStatusClass = computed(() => {
    const m = { 0: 'status-pending', 1: 'status-delivering', 2: 'status-completed' }
    return m[batchStatus.value] || ''
})

// ── Helpers ──
const getCurrentUserWarehouseId = () => {
    const u = JSON.parse(sessionStorage.getItem('userInfo') || '{}')
    return (u.role === 'driver' && u.warehouseId) ? u.warehouseId : null
}

const fetchWarehouseInfo = async (id) => {
    try {
        const res = await request.get(`/warehouse/${id}`)
        if (res.success && res.data) { warehouseInfo.value = res.data; return res.data }
    } catch (e) { console.error('获取仓库信息失败:', e) }
    return null
}

// ── Fetch batch orders ──
const fetchBatchOrders = async () => {
    try {
        const stateData = window.history.state?.batchData
        if (stateData?.orders?.length > 0) {
            orders.value = stateData.orders
            batchStatus.value = stateData.status ?? 1
            batchInfo.value = {
                deliveryTime: stateData.startedAt || stateData.createdAt || '未知',
                orderCount: stateData.orders.length
            }
            return stateData.orders
        }

        // Fallback: fetch from API
        const uid = JSON.parse(sessionStorage.getItem('userInfo') || '{}').id
        if (!uid) { router.push('/'); return [] }

        const res = await request.get(`/orders/delivery-batches-with-status?deliveryPersonnelId=${uid}`)
        if (res.code === 200 && res.data) {
            const batch = res.data.find(b => String(b.batchId) === String(batchId.value))
            if (batch?.orders?.length > 0) {
                orders.value = batch.orders
                batchStatus.value = batch.status ?? 1
                batchInfo.value = {
                    deliveryTime: batch.startedAt || batch.createdAt || '未知',
                    orderCount: batch.orders.length
                }
                return batch.orders
            }
        }
        console.error('未找到批次数据, batchId:', batchId.value)
        router.push('/driver/delivery-batch')
        return []
    } catch (e) {
        console.error('获取批次订单失败:', e)
        return []
    }
}

// ── Map ──
const initMap = (warehouse) => {
    if (!warehouse?.longitude || !warehouse?.latitude) return
    const center = new window.TMap.LatLng(warehouse.latitude, warehouse.longitude)
    map = new window.TMap.Map(document.getElementById('map-container'), { zoom: 12, center })

    startMarker = new window.TMap.MultiMarker({
        map,
        styles: {
            start: new window.TMap.MarkerStyle({
                width: 40, height: 40, anchor: { x: 20, y: 40 },
                src: 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
                    '<svg xmlns="http://www.w3.org/2000/svg" width="40" height="40"><circle cx="20" cy="20" r="18" fill="#FF6B6B" stroke="white" stroke-width="3"/><text x="20" y="26" text-anchor="middle" fill="white" font-size="16" font-weight="bold">起</text></svg>'
                )
            })
        },
        geometries: [{ id: 'start', styleId: 'start', position: center }]
    })
}

const addDeliveryMarkers = (orderList) => {
    if (!map || !orderList?.length) return
    const geometries = []
    const styles = {}
    orderList.forEach((order, i) => {
        if (order.address?.longitude && order.address?.latitude) {
            const sid = `m-${i}`
            styles[sid] = new window.TMap.MarkerStyle({
                width: 32, height: 32, anchor: { x: 16, y: 32 },
                src: 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
                    `<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32"><circle cx="16" cy="16" r="14" fill="#0f1419" stroke="white" stroke-width="3"/><text x="16" y="21" text-anchor="middle" fill="white" font-size="14" font-weight="bold">${i + 1}</text></svg>`
                )
            })
            geometries.push({
                id: sid, styleId: sid,
                position: new window.TMap.LatLng(order.address.latitude, order.address.longitude)
            })
        }
    })
    if (geometries.length) {
        deliveryMarkers.push(new window.TMap.MultiMarker({ map, styles, geometries }))
    }
}

const createTruck = (pos) => {
    if (deliveryTruckMarker) deliveryTruckMarker.setMap(null)
    deliveryTruckMarker = new window.TMap.MultiMarker({
        map,
        styles: {
            truck: new window.TMap.MarkerStyle({
                width: 48, height: 48, anchor: { x: 24, y: 24 },
                src: 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
                    '<svg xmlns="http://www.w3.org/2000/svg" width="48" height="48"><rect x="8" y="16" width="24" height="16" fill="#FF6B6B" stroke="white" stroke-width="2" rx="2"/><rect x="32" y="20" width="8" height="12" fill="#FF6B6B" stroke="white" stroke-width="2" rx="1"/><circle cx="16" cy="34" r="3" fill="#333" stroke="white"/><circle cx="32" cy="34" r="3" fill="#333" stroke="white"/></svg>'
                )
            })
        },
        geometries: [{ id: 'truck', styleId: 'truck', position: pos }]
    })
}

// ── Route + Location polling (by batchId) ──
const fetchRouteByBatch = async () => {
    try {
        const res = await request.get('/delivery-batch/route-by-batch', { params: { batchId: batchId.value } })
        if (res.code === 200 && res.data) {
            const { polyline, totalDistance, totalDuration } = res.data

            // Decompress polyline
            const coors = [...polyline]
            for (let i = 2; i < coors.length; i++) coors[i] = coors[i - 2] + coors[i] / 1000000
            const path = []
            for (let i = 0; i < coors.length; i += 2) {
                if (i + 1 < coors.length) path.push(new window.TMap.LatLng(coors[i], coors[i + 1]))
            }

            // Draw route
            if (routePolyline) routePolyline.setMap(null)
            routePolyline = new window.TMap.MultiPolyline({
                map,
                styles: { route: new window.TMap.PolylineStyle({ color: '#3777FF', width: 6, borderWidth: 2, borderColor: '#FFF', lineCap: 'round' }) },
                geometries: [{ id: 'route', styleId: 'route', paths: path }]
            })

            // Update info
            const dist = totalDistance + ' 公里'
            const h = Math.floor(totalDuration / 60), m = totalDuration % 60
            routeInfo.value = {
                distance: dist,
                duration: h > 0 ? (m > 0 ? `${h}小时${m}分钟` : `${h}小时`) : `${m}分钟`
            }
            return true
        }
    } catch (e) { console.log('暂无路线数据') }
    return false
}

const fetchLocationByBatch = async () => {
    try {
        const res = await request.get('/delivery-batch/location-by-batch', { params: { batchId: batchId.value } })
        if (res.code === 200 && res.data) {
            const { latitude, longitude, address, status, remainingTime } = res.data
            currentLocation.value = { address, lat: latitude, lng: longitude, remainingTime: remainingTime || 0 }

            const pos = new window.TMap.LatLng(latitude, longitude)
            if (!deliveryTruckMarker) createTruck(pos)
            else deliveryTruckMarker.updateGeometries([{ id: 'truck', styleId: 'truck', position: pos }])
            map.setCenter(pos)

            if (status === 'COMPLETED') {
                batchStatus.value = 2
                stopPolling()
            }
        }
    } catch (e) { console.error('获取位置失败:', e) }
}

const startPolling = () => {
    fetchLocationByBatch()
    pollingTimer = setInterval(fetchLocationByBatch, 3000)
}
const stopPolling = () => { if (pollingTimer) { clearInterval(pollingTimer); pollingTimer = null } }

// ── Complete batch ──
const handleCompleteBatch = async () => {
    try {
        await ElMessageBox.confirm(
            `确定完成该批次的配送吗？包含 ${orders.value.length} 个订单`,
            '确认', { confirmButtonText: '确认送达', cancelButtonText: '取消' }
        )
    } catch { return }
    completing.value = true
    try {
        const res = await request.post(`/delivery-batch/complete-batch?batchId=${batchId.value}`)
        if (res.code === 200) {
            batchStatus.value = 2
            stopPolling()
            ElMessage.success('配送完成！')
            router.push('/driver/delivery-batch')
        } else {
            ElMessage.error(res.message || '操作失败')
        }
    } finally { completing.value = false }
}

// ── Format ──
const formatTime = (t) => {
    if (!t) return '未知'
    const d = new Date(t)
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}
const formatRemainingTime = (min) => {
    if (!min || min <= 0) return '即将到达'
    const m = Math.ceil(min)
    return m >= 60 ? `${Math.floor(m / 60)}小时${m % 60 > 0 ? m % 60 + '分钟' : ''}` : `${m}分钟`
}

// ── Mount ──
onMounted(async () => {
    // 1. Get batchId
    batchId.value = route.params.batchId || route.query.batchId
    if (!batchId.value) { router.push('/driver/delivery-batch'); return }

    // 2. Wait for map SDK
    try {
        await new Promise((resolve, reject) => {
            if (window.TMap) { resolve(); return }
            let n = 0
            const t = setInterval(() => { n++; if (window.TMap) { clearInterval(t); resolve() } else if (n >= 50) { clearInterval(t); reject() } }, 100)
        })
    } catch { console.error('地图SDK加载失败'); return }

    // 3. Warehouse
    const whId = getCurrentUserWarehouseId()
    if (!whId) { router.push('/'); return }
    const wh = await fetchWarehouseInfo(whId)
    if (!wh) return

    // 4. Orders
    const orderList = await fetchBatchOrders()
    if (!orderList?.length) return

    // 5. Map
    initMap(wh)
    addDeliveryMarkers(orderList)

    // 6. Route + polling (only for 配送中 batches)
    if (batchStatus.value === 1) {
        const hasRoute = await fetchRouteByBatch()
        if (hasRoute) startPolling()
    }
})

onUnmounted(() => stopPolling())
</script>

<style scoped>
.detail-container { width: 100%; max-width: 1400px; margin: 0 auto; padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
h2 { font-size: 18px; font-weight: 700; color: #0f1419; margin: 0; }
.batch-info { display: flex; gap: 16px; font-size: 13px; color: #536471; align-items: center; }

.status-badge { padding: 3px 12px; border-radius: 9999px; font-size: 11px; font-weight: 600; }
.status-pending { background: #fff7e6; color: #b45309; border: 1px solid #fde68a; }
.status-delivering { background: #eff6ff; color: #1d4ed8; border: 1px solid #bfdbfe; }
.status-completed { background: #f0fdf4; color: #15803d; border: 1px solid #bbf7d0; }

.map-section { background: #fff; border: 1px solid #eff3f4; border-radius: 12px; padding: 16px; margin-bottom: 20px; }
.map-container { width: 100%; height: 500px; border-radius: 8px; overflow: hidden; }
.route-info { display: flex; flex-direction: column; gap: 10px; margin-top: 12px; padding-top: 12px; border-top: 1px solid #eff3f4; }
.route-info-row { display: flex; gap: 24px; align-items: center; }
.info-item { display: flex; align-items: center; gap: 6px; }
.info-item .label { font-size: 13px; color: #536471; }
.info-item .value { font-size: 14px; color: #0f1419; font-weight: 700; }

.map-actions { display: flex; gap: 8px; margin-top: 12px; padding-top: 12px; border-top: 1px solid #eff3f4; }
.btn { display: inline-flex; align-items: center; justify-content: center; padding: 0 20px; height: 40px; border-radius: 9999px; font-size: 14px; font-weight: 600; cursor: pointer; border: none; transition: all 0.15s; }
.btn-primary { background: #0f1419; color: #fff; }
.btn-primary:hover { background: #272c30; }
.btn-primary:disabled { background: #8899a6; cursor: not-allowed; }

.delivery-points { background: #fff; border: 1px solid #eff3f4; border-radius: 12px; padding: 20px; }
.delivery-points h3 { font-size: 15px; font-weight: 600; color: #0f1419; margin: 0 0 16px 0; }
.point-list { display: flex; flex-direction: column; gap: 8px; }
.point-item { display: flex; align-items: flex-start; gap: 12px; padding: 12px; background: #f7f9f9; border-radius: 10px; transition: background 0.15s; }
.point-item:hover { background: #eff3f4; }
.point-marker { flex-shrink: 0; width: 30px; height: 30px; display: flex; align-items: center; justify-content: center; background: #0f1419; border-radius: 50%; font-size: 12px; font-weight: 700; color: #fff; }
.start-point .point-marker { background: #FF6B6B; }
.point-content { flex: 1; }
.point-title { font-size: 13px; font-weight: 600; color: #0f1419; margin-bottom: 2px; }
.point-address { font-size: 12px; color: #536471; line-height: 1.4; }
.point-product { font-size: 11px; color: #8899a6; margin-top: 2px; }

@media (max-width: 640px) {
    .page-header { flex-direction: column; align-items: flex-start; gap: 8px; }
    .map-container { height: 350px; }
    .route-info-row { flex-direction: column; gap: 8px; }
}
</style>
