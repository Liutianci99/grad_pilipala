<template>
    <div class="page-container">
        <!-- 筛选栏 -->
        <div class="filter-bar">
            <div class="time-filter">
                <label>时间范围：</label>
                <a-range-picker
                    v-model:value="dateRange"
                    @change="handleFilterChange"
                    :placeholder="['开始时间', '结束时间']"
                    format="YYYY-MM-DD"
                    :allow-clear="true"
                    style="width: 280px;"
                />
                <button @click="resetFilter" class="reset-btn">重置</button>
            </div>
            <div class="batch-count">
                <span>已完成批次：{{ filteredBatches.length }}</span>
            </div>
        </div>

        <!-- 批次列表 -->
        <div class="batch-list">
            <div v-if="filteredBatches.length === 0" class="empty-state">
                <p>暂无已完成的批次</p>
            </div>

            <div v-else v-for="(batch, index) in filteredBatches" :key="index" class="batch-item">
                <!-- 批次标题栏 -->
                <div class="batch-header" @click="toggleBatch(index)">
                    <div class="header-left">
                        <span class="toggle-arrow" :class="{ expanded: expandedBatches[index] }">
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                                <path d="M7 10l5 5 5-5" stroke="#222" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                            </svg>
                        </span>
                        <span class="batch-label">运输批次 {{ index + 1 }}</span>
                        <span class="batch-time">开始时间：{{ formatTime(batch.startedAt) }}</span>
                        <span class="batch-time">完成时间：{{ formatTime(batch.completedAt) }}</span>
                    </div>
                    <div class="header-right">
                        <span class="order-count">订单数：{{ batch.orders.length }}</span>
                        <span class="status-badge completed">已完成</span>
                    </div>
                </div>

                <!-- 批次内容（折叠/展开） -->
                <transition name="expand">
                    <div v-if="expandedBatches[index]" class="batch-content">
                        <!-- 订单列表 -->
                        <div class="orders-container">
                            <div class="order-item" v-for="order in batch.orders" :key="order.orderId">
                                <div class="order-header">
                                    <span class="order-id">订单号：{{ order.orderId }}</span>
                                    <span class="customer-info">顾客：{{ order.customerName }}</span>
                                </div>

                                <div class="order-content">
                                    <div class="product-image">
                                        <img :src="order.imageUrl || '/placeholder.png'" :alt="order.productName" />
                                    </div>
                                    <div class="product-info">
                                        <h4 class="product-name">{{ order.productName }}</h4>
                                        <div class="product-detail">
                                            <span>数量：{{ order.quantity }}</span>
                                            <span>单价：¥{{ order.unitPrice }}</span>
                                            <span>总价：¥{{ order.totalAmount }}</span>
                                        </div>
                                        <div class="address-info">
                                            <span class="label">收货地址：</span>
                                            <span v-if="order.address">{{ order.address.province }} {{ order.address.city }} {{ order.address.district }} {{ order.address.detailAddress }}</span>
                                            <span v-else class="no-address">暂无地址信息</span>
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
import { RangePicker } from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import request from '@/utils/request'
import dayjs from 'dayjs'

// 注册组件
const ARangePicker = RangePicker

// 状态数据
const dateRange = ref(null)
const batches = ref([])
const expandedBatches = ref({})

// 计算属性：根据日期范围过滤批次
const filteredBatches = computed(() => {
    if (!dateRange.value || dateRange.value.length !== 2) {
        return batches.value
    }

    const [startDate, endDate] = dateRange.value
    if (!startDate || !endDate) {
        return batches.value
    }

    return batches.value.filter(batch => {
        if (!batch.completedAt) return false

        const completedDate = dayjs(batch.completedAt)
        const start = dayjs(startDate).startOf('day')
        const end = dayjs(endDate).endOf('day')

        return completedDate.isAfter(start) && completedDate.isBefore(end)
    })
})

// 获取当前用户的配送员ID
const getCurrentDeliveryPersonnelId = () => {
    const userInfoStr = sessionStorage.getItem('userInfo')
    if (userInfoStr) {
        const userInfo = JSON.parse(userInfoStr)
        console.log('用户信息:', userInfo)
        if (userInfo.role === 'driver' && userInfo.id) {
            return userInfo.id
        }
    }
    return null
}

// 获取已完成的运输批次（不传递时间参数，获取所有数据）
const fetchCompletedBatches = async () => {
    try {
        const deliveryPersonnelId = getCurrentDeliveryPersonnelId()
        if (!deliveryPersonnelId) {
            console.error('无法获取配送员ID')
            return
        }

        console.log('配送员ID:', deliveryPersonnelId)
        const params = `?deliveryPersonnelId=${deliveryPersonnelId}`
        console.log('请求URL:', `/orders/completed-batches-with-status${params}`)

        const response = await request.get(`/orders/completed-batches-with-status${params}`)
        console.log('API响应:', response)

        if (response.code === 200 && response.data) {
            // 保存所有批次数据，前端进行过滤
            batches.value = response.data
            console.log('批次数据:', batches.value)
            expandedBatches.value = {}
        } else {
            console.error('获取历史任务失败:', response.message)
        }
    } catch (error) {
        console.error('获取历史任务失败:', error)
    }
}

// 切换批次的展开/折叠状态
const toggleBatch = (index) => {
    expandedBatches.value[index] = !expandedBatches.value[index]
}

// 筛选条件变化（前端过滤，不需要重新请求）
const handleFilterChange = () => {
    console.log('日期范围变化:', dateRange.value)
    // 不需要做任何事，computed 属性会自动更新
}

// 重置筛选
const resetFilter = () => {
    dateRange.value = null
}

// 格式化时间（只显示到日）
const formatTime = (time) => {
    if (!time) return '未知'
    const date = new Date(time)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
}

// 页面加载
onMounted(() => {
    fetchCompletedBatches()
})
</script>

<style scoped>
.page-container {
    width: 100%;
    max-width: 1200px;
    margin: 0 auto;
    padding: 20px;
}

/* 筛选栏 */
.filter-bar {
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    padding: 20px;
    margin-bottom: 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.time-filter {
    display: flex;
    align-items: center;
    gap: 12px;
}

.time-filter label {
    font-size: 14px;
    color: #374151;
    font-weight: 500;
}

.date-input {
    padding: 8px 12px;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    font-size: 14px;
    cursor: pointer;
}

.reset-btn {
    padding: 8px 16px;
    background-color: #ffffff;
    border: 1px solid #e5e7eb;
    color: #374151;
    border-radius: 6px;
    cursor: pointer;
    font-size: 14px;
    transition: all 0.2s;
}

.reset-btn:hover {
    background-color: #f9fafb;
    border-color: #d1d5db;
}

.batch-count {
    font-size: 14px;
    color: #666;
    font-weight: 500;
}

/* 批次列表 */
.batch-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.empty-state {
    text-align: center;
    padding: 60px 20px;
    color: #999;
    font-size: 16px;
    background: #fafafa;
    border-radius: 8px;
}

/* 批次项 */
.batch-item {
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

/* 批次标题栏 */
.batch-header {
    background: #fff;
    color: #222;
    padding: 16px 20px;
    cursor: pointer;
    display: flex;
    justify-content: space-between;
    align-items: center;
    transition: all 0.3s ease;
}

.batch-header:hover {
    background: #f5f6fa;
    box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.header-left {
    display: flex;
    align-items: center;
    gap: 12px;
}

.toggle-arrow {
    display: inline-block;
    transition: transform 0.3s ease;
    vertical-align: middle;
}

.toggle-arrow.expanded {
    transform: rotate(180deg);
}

.batch-label {
    font-size: 16px;
    font-weight: 600;
}

.batch-time {
    font-size: 12px;
    opacity: 0.9;
}

.header-right {
    display: flex;
    align-items: center;
    gap: 16px;
}

.order-count {
    font-size: 14px;
}

.status-badge {
    padding: 4px 12px;
    border-radius: 20px;
    font-size: 12px;
}

.status-badge.completed {
    background: #d1fae5;
    color: #065f46;
}

/* 批次内容 */
.batch-content {
    padding: 20px;
    background: #fff;
}

.orders-container {
    display: flex;
    flex-direction: column;
    gap: 16px;
}

/* 订单项 */
.order-item {
    background: white;
    border: 1px solid #e5e7eb;
    border-radius: 6px;
    overflow: hidden;
}

.order-header {
    background: #f3f4f6;
    padding: 12px 16px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 14px;
    font-weight: 500;
}

.order-id {
    color: #333;
}

.customer-info {
    color: #666;
}

.order-content {
    display: flex;
    gap: 16px;
    padding: 16px;
}

.product-image {
    flex-shrink: 0;
    width: 80px;
    height: 80px;
    border-radius: 4px;
    overflow: hidden;
    background: #f0f0f0;
}

.product-image img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.product-info {
    flex: 1;
}

.product-name {
    margin: 0 0 8px 0;
    font-size: 14px;
    font-weight: 600;
    color: #333;
}

.product-detail {
    display: flex;
    gap: 16px;
    font-size: 12px;
    color: #666;
    margin-bottom: 8px;
}

.address-info {
    display: flex;
    gap: 8px;
    font-size: 12px;
    color: #666;
    padding: 8px 0;
    border-top: 1px solid #eee;
}

.address-info .label {
    font-weight: 500;
}

.no-address {
    color: #999;
    font-style: italic;
}

/* 展开/折叠动画 */
.expand-enter-active,
.expand-leave-active {
    transition: all 0.3s ease;
}

.expand-enter-from,
.expand-leave-to {
    opacity: 0;
    max-height: 0;
}

.expand-enter-to,
.expand-leave-from {
    opacity: 1;
    max-height: 1000px;
}

/* 响应式设计 */
@media (max-width: 768px) {
    .filter-bar {
        flex-direction: column;
        align-items: flex-start;
        gap: 16px;
    }

    .time-filter {
        flex-wrap: wrap;
    }

    .batch-header {
        flex-direction: column;
        align-items: flex-start;
        gap: 12px;
    }

    .header-right {
        width: 100%;
        justify-content: space-between;
    }

    .order-content {
        flex-direction: column;
    }

    .product-image {
        width: 100%;
        height: 150px;
    }
}
</style>
