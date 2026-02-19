<template>
    <div class="page-container">
        <!-- 搜索筛选栏 -->
        <div class="filter-bar">
            <div class="search-box">
                <input 
                    type="text" 
                    v-model="searchKeyword" 
                    placeholder="搜索商品名称..."
                    class="search-input"
                    @keyup.enter="handleSearch"
                />
                <button @click="handleSearch" class="search-btn">搜索</button>
            </div>
            
            <div class="status-tabs">
                <button 
                    :class="['tab-btn', { active: currentStatus === 'all' }]" 
                    @click="changeStatus('all')"
                >
                    全部
                </button>
                <button 
                    :class="['tab-btn', { active: currentStatus === 'pending' }]" 
                    @click="changeStatus('pending')"
                >
                    待发货
                </button>
                <button 
                    :class="['tab-btn', { active: currentStatus === 'shipped' }]" 
                    @click="changeStatus('shipped')"
                >
                    已发货
                </button>
                <button 
                    :class="['tab-btn', { active: currentStatus === 'completed' }]" 
                    @click="changeStatus('completed')"
                >
                    已完成
                </button>
            </div>
        </div>

        <!-- 订单列表 -->
        <div class="order-list">
            <div v-if="orders.length === 0" class="empty-state">
                <p>暂无订单数据</p>
            </div>
            
            <div v-else class="order-item" v-for="order in orders" :key="order.orderId">
                <div class="order-header">
                    <span class="order-time">下单时间：{{ formatTime(order.orderTime) }}</span>
                    <span class="order-id">订单号：{{ order.orderId }}</span>
                    <span class="order-status">{{ getStatusText(order.status) }}</span>
                </div>
                
                <div class="order-content">
                    <div class="product-image">
                        <img :src="order.imageUrl || '/placeholder.png'" :alt="order.productName" />
                    </div>
                    <div class="product-info">
                        <h3 class="product-name">{{ order.productName }}</h3>
                        <div class="product-detail">
                            <span>单价：¥{{ order.unitPrice }}</span>
                            <span>数量：{{ order.quantity }}</span>
                        </div>
                    </div>
                    <div class="order-amount">
                        <span class="amount-label">总计</span>
                        <span class="amount-value">¥{{ order.totalAmount }}</span>
                    </div>
                    <div class="order-actions">
                        <button v-if="order.status === 0" class="action-btn primary-btn" @click="shipOrder(order.orderId)">
                            发货
                        </button>
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
const searchKeyword = ref('')
const activeSearchKeyword = ref('')
const currentStatus = ref('all')
const orders = ref([])

// 获取订单列表
const getOrders = async () => {
    try {
        // 获取当前用户信息
        const userInfo = sessionStorage.getItem('userInfo')
        if (!userInfo) {
            alert('请先登录')
            return
        }
        
        const user = JSON.parse(userInfo)
        
        const params = {
            merchantId: user.id
        }
        
        if (currentStatus.value !== 'all') {
            params.status = getStatusValue(currentStatus.value)
        }
        if (activeSearchKeyword.value) {
            params.search = activeSearchKeyword.value
        }
        
        const res = await request.get('/orders/merchant', { params })
        if (res.code === 200) {
            orders.value = res.data || []
        } else {
            alert(res.message || '获取订单列表失败')
        }
    } catch (error) {
        console.error('获取订单列表失败:', error)
        alert('获取订单列表失败')
    }
}

// 切换状态
const changeStatus = (status) => {
    currentStatus.value = status
    getOrders()
}

// 搜索
const handleSearch = () => {
    activeSearchKeyword.value = searchKeyword.value
    getOrders()
}

// 发货
const shipOrder = async (orderId) => {
    if (!confirm('确认要发货吗？')) {
        return
    }
    
    try {
        // 获取当前用户信息
        const userInfo = sessionStorage.getItem('userInfo')
        if (!userInfo) {
            alert('请先登录')
            return
        }
        
        const user = JSON.parse(userInfo)
        
        const res = await request.put(`/orders/${orderId}/ship`, null, {
            params: { merchantId: user.id }
        })
        if (res.code === 200) {
            alert('发货成功')
            getOrders()
        } else {
            alert(res.message || '发货失败')
        }
    } catch (error) {
        console.error('发货失败:', error)
        alert('发货失败')
    }
}

// 格式化时间
const formatTime = (time) => {
    if (!time) return ''
    const date = new Date(time)
    return date.toLocaleString('zh-CN')
}

// 获取状态文本
const getStatusText = (status) => {
    const statusMap = {
        '-1': '已取消',
        0: '未发货',
        1: '已发货',
        2: '已揽收',
        3: '运输中',
        4: '已到达',
        5: '已收货',
        6: '待评价',
        7: '已评价'
    }
    return statusMap[status] || '未知'
}

// 获取状态值（用于筛选）
const getStatusValue = (status) => {
    const statusMap = {
        'pending': 0,    // 待发货
        'shipped': 1,    // 已发货
        'completed': 5   // 已完成（已收货）
    }
    return statusMap[status]
}

// 页面加载时获取数据
onMounted(() => {
    getOrders()
})
</script>

<style scoped>
.page-container {
    width: 100%;
}

/* 搜索筛选栏样式 */
.filter-bar {
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    padding: 20px;
    margin-bottom: 20px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.search-box {
    margin-bottom: 16px;
    display: flex;
    gap: 12px;
}

.search-input {
    flex: 1;
    padding: 10px 16px;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    font-size: 14px;
    color: #374151;
    transition: border-color 0.2s;
}

.search-input:focus {
    outline: none;
    border-color: #3b82f6;
}

.search-btn {
    padding: 10px 24px;
    background: #ffffff;
    color: #1f2937;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
    white-space: nowrap;
}

.search-btn:hover {
    background: #f9fafb;
    border-color: #9ca3af;
}

.search-btn:active {
    background: #f3f4f6;
}

.status-tabs {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
}

.tab-btn {
    padding: 8px 16px;
    background: #ffffff;
    color: #1f2937;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
}

.tab-btn:hover {
    background: #f9fafb;
    border-color: #9ca3af;
}

.tab-btn.active {
    background: #1f2937;
    color: #ffffff;
    border-color: #1f2937;
}

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
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.empty-state p {
    margin: 0;
    color: #9ca3af;
    font-size: 16px;
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
    background: #f9fafb;
    padding: 12px 16px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px solid #e5e7eb;
}

.order-time {
    color: #6b7280;
    font-size: 14px;
}

.order-id {
    color: #6b7280;
    font-size: 14px;
}

.order-status {
    color: #1f2937;
    font-weight: 500;
    font-size: 14px;
}

.order-content {
    padding: 16px;
    display: flex;
    align-items: center;
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
    font-size: 18px;
    font-weight: 600;
}

.product-detail {
    display: flex;
    gap: 24px;
    color: #6b7280;
    font-size: 14px;
}

.order-amount {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    min-width: 100px;
}

.amount-label {
    font-size: 12px;
    color: #9ca3af;
    margin-bottom: 4px;
}

.amount-value {
    font-size: 16px;
    font-weight: 500;
    color: #1f2937;
}

.order-actions {
    display: flex;
    flex-direction: column;
    gap: 8px;
    min-width: 100px;
}

.action-btn {
    padding: 8px 16px;
    background: #ffffff;
    color: #1f2937;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
    white-space: nowrap;
}

.action-btn:hover {
    background: #f9fafb;
    border-color: #9ca3af;
}

.action-btn:active {
    background: #f3f4f6;
}

.action-btn.primary-btn {
    background: #1f2937;
    color: #ffffff;
    border-color: #1f2937;
}

.action-btn.primary-btn:hover {
    background: #374151;
    border-color: #374151;
}

/* 响应式设计 */
@media (max-width: 768px) {
    .order-header {
        flex-direction: column;
        align-items: flex-start;
        gap: 4px;
    }
    
    .order-content {
        flex-direction: column;
        align-items: stretch;
    }

    .product-image {
        width: 100%;
        height: 200px;
    }

    .product-info {
        margin-bottom: 12px;
    }

    .order-amount {
        align-items: flex-start;
        margin-bottom: 12px;
    }

    .order-actions {
        flex-direction: row;
        flex-wrap: wrap;
    }
}
</style>
