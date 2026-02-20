<template>
    <div class="page-container">
        <h1>历史任务</h1>

        <div class="filter-bar">
            <div class="search-box">
                <input type="date" v-model="startDate" class="search-input" style="border-radius: 8px;" />
                <span style="color: #8899a6; font-size: 13px;">至</span>
                <input type="date" v-model="endDate" class="search-input" style="border-radius: 8px;" />
                <button class="btn btn-ghost" @click="resetFilter">重置</button>
            </div>
            <span style="font-size: 13px; color: #536471;">已完成：{{ filteredBatches.length }}</span>
        </div>

        <div v-if="filteredBatches.length === 0" class="empty-state">
            <p>暂无已完成的批次</p>
        </div>

        <div v-else style="display: flex; flex-direction: column; gap: 16px;">
            <div v-for="(batch, index) in filteredBatches" :key="index" class="batch-item">
                <div class="batch-header" @click="toggleBatch(index)">
                    <div style="display: flex; align-items: center; gap: 12px;">
                        <span class="toggle-arrow" :class="{ expanded: expandedBatches[index] }">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                                <path d="M7 10l5 5 5-5" stroke="#536471" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                            </svg>
                        </span>
                        <span class="batch-label">批次 {{ index + 1 }}</span>
                        <span class="batch-time">{{ formatTime(batch.startedAt) }} → {{ formatTime(batch.completedAt) }}</span>
                    </div>
                    <div style="display: flex; align-items: center; gap: 12px;">
                        <span class="order-count">{{ batch.orders.length }} 单</span>
                        <span class="status-badge status-completed">已完成</span>
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
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </transition>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import request from '@/utils/request'

const startDate = ref('')
const endDate = ref('')
const batches = ref([])
const expandedBatches = ref({})

const filteredBatches = computed(() => {
    if (!startDate.value && !endDate.value) return batches.value
    return batches.value.filter(batch => {
        if (!batch.completedAt) return false
        const completed = new Date(batch.completedAt)
        if (startDate.value && completed < new Date(startDate.value)) return false
        if (endDate.value && completed > new Date(endDate.value + 'T23:59:59')) return false
        return true
    })
})

const getCurrentDeliveryPersonnelId = () => {
    const userInfoStr = sessionStorage.getItem('userInfo')
    if (userInfoStr) {
        const userInfo = JSON.parse(userInfoStr)
        if (userInfo.role === 'driver' && userInfo.id) return userInfo.id
    }
    return null
}

const fetchCompletedBatches = async () => {
    const deliveryPersonnelId = getCurrentDeliveryPersonnelId()
    if (!deliveryPersonnelId) return

    const response = await request.get(`/orders/completed-batches-with-status?deliveryPersonnelId=${deliveryPersonnelId}`)
    if (response.code === 200 && response.data) {
        batches.value = response.data
        expandedBatches.value = {}
    }
}

const toggleBatch = (index) => {
    expandedBatches.value[index] = !expandedBatches.value[index]
}

const resetFilter = () => {
    startDate.value = ''
    endDate.value = ''
}

const formatTime = (time) => {
    if (!time) return '未知'
    const d = new Date(time)
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

onMounted(() => {
    fetchCompletedBatches()
})
</script>

<style scoped>
/* All shared classes come from design.css */
</style>
