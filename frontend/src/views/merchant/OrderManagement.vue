<template>
    <div class="page-container">
        <h1>订单管理</h1>

        <div class="filter-bar">
            <div class="search-box">
                <input type="text" v-model="searchKeyword" placeholder="搜索商品名称..." class="search-input" @keyup.enter="handleSearch" />
                <button class="btn btn-ghost" @click="handleSearch">搜索</button>
            </div>
            <div class="tabs">
                <button :class="['tab', { active: currentStatus === 'all' }]" @click="changeStatus('all')">全部</button>
                <button :class="['tab', { active: currentStatus === 'pending' }]" @click="changeStatus('pending')">待发货</button>
                <button :class="['tab', { active: currentStatus === 'shipped' }]" @click="changeStatus('shipped')">已发货</button>
                <button :class="['tab', { active: currentStatus === 'completed' }]" @click="changeStatus('completed')">已完成</button>
            </div>
        </div>

        <div v-if="orders.length === 0" class="empty-state">
            <p>暂无订单数据</p>
        </div>

        <div v-else style="display: flex; flex-direction: column; gap: 12px;">
            <div class="order-item" v-for="order in orders" :key="order.orderId">
                <div class="order-header">
                    <span class="order-id">#{{ order.orderId }}</span>
                    <span class="order-time">{{ formatTime(order.orderTime) }}</span>
                    <span class="status-badge" :class="getStatusBadgeClass(order.status)">{{ getStatusText(order.status) }}</span>
                </div>
                <div style="display: flex; gap: 16px; padding: 16px; align-items: center;">
                    <div class="product-image" style="width: 80px; height: 80px;">
                        <img :src="order.imageUrl || '/placeholder.png'" :alt="order.productName" />
                    </div>
                    <div style="flex: 1;">
                        <h4 class="product-name">{{ order.productName }}</h4>
                        <div class="product-detail">
                            <span>¥{{ order.unitPrice }} × {{ order.quantity }}</span>
                        </div>
                    </div>
                    <div style="text-align: right;">
                        <span class="price">¥{{ order.totalAmount }}</span>
                    </div>
                    <div v-if="order.status === 0">
                        <button class="btn btn-primary btn-sm" @click="shipOrder(order.orderId)">发货</button>
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

const searchKeyword = ref('')
const activeSearchKeyword = ref('')
const currentStatus = ref('all')
const orders = ref([])

const getUserId = () => {
    const userInfo = sessionStorage.getItem('userInfo')
    if (!userInfo) return null
    return JSON.parse(userInfo).id
}

const fetchOrders = async () => {
    const userId = getUserId()
    if (!userId) { ElMessage.error('请先登录'); return }

    const params = { merchantId: userId }
    if (currentStatus.value !== 'all') params.status = getStatusValue(currentStatus.value)
    if (activeSearchKeyword.value) params.search = activeSearchKeyword.value

    const res = await request.get('/orders/merchant', { params })
    if (res.code === 200) {
        orders.value = res.data || []
    } else {
        ElMessage.error(res.message || '获取订单列表失败')
    }
}

const changeStatus = (status) => {
    currentStatus.value = status
    fetchOrders()
}

const handleSearch = () => {
    activeSearchKeyword.value = searchKeyword.value
    fetchOrders()
}

const shipOrder = async (orderId) => {
    try {
        await ElMessageBox.confirm('确认要发货吗？', '确认', { confirmButtonText: '发货', cancelButtonText: '取消' })
    } catch { return }

    const userId = getUserId()
    if (!userId) { ElMessage.error('请先登录'); return }

    const res = await request.put(`/orders/${orderId}/ship`, null, { params: { merchantId: userId } })
    if (res.code === 200) {
        ElMessage.success('发货成功')
        fetchOrders()
    } else {
        ElMessage.error(res.message || '发货失败')
    }
}

const formatTime = (time) => {
    if (!time) return ''
    return new Date(time).toLocaleString('zh-CN')
}

const getStatusText = (status) => {
    return { 0: '未发货', 1: '已发货', 2: '已揽收', 3: '运输中', 4: '已到达', 5: '已收货' }[status] || '未知'
}

const getStatusBadgeClass = (status) => {
    if (status === 0) return 'status-pending'
    if (status >= 1 && status <= 3) return 'status-delivering'
    if (status >= 4) return 'status-completed'
    return ''
}

const getStatusValue = (status) => {
    return { pending: 0, shipped: 1, completed: 5 }[status]
}

onMounted(() => { fetchOrders() })
</script>

<style scoped>
/* All shared classes come from design.css */
</style>
