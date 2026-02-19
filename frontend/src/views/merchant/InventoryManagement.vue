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
            <div class="stock-filter">
                <label>库存数量：</label>
                <input 
                    type="number" 
                    v-model.number="minStock" 
                    placeholder="最少库存"
                    class="stock-input"
                />
                <span class="stock-separator">-</span>
                <input 
                    type="number" 
                    v-model.number="maxStock" 
                    placeholder="最多库存"
                    class="stock-input"
                />
                <label style="margin-left: 16px;">状态：</label>
                <select v-model="statusFilter" class="status-select">
                    <option value="all">全部</option>
                    <option value="online">已上架</option>
                    <option value="offline">未上架</option>
                </select>
                <button @click="handleSearch" class="filter-btn">筛选</button>
                <button @click="clearFilters" class="clear-btn">清除结果</button>
                <button @click="handleStockIn" class="add-btn">商品入库</button>
                <button @click="goToProductManagement" class="add-btn">商品上架</button>
            </div>
        </div>

        <!-- 库存列表 -->
        <div class="inventory-list">
            <div class="inventory-item" v-for="item in filteredInventory" :key="item.id">
                <div class="product-image">
                    <img :src="item.image" :alt="item.name" />
                </div>
                <div class="product-info">
                    <h3 class="product-name">{{ item.name }}</h3>
                </div>
                <div class="product-warehouse">
                    <span class="warehouse-label">仓库</span>
                    <span class="warehouse-value">{{ item.warehouse }}</span>
                </div>
                <div class="inventory-stock">
                    <span class="stock-label">库存</span>
                    <span class="stock-value" :class="getStockClass(item.stock)">{{ item.stock }} 件</span>
                </div>
                <div class="product-status">
                    <span class="status-label">状态</span>
                    <span class="status-value" :class="item.status === 'online' ? 'status-online' : 'status-offline'">
                        {{ item.status === 'online' ? '已上架' : '未上架' }}
                    </span>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '../../utils/request'

const router = useRouter()

// 搜索和筛选 - 输入框绑定的值
const searchKeyword = ref('')
const minStock = ref(null)
const maxStock = ref(null)
const statusFilter = ref('all')

// 实际用于筛选的值（点击搜索后更新）
const activeSearchKeyword = ref('')
const activeMinStock = ref(null)
const activeMaxStock = ref(null)
const activeStatusFilter = ref('all')

// 库存数据（从后端获取）
const inventory = ref([])
const loading = ref(false)

// 获取库存数据
const fetchInventory = async () => {
    try {
        loading.value = true
        
        // 获取登录用户ID
        const userInfo = JSON.parse(sessionStorage.getItem('userInfo') || '{}')
        const userId = userInfo.id || 1
        
        console.log('获取库存数据，用户ID:', userId)
        
        // 构建查询参数
        const params = {
            userId: userId
        }
        
        if (activeSearchKeyword.value) {
            params.productName = activeSearchKeyword.value
        }
        if (activeMinStock.value !== null && activeMinStock.value !== '') {
            params.minStock = activeMinStock.value
        }
        if (activeMaxStock.value !== null && activeMaxStock.value !== '') {
            params.maxStock = activeMaxStock.value
        }
        if (activeStatusFilter.value !== 'all') {
            params.isPublished = activeStatusFilter.value === 'online' ? 1 : 0
        }
        
        console.log('请求参数:', params)
        const response = await request.get('/inventory/list', { params })
        console.log('API响应:', response)
        
        if (response.code === 200) {
            // 转换数据格式以适配前端显示
            inventory.value = response.data.map(item => ({
                id: item.productId,
                name: item.productName,
                stock: item.quantity,
                status: item.isPublished === 1 ? 'online' : 'offline',
                image: item.imageUrl || 'https://via.placeholder.com/200',
                warehouse: item.warehouseName || '未分配仓库'
            }))
            console.log('库存数据已更新:', inventory.value)
        } else {
            console.error('获取库存失败:', response.message)
            alert('获取库存失败：' + response.message)
        }
    } catch (error) {
        console.error('获取库存失败 - 详细错误:', error)
        console.error('错误响应:', error.response)
        alert('获取库存失败：' + (error.response?.data?.message || error.message || '网络错误'))
    } finally {
        loading.value = false
    }
}

// 页面加载时获取数据
onMounted(() => {
    fetchInventory()
})

// 过滤后的库存列表（因为已经在后端过滤，这里直接返回）
const filteredInventory = computed(() => {
    return inventory.value
})

// 搜索处理
const handleSearch = () => {
    // 将输入框的值应用到实际筛选变量
    activeSearchKeyword.value = searchKeyword.value
    activeMinStock.value = minStock.value
    activeMaxStock.value = maxStock.value
    activeStatusFilter.value = statusFilter.value
    
    // 重新获取数据
    fetchInventory()
}

// 清除筛选
const clearFilters = () => {
    searchKeyword.value = ''
    minStock.value = null
    maxStock.value = null
    statusFilter.value = 'all'
    // 同时清除实际筛选变量
    activeSearchKeyword.value = ''
    activeMinStock.value = null
    activeMaxStock.value = null
    activeStatusFilter.value = 'all'
    
    // 重新获取数据
    fetchInventory()
}

// 跳转到商品上架页面
const goToProductManagement = () => {
    router.push('/merchant/product-listing')
}

// 商品入库处理
const handleStockIn = () => {
    router.push('/merchant/stock-in')
}

// 根据库存数量返回样式类
const getStockClass = (stock) => {
    if (stock === 0) return 'stock-out'
    if (stock < 10) return 'stock-low'
    return 'stock-normal'
}
</script>

<style scoped>
.page-container {
    width: 100%;
}

h1 {
    margin: 0 0 24px 0;
    color: #1f2937;
    font-size: 28px;
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

.stock-filter {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
}

.stock-filter label {
    font-size: 14px;
    color: #374151;
    font-weight: 500;
}

.stock-input {
    width: 120px;
    padding: 8px 12px;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    font-size: 14px;
    color: #374151;
    transition: border-color 0.2s;
}

.stock-input:focus {
    outline: none;
    border-color: #3b82f6;
}

.stock-separator {
    color: #9ca3af;
    font-weight: 500;
}

.filter-btn {
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

.filter-btn:hover {
    background: #f9fafb;
    border-color: #9ca3af;
}

.clear-btn {
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

.clear-btn:hover {
    background: #f9fafb;
    border-color: #9ca3af;
}

.status-select {
    padding: 8px 12px;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    font-size: 14px;
    color: #374151;
    background: #ffffff;
    cursor: pointer;
    transition: border-color 0.2s;
}

.status-select:focus {
    outline: none;
    border-color: #3b82f6;
}

.add-btn {
    padding: 8px 16px;
    background: #ffffff;
    color: #1f2937;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
    margin-left: auto;
}

.add-btn:hover {
    background: #f9fafb;
    border-color: #9ca3af;
}

/* 库存列表样式 */
.inventory-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.inventory-item {
    display: flex;
    align-items: center;
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    padding: 16px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    transition: box-shadow 0.2s;
}

.inventory-item:hover {
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.product-image {
    width: 100px;
    height: 100px;
    flex-shrink: 0;
    margin-right: 20px;
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
    margin-right: 20px;
}

.product-name {
    margin: 0 0 8px 0;
    color: #1f2937;
    font-size: 18px;
    font-weight: 600;
}

.product-description {
    margin: 0;
    color: #6b7280;
    font-size: 14px;
    line-height: 1.5;
}

.product-warehouse {
    display: flex;
    flex-direction: column;
    align-items: center;
    min-width: 120px;
    margin-right: 20px;
}

.warehouse-label {
    font-size: 12px;
    color: #9ca3af;
    margin-bottom: 4px;
}

.warehouse-value {
    font-size: 14px;
    font-weight: 500;
    color: #1f2937;
    text-align: center;
}

.product-price {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    margin-right: 20px;
    min-width: 100px;
}

.price-label {
    font-size: 12px;
    color: #9ca3af;
    margin-bottom: 4px;
}

.price-value {
    font-size: 16px;
    font-weight: 500;
    color: #1f2937;
}

.inventory-stock {
    display: flex;
    flex-direction: column;
    align-items: center;
    min-width: 100px;
}

.stock-label {
    font-size: 12px;
    color: #9ca3af;
    margin-bottom: 4px;
}

.stock-value {
    font-size: 16px;
    font-weight: 500;
    color: #1f2937;
}

.stock-normal {
    color: #1f2937;
}

.stock-low {
    color: #1f2937;
}

.stock-out {
    color: #1f2937;
}

.product-status {
    display: flex;
    flex-direction: column;
    align-items: center;
    min-width: 100px;
}

.status-label {
    font-size: 12px;
    color: #9ca3af;
    margin-bottom: 4px;
}

.status-value {
    font-size: 14px;
    font-weight: 500;
    color: #1f2937;
}

.status-online {
    color: #1f2937;
}

.status-offline {
    color: #1f2937;
}

/* 响应式设计 */
@media (max-width: 768px) {
    .inventory-item {
        flex-direction: column;
        align-items: stretch;
    }

    .product-image {
        width: 100%;
        height: 200px;
        margin-right: 0;
        margin-bottom: 16px;
    }

    .product-info {
        margin-right: 0;
        margin-bottom: 12px;
    }

    .product-price {
        align-items: flex-start;
        margin-right: 0;
        margin-bottom: 12px;
    }

    .inventory-stock {
        align-items: flex-start;
    }
}
</style>
