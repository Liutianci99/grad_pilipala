<template>
    <div class="page-container">
        <!-- 仅显示运输中的批次数量 -->
        <div class="filter-bar">
            <div class="batch-count">
                <span>运输中的批次：{{ batches.length }}</span>
            </div>
        </div>

        <!-- 批次列表 -->
        <div class="batch-list">
            <div v-if="batches.length === 0" class="empty-state">
                <p>暂无运输中的批次</p>
            </div>
            
            <div v-else v-for="(batch, index) in batches" :key="index" class="batch-item">
                <!-- 批次标题栏 -->
                <div class="batch-header" @click="toggleBatch(index)">
                    <div class="header-left">
                                                <span class="toggle-arrow" :class="{ expanded: expandedBatches[index] }">
                                                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                                                        <path d="M7 10l5 5 5-5" stroke="#222" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                                                    </svg>
                                                </span>
                        <span class="batch-label">运输批次 {{ index + 1 }}</span>
                        <span class="batch-time">创建时间：{{ formatTime(batch.createdAt) }}</span>
                    </div>
                    <div class="header-right">
                        <span class="order-count">订单数：{{ batch.orders.length }}</span>
                        <span class="status-badge" :class="getStatusClass(batch.status)">{{ getStatusText(batch.status) }}</span>
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

                        <!-- 批次操作栏 -->
                        <div class="batch-actions">
                            <!-- 待运输状态(status=0)：只显示开始运输按钮 -->
                            <button v-if="batch.status === 0" class="start-btn" @click="startDelivery(index)">
                                开始运输
                            </button>
                            <!-- 运输中状态(status=1)：只显示查看详情按钮 -->
                            <button v-if="batch.status === 1" class="detail-btn" @click="viewBatchDetail(index)">
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

const router = useRouter()

// 状态数据
const selectedWarehouse = ref('')
const warehouses = ref([])
const batches = ref([])
const expandedBatches = ref({})
const currentUserWarehouseId = ref(null)

// 获取当前用户的仓库ID
const getCurrentUserWarehouse = () => {
    const userInfoStr = sessionStorage.getItem('userInfo')
    if (userInfoStr) {
        const userInfo = JSON.parse(userInfoStr)
        if (userInfo.role === 'driver' && userInfo.warehouseId) {
            currentUserWarehouseId.value = userInfo.warehouseId
            selectedWarehouse.value = userInfo.warehouseId
        }
    }
}

// 获取仓库列表
const fetchWarehouses = async () => {
    try {
        const response = await request.get('/warehouse/list')
        if (response.success) {
            warehouses.value = response.data
        } else {
            console.error('获取仓库列表失败:', response.message)
        }
    } catch (error) {
        console.error('获取仓库列表失败:', error)
    }
}

// 获取运输中的批次（status=3的订单）
const fetchDeliveryBatches = async () => {
    try {
        const deliveryPersonnelId = getCurrentDeliveryPersonnelId()
        if (!deliveryPersonnelId) {
            console.error('无法获取配送员ID')
            return
        }

        console.log('配送员ID:', deliveryPersonnelId)
        const params = `?deliveryPersonnelId=${deliveryPersonnelId}`
        console.log('请求URL:', `/orders/delivery-batches-with-status${params}`)

        const response = await request.get(`/orders/delivery-batches-with-status${params}`)
        console.log('API响应:', response)

        if (response.code === 200 && response.data) {
            // 直接使用返回的批次数据
            batches.value = response.data
            console.log('批次数据:', batches.value)
            expandedBatches.value = {}
        } else {
            console.error('获取运输批次失败:', response.message)
        }
    } catch (error) {
        console.error('获取运输批次失败:', error)
    }
}

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

// 切换批次的展开/折叠状态
const toggleBatch = (index) => {
    expandedBatches.value[index] = !expandedBatches.value[index]
}

// 查看批次详情
const viewBatchDetail = (index) => {
    const batch = batches.value[index]

    // Debug: 打印批次数据
    console.log('=== 查看批次详情 ===')
    console.log('批次索引:', index)
    console.log('批次数据:', batch)
    console.log('批次ID:', batch.batchId)
    console.log('批次订单数:', batch.orders?.length)

    if (!batch.batchId) {
        alert('批次数据错误：缺少 batchId 字段')
        return
    }

    // 获取deliveryTime（用于查询路线信息）
    // 优先使用 startedAt（批次开始时间），如果没有则使用 createdAt（批次创建时间）
    let deliveryTime = batch.startedAt || batch.createdAt

    if (!deliveryTime) {
        alert('批次数据错误：缺少时间信息')
        return
    }

    // 确保时间格式为 ISO 8601 格式（YYYY-MM-DDTHH:mm:ss）
    // 如果是 Date 对象，转换为 ISO 字符串
    if (deliveryTime instanceof Date) {
        deliveryTime = deliveryTime.toISOString()
    } else if (typeof deliveryTime === 'string') {
        // 如果已经是字符串，确保格式正确
        // 移除可能的毫秒和时区信息，保持 ISO_DATE_TIME 格式
        deliveryTime = deliveryTime.replace(/\.\d+/, '').replace(/Z$/, '')
    }

    console.log('deliveryTime:', deliveryTime)

    // 通过路由状态传递完整的批次数据，同时在query中传递id和时间
    router.push({
        path: `/driver/delivery-batch-detail/${batch.batchId}`,
        query: {
            batchId: batch.batchId,
            deliveryTime: deliveryTime
        },
        state: {
            batchData: JSON.parse(JSON.stringify(batch))
        }
    })
}

// 开始运输
const startDelivery = async (index) => {
    const batch = batches.value[index]

    if (!confirm(`确定开始运输批次 ${index + 1} 吗？\n\n该批次包含 ${batch.orders.length} 个订单`)) {
        return
    }

    try {
        const response = await request.post(`/delivery-batch/start-batch?batchId=${batch.batchId}`)

        if (response.code === 200) {
            alert('开始运输成功！')
            // 刷新批次列表
            await fetchDeliveryBatches()
        } else {
            alert('开始运输失败：' + (response.message || '未知错误'))
        }
    } catch (error) {
        console.error('开始运输失败:', error)
        alert('开始运输失败：' + (error.response?.data?.message || error.message))
    }
}

// 完成送货
const completeBatch = async (orders) => {
    if (!orders || orders.length === 0) {
        alert('批次中没有订单')
        return
    }
    
    if (!confirm(`确定要完成这${orders.length}个订单的送货吗？`)) {
        return
    }
    
    try {
        // 调用后端API，将订单状态更新为"已到达"(4)
        const orderIds = orders.map(o => o.orderId)
        const response = await request.post('/orders/delivery-complete', orderIds)
        
        if (response.code === 200) {
            alert('送货完成！')
            // 刷新批次列表
            await fetchDeliveryBatches()
        } else {
            alert('操作失败：' + (response.message || '未知错误'))
        }
    } catch (error) {
        console.error('完成送货失败:', error)
        alert('完成送货失败：' + (error.response?.data?.message || error.message))
    }
}

// 仓库筛选变化
const handleWarehouseChange = () => {
    fetchDeliveryBatches()
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

// 获取状态文本
const getStatusText = (status) => {
    const statusMap = {
        0: '待出发',
        1: '配送中',
        2: '已完成'
    }
    return statusMap[status] || '未知'
}

// 获取状态样式类
const getStatusClass = (status) => {
    const classMap = {
        0: 'status-pending',
        1: 'status-delivering',
        2: 'status-completed'
    }
    return classMap[status] || ''
}

// 页面加载
onMounted(() => {
    getCurrentUserWarehouse()
    fetchWarehouses()
    fetchDeliveryBatches()
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

.warehouse-filter {
    display: flex;
    align-items: center;
    gap: 10px;
}

.warehouse-select {
    padding: 8px 12px;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    font-size: 14px;
    cursor: pointer;
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

.status-badge.status-pending {
    background: #fef3c7;
    color: #92400e;
}

.status-badge.status-delivering {
    background: #dbeafe;
    color: #1e40af;
}

.status-badge.status-completed {
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
    margin-bottom: 20px;
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

/* 批次操作栏 */
.batch-actions {
    display: flex;
    gap: 12px;
    padding-top: 16px;
    border-top: 1px solid #e5e7eb;
}

.start-btn {
    flex: 1;
    padding: 8px 16px;
    background-color: #ffffff;
    border: 1px solid #e5e7eb;
    color: #374151;
    border-radius: 6px;
    cursor: pointer;
    font-size: 14px;
    transition: all 0.2s;
}

.start-btn:hover {
    background-color: #f9fafb;
    border-color: #d1d5db;
}

.detail-btn {
    flex: 1;
    padding: 8px 16px;
    background-color: #ffffff;
    border: 1px solid #e5e7eb;
    color: #374151;
    border-radius: 6px;
    cursor: pointer;
    font-size: 14px;
    transition: all 0.2s;
}

.detail-btn:hover {
    background-color: #f9fafb;
    border-color: #d1d5db;
}

.complete-btn {
    flex: 1;
    padding: 12px 16px;
    background-color: #ffffff;
    color: #374151;
    border: 1px solid #e5e7eb;
    border-radius: 6px;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s ease;
}

.complete-btn:hover {
    background-color: #f9fafb;
    border-color: #d1d5db;
}

.cancel-btn {
    flex: 1;
    padding: 12px 16px;
    background-color: #ffffff;
    color: #374151;
    border: 1px solid #e5e7eb;
    border-radius: 6px;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s ease;
}

.cancel-btn:hover {
    background-color: #f9fafb;
    border-color: #d1d5db;
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

    .batch-actions {
        flex-direction: column;
    }
}
</style>
