<template>
    <div class="page-container">
        <!-- 批次操作栏 -->
        <div class="filter-bar">
            <div class="batch-info">
                <button 
                    class="create-batch-btn" 
                    @click="createBatch" 
                    :disabled="selectedOrders.length === 0"
                >
                    创建送货批次
                </button>
                <span class="selected-count">已选择: {{ selectedOrders.length }}/5</span>
            </div>
        </div>

        <!-- 订单列表 -->
        <div class="order-list">
            <div v-if="orders.length === 0" class="empty-state">
                <p>暂无待送货订单</p>
            </div>
            
            <div v-else class="order-item" v-for="order in orders" :key="order.orderId">
                <div class="order-header">
                    <div class="header-left">
                        <input 
                            type="checkbox" 
                            :value="order.orderId" 
                            v-model="selectedOrders"
                            :disabled="!selectedOrders.includes(order.orderId) && selectedOrders.length >= 5"
                            class="order-checkbox"
                        />
                        <span class="order-time">揽收时间：{{ formatTime(order.pickupTime) }}</span>
                    </div>
                    <span class="order-id">订单号：{{ order.orderId }}</span>
                    <span class="default-badge">待送货</span>
                </div>
                
                <div class="order-content">
                    <div class="product-image">
                        <img :src="order.imageUrl || '/placeholder.png'" :alt="order.productName" />
                    </div>
                    <div class="product-info">
                        <h3 class="product-name">{{ order.productName }}</h3>
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

// 状态数据
const orders = ref([])
const selectedOrders = ref([])
const currentUserId = ref(null)

// 获取当前用户ID
const getCurrentUserId = () => {
    const userInfoStr = sessionStorage.getItem('userInfo')
    if (userInfoStr) {
        const userInfo = JSON.parse(userInfoStr)
        if (userInfo.role === 'driver' && userInfo.id) {
            currentUserId.value = userInfo.id
        }
    }
}

// 获取待送货订单列表（根据配送员ID自动获取所属仓库的订单）
const getOrders = async () => {
    try {
        if (!currentUserId.value) {
            alert('未获取到配送员信息，请重新登录')
            return
        }
        
        const params = {
            deliveryPersonnelId: currentUserId.value
        }
        
        const res = await request.get('/orders/pending-delivery', { params })
        if (res.success) {
            orders.value = res.data || []
        } else {
            alert(res.message || '获取订单列表失败')
        }
    } catch (error) {
        console.error('获取订单列表失败:', error)
        alert('获取订单列表失败')
    }
}

// 创建送货批次
const createBatch = async () => {
    if (selectedOrders.value.length === 0) {
        alert('请至少选择一个订单')
    
            if (!currentUserId.value) {
                alert('未获取到配送员信息，请重新登录')
                return
            }
        return
    }
    
    if (!confirm(`确认创建送货批次？共 ${selectedOrders.value.length} 个订单`)) {
        return
    }
    
    try {
        const res = await request.post(`/orders/delivery-batch?deliveryPersonnelId=${currentUserId.value}`, selectedOrders.value)
        if (res.success) {
            alert('创建送货批次成功')
            selectedOrders.value = []
            getOrders()
        } else {
            alert(res.message || '创建送货批次失败')
        }
    } catch (error) {
        console.error('创建送货批次失败:', error)
        alert('创建送货批次失败')
    }
}

// 格式化时间
const formatTime = (time) => {
    if (!time) return ''
    const date = new Date(time)
    return date.toLocaleString('zh-CN')
}

// 页面加载时获取数据
onMounted(() => {
    getCurrentUserId()
    getOrders()
})
</script>

<style scoped>
.page-container {
    width: 100%;
}

/* 批次操作栏样式 */
.filter-bar {
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    padding: 20px;
    margin-bottom: 20px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    display: flex;
    justify-content: flex-start;
    align-items: center;
}

.batch-info {
    display: flex;
    align-items: center;
    gap: 16px;
}

.selected-count {
    font-size: 14px;
    color: #6b7280;
}

.create-batch-btn {
    padding: 10px 24px;
    background: #111111;
    color: #ffffff;
    border: none;
    border-radius: 6px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
}

.create-batch-btn:hover:not(:disabled) {
    background: #000000;
}

.create-batch-btn:disabled {
    background: #d1d5db;
    cursor: not-allowed;
}

/* 订单列表样式 */
.order-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.empty-state {
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    padding: 60px 20px;
    text-align: center;
    color: #9ca3af;
}

.order-item {
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    transition: box-shadow 0.2s;
}

.order-item:hover {
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.order-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 20px;
    background: #f9fafb;
    border-bottom: 1px solid #e5e7eb;
}

.header-left {
    display: flex;
    align-items: center;
    gap: 12px;
}

.order-checkbox {
    width: 18px;
    height: 18px;
    cursor: pointer;
    accent-color: #111111;
}

.order-checkbox:disabled {
    cursor: not-allowed;
}

.order-time {
    font-size: 14px;
    color: #6b7280;
}

.order-id {
    font-size: 14px;
    color: #374151;
}

.default-badge {
    padding: 2px 8px;
    background-color: transparent;
    color: #374151;
    border-radius: 4px;
    font-size: 14px;
    font-weight: 500;
}

.order-content {
    display: flex;
    align-items: center;
    padding: 20px;
    gap: 20px;
}

.product-image {
    width: 100px;
    height: 100px;
    flex-shrink: 0;
    border-radius: 6px;
    overflow: hidden;
    background: #f3f4f6;
}

.product-image img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.product-info {
    flex: 1;
    min-width: 0;
}

.product-name {
    margin: 0 0 8px 0;
    color: #1f2937;
    font-size: 16px;
    font-weight: 600;
}

.product-detail {
    display: flex;
    gap: 16px;
    color: #6b7280;
    font-size: 14px;
}
</style>
