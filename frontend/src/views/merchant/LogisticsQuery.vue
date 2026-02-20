<template>
    <div class="page-container">
        <!-- Order list view -->
        <div v-if="!selectedOrder">
            <h1>物流查询</h1>
            <div class="filter-bar">
                <div class="search-box">
                    <input
                        type="text"
                        v-model="searchKeyword"
                        placeholder="搜索订单号、商品或顾客名称..."
                        class="search-input"
                        @keyup.enter="handleSearch"
                    />
                    <button @click="handleSearch" class="btn btn-ghost">搜索</button>
                </div>
                <div class="tabs">
                    <button :class="['tab', { active: currentTab === 'all' }]" @click="changeTab('all')">全部</button>
                    <button :class="['tab', { active: currentTab === 'unpaid' }]" @click="changeTab('unpaid')">未发货</button>
                    <button :class="['tab', { active: currentTab === 'shipped' }]" @click="changeTab('shipped')">已发货</button>
                    <button :class="['tab', { active: currentTab === 'picked' }]" @click="changeTab('picked')">已揽收</button>
                    <button :class="['tab', { active: currentTab === 'transit' }]" @click="changeTab('transit')">运输中</button>
                    <button :class="['tab', { active: currentTab === 'arrived' }]" @click="changeTab('arrived')">已到达</button>
                    <button :class="['tab', { active: currentTab === 'received' }]" @click="changeTab('received')">已收货</button>
                </div>
            </div>

            <div v-if="filteredOrders.length === 0 && !loading" class="empty-state"><p>暂无订单</p></div>

            <div class="order-list">
                <div
                    v-for="order in filteredOrders"
                    :key="order.orderId"
                    class="order-card"
                    @click="viewTracking(order)"
                >
                    <div class="order-card-left">
                        <img :src="order.imageUrl || '/placeholder.png'" :alt="order.productName" class="product-thumb" />
                    </div>
                    <div class="order-card-center">
                        <div class="order-title">{{ order.productName }}</div>
                        <div class="order-meta">
                            #{{ order.orderId }} · <span class="customer-name">{{ order.customerName || '顾客' }}</span> · ¥{{ order.totalAmount }}
                        </div>
                        <div class="order-time-text">{{ formatTime(order.orderTime) }}</div>
                    </div>
                    <div class="order-card-right">
                        <span class="badge" :class="getStatusClass(order.status)">{{ getStatusText(order.status) }}</span>
                        <span class="arrow">→</span>
                    </div>
                </div>
            </div>
        </div>

        <!-- Tracking detail view -->
        <div v-else class="detail-view">
            <!-- Map -->
            <div class="map-section">
                <div id="tracking-map" class="map-container"></div>
                <div v-if="trackingData.totalDistance" class="route-info">
                    <div class="route-info-row">
                        <div class="info-item">
                            <span class="label">总距离：</span>
                            <span class="value">{{ trackingData.totalDistance }} 公里</span>
                        </div>
                        <div class="info-item" v-if="trackingData.totalDuration">
                            <span class="label">预计总时间：</span>
                            <span class="value">{{ formatDuration(trackingData.totalDuration) }}</span>
                        </div>
                    </div>
                    <div class="route-info-row" v-if="trackingData.remainingTime > 0">
                        <div class="info-item">
                            <span class="label">剩余时间：</span>
                            <span class="value highlight">{{ formatRemainingTime(trackingData.remainingTime) }}</span>
                        </div>
                        <div class="info-item" v-if="trackingData.progress">
                            <span class="label">配送进度：</span>
                            <span class="value">
                                <span class="progress-inline">
                                    <span class="progress-bar-inline">
                                        <span class="progress-fill-inline" :style="{ width: trackingData.progress + '%' }"></span>
                                    </span>
                                    {{ trackingData.progress }}%
                                </span>
                            </span>
                        </div>
                    </div>
                </div>
                <div v-if="!trackingData.batchStatus && trackingData.batchStatus !== 0" class="no-tracking">
                    该订单暂无物流信息
                </div>
            </div>

            <!-- Info cards -->
            <div class="info-cards">
                <div class="info-card" v-if="trackingData.warehouseName">
                    <div class="info-card-icon">🏭</div>
                    <div class="info-card-content">
                        <div class="info-card-label">发货仓库</div>
                        <div class="info-card-value">{{ trackingData.warehouseName }}</div>
                        <div class="info-card-sub" v-if="trackingData.warehouseAddress">{{ trackingData.warehouseAddress }}</div>
                    </div>
                </div>
                <div class="info-card">
                    <div class="info-card-icon">👤</div>
                    <div class="info-card-content">
                        <div class="info-card-label">收货人</div>
                        <div class="info-card-value">{{ selectedOrder.customerName || '顾客' }}</div>
                        <div class="info-card-sub">
                            {{ selectedOrder.address?.province }}{{ selectedOrder.address?.city }}{{ selectedOrder.address?.district }}{{ selectedOrder.address?.detailAddress }}
                        </div>
                    </div>
                </div>
                <div class="info-card">
                    <div class="info-card-icon">🛒</div>
                    <div class="info-card-content">
                        <div class="info-card-label">商品信息</div>
                        <div class="info-card-value">{{ selectedOrder.productName }} × {{ selectedOrder.quantity }}</div>
                        <div class="info-card-sub">合计 ¥{{ selectedOrder.totalAmount }}</div>
                    </div>
                </div>
            </div>

            <!-- Timeline -->
            <div class="timeline-section">
                <h3>物流动态</h3>
                <div class="timeline">
                    <div class="timeline-item" :class="{ active: selectedOrder.status >= 5 }">
                        <div class="timeline-dot"></div>
                        <div class="timeline-content">
                            <div class="timeline-title">已收货</div>
                            <div class="timeline-time" v-if="selectedOrder.receiveTime">{{ formatFullTime(selectedOrder.receiveTime) }}</div>
                        </div>
                    </div>
                    <div class="timeline-item" :class="{ active: selectedOrder.status >= 4 }">
                        <div class="timeline-dot"></div>
                        <div class="timeline-content">
                            <div class="timeline-title">已到达</div>
                            <div class="timeline-time" v-if="selectedOrder.deliveryTime">{{ formatFullTime(selectedOrder.deliveryTime) }}</div>
                        </div>
                    </div>
                    <div class="timeline-item" :class="{ active: selectedOrder.status >= 3, delivering: trackingData.batchStatus === 1 }">
                        <div class="timeline-dot"></div>
                        <div class="timeline-content">
                            <div class="timeline-title">运输中</div>
                            <div class="timeline-time" v-if="trackingData.startedAt">{{ formatFullTime(trackingData.startedAt) }}</div>
                            <div class="timeline-detail" v-if="trackingData.batchStatus === 1 && trackingData.remainingTime > 0">
                                预计 {{ formatRemainingTime(trackingData.remainingTime) }} 后送达
                            </div>
                        </div>
                    </div>
                    <div class="timeline-item" :class="{ active: selectedOrder.status >= 2 }">
                        <div class="timeline-dot"></div>
                        <div class="timeline-content">
                            <div class="timeline-title">已揽收</div>
                            <div class="timeline-time" v-if="selectedOrder.pickupTime">{{ formatFullTime(selectedOrder.pickupTime) }}</div>
                        </div>
                    </div>
                    <div class="timeline-item" :class="{ active: selectedOrder.status >= 1 }">
                        <div class="timeline-dot"></div>
                        <div class="timeline-content">
                            <div class="timeline-title">已发货</div>
                            <div class="timeline-time" v-if="selectedOrder.shipTime">{{ formatFullTime(selectedOrder.shipTime) }}</div>
                        </div>
                    </div>
                    <div class="timeline-item active">
                        <div class="timeline-dot"></div>
                        <div class="timeline-content">
                            <div class="timeline-title">已下单</div>
                            <div class="timeline-time">{{ formatFullTime(selectedOrder.orderTime) }}</div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
/* global TMap */
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const searchKeyword = ref('')
const activeSearch = ref('')
const currentTab = ref('all')
const orders = ref([])
const selectedOrder = ref(null)
const trackingData = ref({})

let map = null
let truckMarker = null
let routePolyline = null
let destMarker = null
let pollingTimer = null

const statusMap = {
    all: null, unpaid: 0, shipped: 1, picked: 2, transit: 3, arrived: 4, received: 5
}

const filteredOrders = computed(() => {
    let list = orders.value
    const statusVal = statusMap[currentTab.value]
    if (statusVal !== null && statusVal !== undefined) {
        list = list.filter(o => o.status === statusVal)
    }
    if (activeSearch.value) {
        const q = activeSearch.value.toLowerCase()
        list = list.filter(o =>
            String(o.orderId).includes(q) ||
            (o.productName && o.productName.toLowerCase().includes(q)) ||
            (o.customerName && o.customerName.toLowerCase().includes(q))
        )
    }
    return list
})

const loadOrders = async () => {
    loading.value = true
    try {
        const user = JSON.parse(sessionStorage.getItem('userInfo') || '{}')
        if (!user.id) return
        const res = await request.get('/orders/merchant', { params: { merchantId: user.id } })
        if (res.code === 200) orders.value = res.data || []
    } catch (e) { /* handled */ } finally { loading.value = false }
}

const changeTab = (tab) => { currentTab.value = tab }
const handleSearch = () => { activeSearch.value = searchKeyword.value }

const viewTracking = (order) => {
    router.push(`/merchant/logistics-query/${order.orderId}`)
}

const loadOrderDetail = async (orderId) => {
    let order = orders.value.find(o => String(o.orderId) === String(orderId))
    if (!order) {
        await loadOrders()
        order = orders.value.find(o => String(o.orderId) === String(orderId))
    }
    if (!order) return

    selectedOrder.value = order
    trackingData.value = {}

    try {
        const res = await request.get('/delivery-batch/track-by-order', { params: { orderId } })
        if (res.code === 200 && res.data) {
            trackingData.value = res.data
            setTimeout(async () => {
                await initMap(res.data, order)
                if (res.data.batchStatus === 1) startPolling(orderId)
            }, 150)
        } else {
            setTimeout(() => initEmptyMap(), 150)
        }
    } catch (e) {
        // error handled by GlobalExceptionHandler
        setTimeout(() => initEmptyMap(), 150)
    }
}

watch(() => route.params.orderId, (newId) => {
    if (newId) {
        loadOrderDetail(newId)
    } else {
        selectedOrder.value = null
        trackingData.value = {}
        stopPolling()
        if (map) { map.destroy(); map = null }
        truckMarker = null; routePolyline = null; destMarker = null
    }
}, { immediate: false })

// ── Map ──
const waitForTMap = () => new Promise((resolve, reject) => {
    if (window.TMap) { resolve(); return }
    let n = 0
    const t = setInterval(() => {
        n++
        if (window.TMap) { clearInterval(t); resolve() }
        else if (n >= 50) { clearInterval(t); reject() }
    }, 100)
})

const initMap = async (data, order) => {
    try { await waitForTMap() } catch { return }
    const el = document.getElementById('tracking-map')
    if (!el) return

    if (map) { map.destroy(); map = null }
    truckMarker = null; routePolyline = null; destMarker = null

    const center = data.currentLat
        ? new TMap.LatLng(data.currentLat, data.currentLng)
        : new TMap.LatLng(39.9, 116.4)

    map = new TMap.Map(el, { zoom: 10, center })

    if (data.polyline) {
        const coors = [...data.polyline]
        for (let i = 2; i < coors.length; i++) coors[i] = coors[i - 2] + coors[i] / 1000000
        const path = []
        for (let i = 0; i < coors.length; i += 2) {
            if (i + 1 < coors.length) path.push(new TMap.LatLng(coors[i], coors[i + 1]))
        }
        if (path.length) {
            routePolyline = new TMap.MultiPolyline({
                map,
                styles: { route: new TMap.PolylineStyle({ color: '#3777FF', width: 6, borderWidth: 2, borderColor: '#FFF', lineCap: 'round' }) },
                geometries: [{ id: 'route', styleId: 'route', paths: path }]
            })
        }
    }

    if (order.address?.latitude && order.address?.longitude) {
        destMarker = new TMap.MultiMarker({
            map,
            styles: {
                dest: new TMap.MarkerStyle({
                    width: 36, height: 36, anchor: { x: 18, y: 36 },
                    src: 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
                        '<svg xmlns="http://www.w3.org/2000/svg" width="36" height="36"><circle cx="18" cy="18" r="16" fill="#15803d" stroke="white" stroke-width="3"/><text x="18" y="24" text-anchor="middle" fill="white" font-size="16">📍</text></svg>'
                    )
                })
            },
            geometries: [{ id: 'dest', styleId: 'dest', position: new TMap.LatLng(order.address.latitude, order.address.longitude) }]
        })
    }

    if (data.currentLat) {
        truckMarker = new TMap.MultiMarker({
            map,
            styles: {
                truck: new TMap.MarkerStyle({
                    width: 48, height: 48, anchor: { x: 24, y: 24 },
                    src: 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
                        '<svg xmlns="http://www.w3.org/2000/svg" width="48" height="48"><rect x="8" y="16" width="24" height="16" fill="#FF6B6B" stroke="white" stroke-width="2" rx="2"/><rect x="32" y="20" width="8" height="12" fill="#FF6B6B" stroke="white" stroke-width="2" rx="1"/><circle cx="16" cy="34" r="3" fill="#333" stroke="white"/><circle cx="32" cy="34" r="3" fill="#333" stroke="white"/></svg>'
                    )
                })
            },
            geometries: [{ id: 'truck', styleId: 'truck', position: new TMap.LatLng(data.currentLat, data.currentLng) }]
        })
    }
}

const initEmptyMap = async () => {
    try { await waitForTMap() } catch { return }
    const el = document.getElementById('tracking-map')
    if (!el) return
    if (map) { map.destroy(); map = null }
    map = new TMap.Map(el, { zoom: 5, center: new TMap.LatLng(35, 110) })
}

const startPolling = (orderId) => {
    stopPolling()
    pollingTimer = setInterval(async () => {
        try {
            const res = await request.get('/delivery-batch/track-by-order', { params: { orderId } })
            if (res.code === 200 && res.data) {
                trackingData.value = res.data
                if (res.data.currentLat && truckMarker) {
                    const pos = new TMap.LatLng(res.data.currentLat, res.data.currentLng)
                    truckMarker.updateGeometries([{ id: 'truck', styleId: 'truck', position: pos }])
                }
                if (res.data.batchStatus === 2) stopPolling()
            }
        } catch { /* ignore */ }
    }, 5000)
}
const stopPolling = () => { if (pollingTimer) { clearInterval(pollingTimer); pollingTimer = null } }

// ── Helpers ──
const getStatusText = (s) => {
    const m = { '-1': '已取消', 0: '未发货', 1: '已发货', 2: '已揽收', 3: '运输中', 4: '已到达', 5: '已收货' }
    return m[s] || '未知'
}
const getStatusClass = (s) => {
    if (s <= 0) return 'badge-default'
    if (s <= 3) return 'badge-transit'
    if (s === 4) return 'badge-arrived'
    return 'badge-done'
}
const formatTime = (t) => {
    if (!t) return ''
    const d = new Date(t)
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}
const formatFullTime = (t) => {
    if (!t) return ''
    const d = new Date(t)
    return `${d.getMonth() + 1}月${d.getDate()}日 ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}
const formatDuration = (min) => {
    if (!min) return ''
    const h = Math.floor(min / 60), m = min % 60
    return h > 0 ? (m > 0 ? `${h}小时${m}分钟` : `${h}小时`) : `${m}分钟`
}
const formatRemainingTime = (min) => {
    if (!min || min <= 0) return '即将到达'
    const m = Math.ceil(min)
    return m >= 60 ? `${Math.floor(m / 60)}小时${m % 60 > 0 ? m % 60 + '分钟' : ''}` : `${m}分钟`
}

onMounted(async () => {
    await loadOrders()
    if (route.params.orderId) {
        loadOrderDetail(route.params.orderId)
    }
})
onUnmounted(() => { stopPolling(); if (map) map.destroy() })
</script>

<style scoped>
/* Order list */
.order-list { display: flex; flex-direction: column; gap: 8px; }

.order-card {
    display: flex; align-items: center; gap: 12px;
    background: #fff; border: 1px solid #eff3f4; border-radius: 12px;
    padding: 12px 16px; cursor: pointer; transition: all 0.15s;
}
.order-card:hover { border-color: #cfd9de; background: #f7f9f9; }

.product-thumb { width: 52px; height: 52px; border-radius: 10px; object-fit: cover; background: #f7f9f9; flex-shrink: 0; }
.order-card-center { flex: 1; min-width: 0; }
.order-title { font-size: 14px; font-weight: 600; color: #0f1419; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.order-meta { font-size: 12px; color: #536471; margin-top: 2px; }
.customer-name { color: #0f1419; font-weight: 500; }
.order-time-text { font-size: 11px; color: #8899a6; margin-top: 2px; }
.order-card-right { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.arrow { color: #8899a6; font-size: 16px; }

/* Status badges */
.badge-default { background: #f7f9f9; color: #536471; border: 1px solid #eff3f4; }
.badge-transit { background: #eff6ff; color: #1d4ed8; border: 1px solid #bfdbfe; }
.badge-arrived { background: #fff7e6; color: #b45309; border: 1px solid #fde68a; }
.badge-done { background: #f0fdf4; color: #15803d; border: 1px solid #bbf7d0; }

/* No tracking */
.no-tracking { text-align: center; padding: 24px; color: #8899a6; font-size: 14px; }

/* Detail view */
.detail-view { width: 100%; }

/* Progress bar inline */
.progress-inline { display: inline-flex; align-items: center; gap: 8px; }
.progress-bar-inline { width: 80px; height: 4px; background: #eff3f4; border-radius: 2px; overflow: hidden; display: inline-block; }
.progress-fill-inline { height: 100%; background: #0f1419; border-radius: 2px; transition: width 0.5s; display: block; }

/* Info cards */
.info-cards { display: flex; flex-direction: column; gap: 8px; margin-bottom: 16px; }
.info-card {
    display: flex; gap: 12px; align-items: flex-start;
    background: #fff; border: 1px solid #eff3f4; border-radius: 12px; padding: 14px 16px;
}
.info-card-icon { font-size: 20px; flex-shrink: 0; margin-top: 2px; }
.info-card-content { flex: 1; }
.info-card-label { font-size: 11px; color: #8899a6; text-transform: uppercase; letter-spacing: 0.5px; }
.info-card-value { font-size: 14px; color: #0f1419; font-weight: 600; margin-top: 2px; }
.info-card-sub { font-size: 12px; color: #536471; margin-top: 2px; }

/* Timeline */
.timeline-section { background: #fff; border: 1px solid #eff3f4; border-radius: 12px; padding: 20px; }
.timeline-section h3 { font-size: 15px; font-weight: 600; color: #0f1419; margin: 0 0 16px 0; }

.timeline { position: relative; padding-left: 24px; }
.timeline::before {
    content: ''; position: absolute; left: 7px; top: 4px; bottom: 4px;
    width: 2px; background: #eff3f4;
}
.timeline-item { position: relative; padding-bottom: 20px; }
.timeline-item:last-child { padding-bottom: 0; }
.timeline-dot {
    position: absolute; left: -24px; top: 2px;
    width: 16px; height: 16px; border-radius: 50%;
    background: #eff3f4; border: 2px solid #fff;
    box-shadow: 0 0 0 2px #eff3f4;
}
.timeline-item.active .timeline-dot { background: #0f1419; box-shadow: 0 0 0 2px #0f1419; }
.timeline-item.delivering .timeline-dot { background: #1d4ed8; box-shadow: 0 0 0 2px #1d4ed8; animation: pulse 2s infinite; }

@keyframes pulse {
    0%, 100% { box-shadow: 0 0 0 2px #1d4ed8; }
    50% { box-shadow: 0 0 0 6px rgba(29, 78, 216, 0.2); }
}

.timeline-content { padding-left: 4px; }
.timeline-title { font-size: 13px; font-weight: 600; color: #8899a6; }
.timeline-item.active .timeline-title { color: #0f1419; }
.timeline-time { font-size: 12px; color: #536471; margin-top: 2px; }
.timeline-detail { font-size: 12px; color: #1d4ed8; margin-top: 2px; font-weight: 500; }

@media (max-width: 640px) {
    .map-container { height: 300px; }
    .route-info-row { flex-direction: column; gap: 6px; }
}
</style>
