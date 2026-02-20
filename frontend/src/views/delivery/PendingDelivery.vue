<template>
    <div class="page-container">
        <h1>待送货</h1>

        <div class="filter-bar">
            <div style="display: flex; align-items: center; gap: 16px;">
                <button class="btn btn-primary" @click="createBatch" :disabled="selectedOrders.length === 0">
                    创建送货批次
                </button>
                <span style="font-size: 13px; color: #536471;">已选择: {{ selectedOrders.length }}/5</span>
            </div>
        </div>

        <div v-if="orders.length === 0" class="empty-state">
            <p>暂无待送货订单</p>
        </div>

        <div v-else style="display: flex; flex-direction: column; gap: 12px;">
            <div class="order-item" v-for="order in orders" :key="order.orderId">
                <div class="order-header">
                    <input
                        type="checkbox"
                        :value="order.orderId"
                        v-model="selectedOrders"
                        :disabled="!selectedOrders.includes(order.orderId) && selectedOrders.length >= 5"
                        class="order-checkbox"
                    />
                    <span class="order-id">#{{ order.orderId }}</span>
                    <span class="order-time">{{ formatTime(order.pickupTime) }}</span>
                    <span class="status-badge status-pending">待送货</span>
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
const selectedOrders = ref([])

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
    const res = await request.get('/orders/pending-delivery', { params: { deliveryPersonnelId: userId } })
    if (res.success) {
        orders.value = res.data || []
    } else {
        ElMessage.error(res.message || '获取订单列表失败')
    }
}

const createBatch = async () => {
    if (selectedOrders.value.length === 0) {
        ElMessage.warning('请至少选择一个订单')
        return
    }
    const userId = getCurrentUserId()
    if (!userId) {
        ElMessage.error('未获取到配送员信息，请重新登录')
        return
    }

    try {
        await ElMessageBox.confirm(
            `确认创建送货批次？共 ${selectedOrders.value.length} 个订单`,
            '确认', { confirmButtonText: '创建', cancelButtonText: '取消' }
        )
    } catch { return }

    const res = await request.post(`/orders/delivery-batch?deliveryPersonnelId=${userId}`, selectedOrders.value)
    if (res.success) {
        ElMessage.success('创建送货批次成功')
        selectedOrders.value = []
        fetchOrders()
    } else {
        ElMessage.error(res.message || '创建送货批次失败')
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
