<template>
    <div class="page-container">
        <h1>运输批次</h1>

        <div class="filter-bar">
            <div class="batch-count" style="font-size: 13px; color: #536471;">
                运输中的批次：{{ batches.length }}
            </div>
        </div>

        <div v-if="batches.length === 0" class="empty-state">
            <p>暂无运输中的批次</p>
        </div>

        <div v-else style="display: flex; flex-direction: column; gap: 16px;">
            <div v-for="(batch, index) in batches" :key="index" class="batch-item">
                <div class="batch-header" @click="toggleBatch(index)">
                    <div style="display: flex; align-items: center; gap: 12px;">
                        <span class="toggle-arrow" :class="{ expanded: expandedBatches[index] }">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                                <path d="M7 10l5 5 5-5" stroke="#536471" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                            </svg>
                        </span>
                        <span class="batch-label">运输批次 {{ index + 1 }}</span>
                        <span class="batch-time">{{ formatTime(batch.createdAt) }}</span>
                    </div>
                    <div style="display: flex; align-items: center; gap: 12px;">
                        <span class="order-count">{{ batch.orders.length }} 单</span>
                        <span class="status-badge" :class="getStatusClass(batch.status)">{{ getStatusText(batch.status) }}</span>
                    </div>
                </div>

                <transition name="expand">
                    <div v-if="expandedBatches[index]" class="batch-content">
                        <div class="orders-container">
                            <div class="order-item" v-for="order in batch.orders" :key="order.orderId">
                                <div class="order-header">
                                    <span class="order-id">#{{ order.orderId }}</span>
                                    <span class="order-time">{{ order.customerName }}</span>
                                </div>
                                <div style="display: flex; gap: 16px; padding: 16px;">
                                    <div class="product-image" style="width: 80px; height: 80px;">
                                        <img :src="order.imageUrl || '/placeholder.png'" :alt="order.productName" />
                                    </div>
                                    <div style="flex: 1;">
                                        <h4 class="product-name">{{ order.productName }}</h4>
                                        <div class="product-detail">
                                            <span>数量：{{ order.quantity }}</span>
                                            <span>¥{{ order.unitPrice }}</span>
                                            <span class="price">¥{{ order.totalAmount }}</span>
                                        </div>
                                        <div class="address-info" v-if="order.address">
                                            <span class="label">收货：</span>
                                            <span>{{ order.address.province }} {{ order.address.city }} {{ order.address.district }} {{ order.address.detailAddress }}</span>
                                        </div>
                                        <div v-else class="address-info">
                                            <span class="no-address">暂无地址信息</span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div class="batch-actions">
                            <button v-if="batch.status === 0" class="btn btn-primary" @click="startDelivery(index)">
                                开始运输
                            </button>
                            <button v-if="batch.status === 1" class="btn btn-ghost" @click="viewBatchDetail(index)">
                                查看详情
                            </button>
                        </div>
                    </div>
                </transition>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

const batches = ref([])
const expandedBatches = ref({})

const getCurrentDeliveryPersonnelId = () => {
    const userInfoStr = sessionStorage.getItem('userInfo')
    if (userInfoStr) {
        const userInfo = JSON.parse(userInfoStr)
        if (userInfo.role === 'driver' && userInfo.id) {
            return userInfo.id
        }
    }
    return null
}

const fetchDeliveryBatches = async () => {
    const deliveryPersonnelId = getCurrentDeliveryPersonnelId()
    if (!deliveryPersonnelId) return

    const response = await request.get(`/orders/delivery-batches-with-status?deliveryPersonnelId=${deliveryPersonnelId}`)
    if (response.code === 200 && response.data) {
        batches.value = response.data
        expandedBatches.value = {}
    }
}

const toggleBatch = (index) => {
    expandedBatches.value[index] = !expandedBatches.value[index]
}

const viewBatchDetail = (index) => {
    const batch = batches.value[index]
    if (!batch.batchId) {
        ElMessage.error('批次数据错误')
        return
    }

    let deliveryTime = batch.startedAt || batch.createdAt
    if (!deliveryTime) {
        ElMessage.error('批次缺少时间信息')
        return
    }

    if (typeof deliveryTime === 'string') {
        deliveryTime = deliveryTime.replace(/\.\d+/, '').replace(/Z$/, '')
    }

    router.push({
        path: `/driver/delivery-batch-detail/${batch.batchId}`,
        query: { batchId: batch.batchId, deliveryTime },
        state: { batchData: JSON.parse(JSON.stringify(batch)) }
    })
}

const startDelivery = async (index) => {
    const batch = batches.value[index]
    try {
        await ElMessageBox.confirm(
            `确定开始运输批次 ${index + 1} 吗？该批次包含 ${batch.orders.length} 个订单`,
            '确认', { confirmButtonText: '开始', cancelButtonText: '取消' }
        )
    } catch { return }

    const response = await request.post(`/delivery-batch/start-batch?batchId=${batch.batchId}`)
    if (response.code === 200) {
        ElMessage.success('开始运输成功')
        await fetchDeliveryBatches()
    } else {
        ElMessage.error(response.message || '开始运输失败')
    }
}

const formatTime = (time) => {
    if (!time) return '未知'
    const date = new Date(time)
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

const getStatusText = (status) => {
    return { 0: '待出发', 1: '配送中', 2: '已完成' }[status] || '未知'
}

const getStatusClass = (status) => {
    return { 0: 'status-pending', 1: 'status-delivering', 2: 'status-completed' }[status] || ''
}

onMounted(() => {
    fetchDeliveryBatches()
})
</script>

<style scoped>
/* All shared classes come from design.css — only page-specific overrides here */
</style>
