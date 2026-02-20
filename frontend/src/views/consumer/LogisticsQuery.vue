<template>
    <div class="page-container">
        <h1>物流查询</h1>

        <!-- Search -->
        <div class="filter-bar">
            <div class="search-box" style="max-width:100%">
                <input
                    type="text"
                    v-model="searchOrderId"
                    placeholder="输入订单号查询物流..."
                    class="search-input"
                    @keyup.enter="searchTracking"
                />
                <button @click="searchTracking" class="btn btn-primary">查询</button>
            </div>
        </div>

        <!-- Orders in transit -->
        <div class="section-title" v-if="transitOrders.length">运输中的订单</div>
        <div class="order-list">
            <div
                v-for="order in transitOrders"
                :key="order.orderId"
                class="tracking-card"
                :class="{ active: selectedOrderId === order.orderId }"
                @click="selectOrder(order.orderId)"
            >
                <div class="tracking-card-header">
                    <span class="order-id">#{{ order.orderId }}</span>
                    <span class="badge" :class="getStatusClass(order.status)">{{ getStatusText(order.status) }}</span>
                </div>
                <div class="tracking-card-body">
                    <div class="product-info">
                        <img :src="order.imageUrl || '/placeholder.png'" :alt="order.productName" class="product-thumb" />
                        <div>
                            <div class="product-name">{{ order.productName }}</div>
                            <div class="product-meta">¥{{ order.unitPrice }} × {{ order.quantity }}</div>
                        </div>
                    </div>
                </div>
                <div class="tracking-card-footer" v-if="order.trackingInfo">
                    <div class="progress-bar">
                        <div class="progress-fill" :style="{ width: (order.trackingInfo.progress || 0) + '%' }"></div>
                    </div>
                    <div class="tracking-meta">
                        <span>{{ order.trackingInfo.warehouseName || '仓库' }}</span>
                        <span v-if="order.trackingInfo.remainingTime">
                            预计 {{ formatRemainingTime(order.trackingInfo.remainingTime) }}
                        </span>
                    </div>
                </div>
                <div class="tracking-card-footer" v-else-if="order.status >= 2 && order.status <= 3">
                    <span class="tracking-hint">点击查看物流详情</span>
                </div>
            </div>
        </div>

        <div v-if="transitOrders.length === 0 && !loading" class="empty-state">暂无运输中的订单</div>

        <!-- Map modal -->
        <div v-if="showMap" class="map-overlay" @click.self="closeMap">
            <div class="map-modal">
                <div class="map-modal-header">
                    <h3>订单 #{{ selectedOrderId }} 物流追踪</h3>
                    <button class="btn-close" @click="closeMap">✕</button>
                </div>
                <div class="map-modal-body">
                    <div id="tracking-map" class="map-container"></div>
                    <div v-if="trackingDetail" class="tracking-detail">
                        <div class="detail-row">
                            <div class="detail-item">
                                <span class="label">状态</span>
                                <span class="value badge" :class="getBatchStatusClass(trackingDetail.batchStatus)">
                                    {{ getBatchStatusText(trackingDetail.batchStatus) }}
                                </span>
                            </div>
                            <div class="detail-item" v-if="trackingDetail.totalDistance">
                                <span class="label">总距离</span>
                                <span class="value">{{ trackingDetail.totalDistance }} 公里</span>
                            </div>
                            <div class="detail-item" v-if="trackingDetail.remainingTime">
                                <span class="label">预计剩余</span>
                                <span class="value">{{ formatRemainingTime(trackingDetail.remainingTime) }}</span>
                            </div>
                            <div class="detail-item" v-if="trackingDetail.progress">
                                <span class="label">进度</span>
                                <span class="value">{{ trackingDetail.progress }}%</span>
                            </div>
                        </div>
                        <div class="detail-row" v-if="trackingDetail.warehouseName">
                            <div class="detail-item full">
                                <span class="label">发货仓库</span>
                                <span class="value">{{ trackingDetail.warehouseName }}</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
/* global TMap */
import { ref, onMounted, onUnmounted } from 'vue'
import request from '@/utils/request'

const searchOrderId = ref('')
const transitOrders = ref([])
const selectedOrderId = ref(null)
const showMap = ref(false)
const trackingDetail = ref(null)
const loading = ref(false)

let map = null
let truckMarker = null
let routePolyline = null
let warehouseMarker = null
let pollingTimer = null

// ── Load orders ──
const loadTransitOrders = async () => {
    loading.value = true
    try {
        const userInfo = JSON.parse(sessionStorage.getItem('userInfo') || '{}')
        if (!userInfo.id) return
        // Get orders with status 1(已发货), 2(已揽收), 3(运输中)
        const res = await request.get('/orders/my', { params: { customerId: userInfo.id } })
        if (res.code === 200 && res.data) {
            transitOrders.value = res.data.filter(o => o.status >= 1 && o.status <= 3)
        }
    } catch (e) {
        console.error('获取订单失败:', e)
    } finally { loading.value = false }
}

// ── Search ──
const searchTracking = () => {
    const id = parseInt(searchOrderId.value)
    if (!id) return
    selectOrder(id)
}

// ── Select order → fetch tracking ──
const selectOrder = async (orderId) => {
    selectedOrderId.value = orderId
    try {
        const res = await request.get('/delivery-batch/track-by-order', { params: { orderId } })
        if (res.code === 200 && res.data) {
            trackingDetail.value = res.data

            // Update the order's tracking info in the list
            const order = transitOrders.value.find(o => o.orderId === orderId)
            if (order) order.trackingInfo = res.data

            // Show map if delivering
            if (res.data.batchStatus === 1 && res.data.currentLat) {
                showMap.value = true
                await initTrackingMap(res.data)
                startPolling(orderId)
            } else if (res.data.batchStatus === 2) {
                showMap.value = true
                await initTrackingMap(res.data)
            } else {
                // 待出发 — no map yet
                showMap.value = true
                trackingDetail.value = res.data
                setTimeout(() => initEmptyMap(res.data), 100)
            }
        } else {
            trackingDetail.value = null
            showMap.value = true
            setTimeout(() => initEmptyMap(null), 100)
        }
    } catch (e) {
        console.error('查询物流失败:', e)
    }
}

// ── Map ──
const waitForTMap = () => new Promise((resolve, reject) => {
    if (window.TMap) { resolve(); return }
    let n = 0
    const t = setInterval(() => { n++; if (window.TMap) { clearInterval(t); resolve() } else if (n >= 50) { clearInterval(t); reject() } }, 100)
})

const initTrackingMap = async (data) => {
    await waitForTMap()
    await new Promise(r => setTimeout(r, 100)) // wait for DOM

    const el = document.getElementById('tracking-map')
    if (!el) return

    const center = data.currentLat
        ? new TMap.LatLng(data.currentLat, data.currentLng)
        : new TMap.LatLng(39.9, 116.4) // default Beijing

    if (map) { map.destroy(); map = null }
    truckMarker = null; routePolyline = null; warehouseMarker = null

    map = new TMap.Map(el, { zoom: 10, center })

    // Draw route polyline
    if (data.polyline) {
        const coors = [...data.polyline]
        for (let i = 2; i < coors.length; i++) coors[i] = coors[i - 2] + coors[i] / 1000000
        const path = []
        for (let i = 0; i < coors.length; i += 2) {
            if (i + 1 < coors.length) path.push(new TMap.LatLng(coors[i], coors[i + 1]))
        }
        routePolyline = new TMap.MultiPolyline({
            map,
            styles: { route: new TMap.PolylineStyle({ color: '#3777FF', width: 6, borderWidth: 2, borderColor: '#FFF', lineCap: 'round' }) },
            geometries: [{ id: 'route', styleId: 'route', paths: path }]
        })
    }

    // Truck marker
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

const initEmptyMap = async (data) => {
    await waitForTMap()
    await new Promise(r => setTimeout(r, 100))
    const el = document.getElementById('tracking-map')
    if (!el) return
    if (map) { map.destroy(); map = null }
    map = new TMap.Map(el, { zoom: 5, center: new TMap.LatLng(35, 110) })
}

// ── Polling ──
const startPolling = (orderId) => {
    stopPolling()
    pollingTimer = setInterval(async () => {
        try {
            const res = await request.get('/delivery-batch/track-by-order', { params: { orderId } })
            if (res.code === 200 && res.data) {
                trackingDetail.value = res.data
                if (res.data.currentLat && truckMarker) {
                    const pos = new TMap.LatLng(res.data.currentLat, res.data.currentLng)
                    truckMarker.updateGeometries([{ id: 'truck', styleId: 'truck', position: pos }])
                }
                if (res.data.batchStatus === 2) stopPolling()
            }
        } catch (e) { /* ignore */ }
    }, 5000)
}
const stopPolling = () => { if (pollingTimer) { clearInterval(pollingTimer); pollingTimer = null } }

const closeMap = () => {
    showMap.value = false
    stopPolling()
    if (map) { map.destroy(); map = null }
    truckMarker = null; routePolyline = null
}

// ── Helpers ──
const getStatusText = (s) => {
    const m = { 0: '未发货', 1: '已发货', 2: '已揽收', 3: '运输中', 4: '已到达', 5: '已收货' }
    return m[s] || '未知'
}
const getStatusClass = (s) => {
    if (s <= 1) return 'badge-pending'
    if (s <= 3) return 'badge-transit'
    return 'badge-done'
}
const getBatchStatusText = (s) => {
    const m = { 0: '待出发', 1: '配送中', 2: '已完成' }
    return m[s] || '未知'
}
const getBatchStatusClass = (s) => {
    const m = { 0: 'badge-pending', 1: 'badge-transit', 2: 'badge-done' }
    return m[s] || ''
}
const formatRemainingTime = (min) => {
    if (!min || min <= 0) return '即将到达'
    const m = Math.ceil(min)
    return m >= 60 ? `${Math.floor(m / 60)}小时${m % 60 > 0 ? m % 60 + '分钟' : ''}` : `${m}分钟`
}

onMounted(() => loadTransitOrders())
onUnmounted(() => { stopPolling(); if (map) map.destroy() })
</script>

<style scoped>
.section-title { font-size: 14px; font-weight: 600; color: #536471; margin: 16px 0 10px; }

.order-list { display: flex; flex-direction: column; gap: 10px; }

.tracking-card {
    background: #fff; border: 1px solid #eff3f4; border-radius: 12px;
    padding: 14px; cursor: pointer; transition: all 0.15s;
}
.tracking-card:hover { border-color: #cfd9de; }
.tracking-card.active { border-color: #0f1419; }

.tracking-card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.order-id { font-size: 13px; font-weight: 600; color: #0f1419; }

.badge { padding: 2px 10px; border-radius: 9999px; font-size: 11px; font-weight: 600; }
.badge-pending { background: #fff7e6; color: #b45309; border: 1px solid #fde68a; }
.badge-transit { background: #eff6ff; color: #1d4ed8; border: 1px solid #bfdbfe; }
.badge-done { background: #f0fdf4; color: #15803d; border: 1px solid #bbf7d0; }

.tracking-card-body { display: flex; }
.product-info { display: flex; gap: 10px; align-items: center; }
.product-thumb { width: 48px; height: 48px; border-radius: 8px; object-fit: cover; background: #f7f9f9; }
.product-name { font-size: 13px; font-weight: 600; color: #0f1419; }
.product-meta { font-size: 12px; color: #536471; margin-top: 2px; }

.tracking-card-footer { margin-top: 10px; padding-top: 10px; border-top: 1px solid #eff3f4; }
.progress-bar { height: 4px; background: #eff3f4; border-radius: 2px; overflow: hidden; margin-bottom: 6px; }
.progress-fill { height: 100%; background: #0f1419; border-radius: 2px; transition: width 0.5s; }
.tracking-meta { display: flex; justify-content: space-between; font-size: 11px; color: #536471; }
.tracking-hint { font-size: 12px; color: #8899a6; }

.empty-state { text-align: center; padding: 40px; color: #8899a6; font-size: 14px; }

/* Map modal */
.map-overlay {
    position: fixed; top: 0; left: 0; right: 0; bottom: 0;
    background: rgba(0,0,0,0.5); z-index: 1000;
    display: flex; align-items: center; justify-content: center;
}
.map-modal {
    background: #fff; border-radius: 16px; width: 90%; max-width: 800px;
    max-height: 90vh; overflow: hidden; box-shadow: 0 20px 60px rgba(0,0,0,0.3);
}
.map-modal-header {
    display: flex; justify-content: space-between; align-items: center;
    padding: 16px 20px; border-bottom: 1px solid #eff3f4;
}
.map-modal-header h3 { margin: 0; font-size: 15px; font-weight: 600; color: #0f1419; }
.btn-close {
    width: 32px; height: 32px; border-radius: 50%; border: none;
    background: #f7f9f9; font-size: 16px; cursor: pointer; color: #536471;
    display: flex; align-items: center; justify-content: center;
}
.btn-close:hover { background: #eff3f4; }

.map-modal-body { padding: 16px; }
.map-container { width: 100%; height: 400px; border-radius: 10px; overflow: hidden; }

.tracking-detail { margin-top: 12px; padding-top: 12px; border-top: 1px solid #eff3f4; }
.detail-row { display: flex; gap: 20px; flex-wrap: wrap; margin-bottom: 8px; }
.detail-item { display: flex; align-items: center; gap: 6px; }
.detail-item.full { flex: 1; }
.detail-item .label { font-size: 12px; color: #536471; }
.detail-item .value { font-size: 13px; color: #0f1419; font-weight: 600; }

@media (max-width: 640px) {
    .map-modal { width: 95%; }
    .map-container { height: 300px; }
    .detail-row { flex-direction: column; gap: 8px; }
}
</style>
