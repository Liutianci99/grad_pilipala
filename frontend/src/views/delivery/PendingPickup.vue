<template>
    <div class="page-container">
        <h1>待揽收</h1>

        <div v-if="orders.length === 0" class="empty-state">
            <p>暂无待揽收订单</p>
        </div>

        <div v-else style="display: flex; flex-direction: column; gap: 12px;">
            <div class="order-item" v-for="order in orders" :key="order.orderId">
                <div class="order-header">
                    <span class="order-id">#{{ order.orderId }}</span>
                    <span class="order-time">{{ formatTime(order.shipTime) }}</span>
                    <span class="status-badge status-pending">待揽收</span>
                </div>
                <div style="display: flex; gap: 16px; padding: 16px;">
                    <div class="product-image" style="width: 80px; height: 80px;">
                        <img :src="order.imageUrl || '/placeholder.png'" :alt="order.productName" />
                    </div>
                    <div style="flex: 1;">
                        <h4 class="product-name">{{ order.productName }}</h4>
                        <div class="product-detail">
                            <span>数量：{{ order.quantity }}</span>
                            <span>仓库：{{ order.warehouseName || '未分配' }}</span>
                        </div>
                    </div>
                    <div style="display: flex; align-items: center;">
                        <button class="btn btn-ghost btn-sm" @click="confirmPickup(order.orderId)">确认揽收</button>
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

const getCurrentUserId = () => {
    const userInfoStr = sessionStorage.getItem('userInfo')
    if (userInfoStr) {
        const userInfo = JSON.parse(userInfoStr)
        if (userInfo.role === 'driver' && userInfo.id) return userInfo.id
    }
    return null
}

const fetchOrders = async () => {
    const userId = getCurrentUserId()
    if (!userId) {
        ElMessage.error('未获取到配送员信息，请重新登录')
        return
    }
    const res = await request.get('/orders/pending-pickup', { params: { driverId: userId } })
    if (res.success) {
        orders.value = res.data || []
    } else {
        ElMessage.error(res.message || '获取订单列表失败')
    }
}

const confirmPickup = async (orderId) => {
    try {
        await ElMessageBox.confirm('确认揽收此订单？', '确认', { confirmButtonText: '揽收', cancelButtonText: '取消' })
    } catch { return }

    const res = await request.put(`/orders/${orderId}/pickup`)
    if (res.success) {
        ElMessage.success('揽收成功')
        fetchOrders()
    } else {
        ElMessage.error(res.message || '揽收失败')
    }
}

const formatTime = (time) => {
    if (!time) return ''
    return new Date(time).toLocaleString('zh-CN')
}

onMounted(() => {
    fetchOrders()
})
</script>

<style scoped>
/* All shared classes come from design.css */
</style>
