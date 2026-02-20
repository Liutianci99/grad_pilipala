<template>
    <div class="page-container">
        <h1>订单管理</h1>
        <div class="filter-bar">
            <div class="search-box">
                <input type="text" v-model="searchKeyword" placeholder="搜索订单号、商品或顾客..." class="search-input" @keyup.enter="loadOrders" />
                <button @click="loadOrders" class="btn btn-ghost">搜索</button>
            </div>
            <div class="tabs">
                <button :class="['tab', { active: currentStatus === 'all' }]" @click="changeStatus('all')">全部</button>
                <button :class="['tab', { active: currentStatus === 0 }]" @click="changeStatus(0)">未发货</button>
                <button :class="['tab', { active: currentStatus === 1 }]" @click="changeStatus(1)">已发货</button>
                <button :class="['tab', { active: currentStatus === 2 }]" @click="changeStatus(2)">已揽收</button>
                <button :class="['tab', { active: currentStatus === 3 }]" @click="changeStatus(3)">运输中</button>
                <button :class="['tab', { active: currentStatus === 4 }]" @click="changeStatus(4)">已到达</button>
                <button :class="['tab', { active: currentStatus === 5 }]" @click="changeStatus(5)">已收货</button>
            </div>
        </div>

        <div class="toolbar">
            <span class="count">共 {{ orders.length }} 个订单</span>
        </div>

        <div v-if="orders.length === 0" class="empty-state"><p>暂无订单</p></div>

        <div class="order-list">
            <div v-for="order in orders" :key="order.orderId" class="order-item">
                <div class="order-header">
                    <span class="order-id">#{{ order.orderId }}</span>
                    <span class="order-time">{{ formatTime(order.orderTime) }}</span>
                    <span class="badge" :class="getStatusClass(order.status)">{{ getStatusText(order.status) }}</span>
                </div>
                <div class="order-body">
                    <div class="order-product">
                        <img :src="order.imageUrl || '/placeholder.png'" class="product-image" />
                        <div class="product-details">
                            <div class="product-name">{{ order.productName }}</div>
                            <div class="product-meta">
                                <span>¥{{ order.unitPrice }} × {{ order.quantity }}</span>
                                <span class="sep">·</span>
                                <span>合计 <strong>¥{{ order.totalAmount }}</strong></span>
                            </div>
                            <div class="order-people">
                                <span v-if="order.customerName">顾客: {{ order.customerName }}</span>
                                <span v-if="order.warehouseName" class="sep">·</span>
                                <span v-if="order.warehouseName">仓库: {{ order.warehouseName }}</span>
                            </div>
                        </div>
                    </div>
                    <div class="order-actions">
                        <select :value="order.status" @change="updateStatus(order, $event)" class="status-select">
                            <option :value="0">未发货</option>
                            <option :value="1">已发货</option>
                            <option :value="2">已揽收</option>
                            <option :value="3">运输中</option>
                            <option :value="4">已到达</option>
                            <option :value="5">已收货</option>
                        </select>
                        <button class="btn btn-danger btn-sm" @click="deleteOrder(order)">删除</button>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const orders = ref([])
const searchKeyword = ref('')
const currentStatus = ref('all')

const loadOrders = async () => {
    try {
        const params = {}
        if (currentStatus.value !== 'all') params.status = currentStatus.value
        if (searchKeyword.value) params.search = searchKeyword.value
        const res = await request.get('/admin/orders', { params })
        if (res.code === 200) orders.value = res.data || []
    } catch (e) { /* handled */ }
}

const changeStatus = (s) => { currentStatus.value = s; loadOrders() }

const getStatusText = (s) => ({ 0: '未发货', 1: '已发货', 2: '已揽收', 3: '运输中', 4: '已到达', 5: '已收货' }[s] || '未知')
const getStatusClass = (s) => {
    if (s <= 0) return 'status-default'
    if (s <= 3) return 'status-transit'
    if (s === 4) return 'status-arrived'
    return 'status-done'
}

const formatTime = (t) => {
    if (!t) return ''
    const d = new Date(t)
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const updateStatus = async (order, event) => {
    const newStatus = parseInt(event.target.value)
    try {
        await ElMessageBox.confirm(`确认将订单 #${order.orderId} 状态改为 "${getStatusText(newStatus)}" 吗？`, '确认', { confirmButtonText: '确认', cancelButtonText: '取消' })
    } catch { event.target.value = order.status; return }

    const res = await request.put(`/admin/orders/${order.orderId}/status`, null, { params: { status: newStatus } })
    if (res.code === 200) loadOrders()
    else { ElMessage.error(res.message || '更新失败'); event.target.value = order.status }
}

const deleteOrder = async (order) => {
    try {
        await ElMessageBox.confirm(`确认删除订单 #${order.orderId} 吗？此操作不可恢复。`, '确认', { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' })
    } catch { return }
    const res = await request.delete(`/admin/orders/${order.orderId}`)
    if (res.code === 200) { ElMessage.success('删除成功'); loadOrders() }
    else ElMessage.error(res.message || '删除失败')
}

onMounted(loadOrders)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.count { font-size: 13px; color: #536471; }

.order-list { display: flex; flex-direction: column; gap: 10px; }
.order-item { background: #fff; border: 1px solid #eff3f4; border-radius: 12px; overflow: hidden; transition: border-color 0.15s; }
.order-item:hover { border-color: #cfd9de; }

.order-header {
    display: flex; align-items: center; gap: 12px;
    padding: 10px 16px; background: #f7f9f9; border-bottom: 1px solid #eff3f4;
}
.order-id { font-size: 13px; font-weight: 700; color: #0f1419; }
.order-time { font-size: 12px; color: #8899a6; flex: 1; }

.status-default { background: #f7f9f9; color: #536471; border: 1px solid #eff3f4; }
.status-transit { background: #eff6ff; color: #1d4ed8; border: 1px solid #bfdbfe; }
.status-arrived { background: #fff7e6; color: #b45309; border: 1px solid #fde68a; }
.status-done { background: #f0fdf4; color: #15803d; border: 1px solid #bbf7d0; }

.order-body { display: flex; align-items: center; gap: 16px; padding: 14px 16px; }
.order-product { display: flex; gap: 12px; flex: 1; min-width: 0; }
.product-image { width: 64px; height: 64px; border-radius: 8px; object-fit: cover; background: #f7f9f9; flex-shrink: 0; }
.product-details { flex: 1; min-width: 0; }
.product-name { font-size: 14px; font-weight: 600; color: #0f1419; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.product-meta { font-size: 12px; color: #536471; margin-top: 3px; }
.product-meta strong { color: #0f1419; }
.sep { margin: 0 4px; color: #cfd9de; }
.order-people { font-size: 12px; color: #8899a6; margin-top: 3px; }

.order-actions { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.status-select {
    padding: 6px 10px; border: 1px solid #eff3f4; border-radius: 8px;
    font-size: 12px; color: #0f1419; background: #fff; cursor: pointer;
}
.status-select:focus { outline: none; border-color: #0f1419; }

@media (max-width: 640px) {
    .order-body { flex-direction: column; align-items: stretch; }
    .order-actions { justify-content: flex-end; }
}
</style>
