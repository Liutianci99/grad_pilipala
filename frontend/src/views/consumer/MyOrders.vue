<template>
    <div class="page-container">
        <div class="filter-bar">
            <div class="search-box">
                <input 
                    type="text" 
                    v-model="searchKeyword" 
                    placeholder="搜索商品名称..."
                    class="search-input"
                    @keyup.enter="handleSearch"
                />
                <button @click="handleSearch" class="btn btn-ghost">搜索</button>
            </div>
            <div class="tabs">
                <button :class="['tab', { active: currentStatus === 'all' }]" @click="changeStatus('all')">全部</button>
                <button :class="['tab', { active: currentStatus === 'pending' }]" @click="changeStatus('pending')">待发货</button>
                <button :class="['tab', { active: currentStatus === 'shipping' }]" @click="changeStatus('shipping')">待收货</button>
                <button :class="['tab', { active: currentStatus === 'completed' }]" @click="changeStatus('completed')">已完成</button>
            </div>
        </div>

        <div class="order-list">
            <div v-if="orders.length === 0" class="empty-state">暂无订单</div>

            <div class="order-card" v-for="order in orders" :key="order.orderId">
                <div class="order-header">
                    <span class="order-time">{{ formatTime(order.orderTime) }}</span>
                    <span class="order-id">#{{ order.orderId }}</span>
                    <span class="badge">{{ getStatusText(order.status) }}</span>
                </div>
                <div class="order-body">
                    <div class="order-image">
                        <img :src="order.imageUrl || '/placeholder.png'" :alt="order.productName" />
                    </div>
                    <div class="order-info">
                        <h3 class="order-name">{{ order.productName }}</h3>
                        <p class="order-meta">¥{{ order.unitPrice }} × {{ order.quantity }}</p>
                    </div>
                    <div class="order-total">
                        <span class="total-label">合计</span>
                        <span class="total-value">¥{{ order.totalAmount }}</span>
                    </div>
                    <div class="order-actions">
                        <button v-if="order.status === 2" class="btn btn-primary btn-sm" @click="confirmReceipt(order.orderId)">确认收货</button>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const searchKeyword = ref('')
const activeSearchKeyword = ref('')
const currentStatus = ref('all')
const orders = ref([])

const getOrders = async () => {
    try {
        const userInfo = sessionStorage.getItem('userInfo')
        if (!userInfo) return
        const user = JSON.parse(userInfo)
        const params = { customerId: user.id }
        if (currentStatus.value !== 'all') params.status = getStatusValue(currentStatus.value)
        if (activeSearchKeyword.value) params.search = activeSearchKeyword.value
        const res = await request.get('/orders/my', { params })
        if (res.code === 200) orders.value = res.data || []
    } catch (error) {
        console.error('获取订单列表失败:', error)
    }
}

const changeStatus = (status) => { currentStatus.value = status; getOrders() }
const handleSearch = () => { activeSearchKeyword.value = searchKeyword.value; getOrders() }

const confirmReceipt = async (orderId) => {
    if (!confirm('确认收到商品了吗？')) return
    try {
        const user = JSON.parse(sessionStorage.getItem('userInfo'))
        const res = await request.put(`/orders/${orderId}/confirm`, null, { params: { customerId: user.id } })
        if (res.code === 200) getOrders()
    } catch (error) {
        console.error('确认收货失败:', error)
    }
}

const formatTime = (time) => time ? new Date(time).toLocaleString('zh-CN') : ''

const getStatusText = (status) => {
    const m = { '-1': '已取消', 0: '未发货', 1: '已发货', 2: '已揽收', 3: '运输中', 4: '已到达', 5: '已收货' }
    return m[status] || '未知'
}

const getStatusValue = (status) => ({ pending: 0, shipping: 2, completed: 5 })[status]

onMounted(() => { getOrders() })
</script>

<style scoped>
.filter-bar {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.order-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
    margin-top: 16px;
}

.order-card {
    background: #ffffff;
    border: 1px solid #e5e5e5;
    border-radius: 10px;
    overflow: hidden;
    transition: border-color 0.15s;
}
.order-card:hover { border-color: #d4d4d4; }

.order-header {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 16px;
    border-bottom: 1px solid #f5f5f5;
}
.order-time { font-size: 12px; color: #a3a3a3; }
.order-id { font-size: 12px; color: #a3a3a3; margin-left: auto; }

.order-body {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 14px 16px;
}

.order-image {
    width: 64px;
    height: 64px;
    flex-shrink: 0;
    border-radius: 8px;
    overflow: hidden;
    background: #f5f5f5;
}
.order-image img { width: 100%; height: 100%; object-fit: cover; }

.order-info { flex: 1; min-width: 0; }
.order-name {
    margin: 0 0 4px 0;
    font-size: 14px;
    font-weight: 600;
    color: #0a0a0a;
}
.order-meta {
    margin: 0;
    font-size: 13px;
    color: #737373;
}

.order-total {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    min-width: 80px;
}
.total-label { font-size: 11px; color: #a3a3a3; }
.total-value { font-size: 15px; font-weight: 600; color: #0a0a0a; }

.order-actions { min-width: 80px; display: flex; justify-content: flex-end; }

@media (max-width: 640px) {
    .order-body { flex-wrap: wrap; }
    .order-total { align-items: flex-start; }
}
</style>
