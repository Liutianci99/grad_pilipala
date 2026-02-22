<template>
    <div class="page-container">
        <div class="page-header">
            <h1>库存管理</h1>
            <div style="display: flex; gap: 8px;">
                <button class="btn btn-ghost" @click="$router.push('/merchant/stock-in')">商品入库</button>
                <button class="btn btn-primary" @click="$router.push('/merchant/product-listing')">商品上架</button>
            </div>
        </div>

        <div class="filter-bar">
            <div class="search-box">
                <input type="text" v-model="searchKeyword" placeholder="搜索商品名称..." class="search-input" @keyup.enter="handleSearch" />
                <button class="btn btn-ghost btn-sm" @click="handleSearch">搜索</button>
            </div>
            <div style="display: flex; align-items: center; gap: 12px; flex-wrap: wrap;">
                <label class="form-label" style="margin: 0;">库存：</label>
                <input type="number" v-model.number="minStock" placeholder="最少" class="form-input" style="width: 100px;" />
                <span style="color: #8899a6;">—</span>
                <input type="number" v-model.number="maxStock" placeholder="最多" class="form-input" style="width: 100px;" />
                <label class="form-label" style="margin: 0 0 0 8px;">状态：</label>
                <select v-model="statusFilter" class="form-select" style="width: 120px;">
                    <option value="all">全部</option>
                    <option value="online">已上架</option>
                    <option value="offline">未上架</option>
                </select>
                <button class="btn btn-ghost btn-sm" @click="handleSearch">筛选</button>
                <button class="btn btn-ghost btn-sm" @click="clearFilters">清除</button>
            </div>
        </div>

        <div class="inventory-stats">
            <div class="stat-pill">
                <span class="stat-label">全部</span>
                <span class="stat-value">{{ inventory.length }}</span>
            </div>
            <div class="stat-pill online">
                <span class="stat-label">已上架</span>
                <span class="stat-value">{{ inventory.filter(i => i.status === 'online').length }}</span>
            </div>
            <div class="stat-pill offline">
                <span class="stat-label">未上架</span>
                <span class="stat-value">{{ inventory.filter(i => i.status === 'offline').length }}</span>
            </div>
        </div>

        <div class="inventory-grid" v-if="inventory.length">
            <div class="inventory-card" v-for="item in inventory" :key="item.id">
                <div class="card-image">
                    <img :src="item.image" :alt="item.name" />
                    <span class="card-status" :class="item.status">
                        {{ item.status === 'online' ? '已上架' : '未上架' }}
                    </span>
                </div>
                <div class="card-body">
                    <h4 class="card-title">{{ item.name }}</h4>
                    <p class="card-desc" v-if="item.description">{{ item.description }}</p>
                    <div class="card-footer">
                        <span class="meta-item">{{ item.warehouse }}</span>
                        <span class="stock-count" :class="{ low: item.stock < 50 }">{{ item.stock }} 件</span>
                    </div>
                </div>
            </div>
        </div>

        <div class="empty-state" v-else>
            <p>暂无库存商品</p>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const searchKeyword = ref('')
const minStock = ref(null)
const maxStock = ref(null)
const statusFilter = ref('all')

const activeSearchKeyword = ref('')
const activeMinStock = ref(null)
const activeMaxStock = ref(null)
const activeStatusFilter = ref('all')

const inventory = ref([])

const fetchInventory = async () => {
    const userInfo = JSON.parse(sessionStorage.getItem('userInfo') || '{}')
    const params = { userId: userInfo.id || 1 }

    if (activeSearchKeyword.value) params.productName = activeSearchKeyword.value
    if (activeMinStock.value !== null && activeMinStock.value !== '') params.minStock = activeMinStock.value
    if (activeMaxStock.value !== null && activeMaxStock.value !== '') params.maxStock = activeMaxStock.value
    if (activeStatusFilter.value !== 'all') params.isPublished = activeStatusFilter.value === 'online' ? 1 : 0

    const response = await request.get('/inventory/list', { params })
    if (response.code === 200) {
        inventory.value = response.data.map(item => ({
            id: item.productId,
            name: item.productName,
            description: item.description || '',
            stock: item.quantity,
            status: item.isPublished === 1 ? 'online' : 'offline',
            image: item.imageUrl || 'https://via.placeholder.com/200',
            warehouse: item.warehouseName || '未分配仓库'
        }))
    } else {
        ElMessage.error('获取库存失败：' + response.message)
    }
}

const handleSearch = () => {
    activeSearchKeyword.value = searchKeyword.value
    activeMinStock.value = minStock.value
    activeMaxStock.value = maxStock.value
    activeStatusFilter.value = statusFilter.value
    fetchInventory()
}

const clearFilters = () => {
    searchKeyword.value = ''
    minStock.value = null
    maxStock.value = null
    statusFilter.value = 'all'
    activeSearchKeyword.value = ''
    activeMinStock.value = null
    activeMaxStock.value = null
    activeStatusFilter.value = 'all'
    fetchInventory()
}

onMounted(() => { fetchInventory() })
</script>

<style scoped>
.inventory-stats {
    display: flex;
    gap: 8px;
    margin-bottom: 16px;
}
.stat-pill {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 6px 14px;
    background: #f7f9f9;
    border: 1px solid #eff3f4;
    border-radius: 9999px;
    font-size: 13px;
}
.stat-label { color: #536471; font-weight: 500; }
.stat-value { font-weight: 700; color: #0f1419; }
.stat-pill.online .stat-value { color: #15803d; }
.stat-pill.offline .stat-value { color: #b45309; }

.inventory-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
    gap: 16px;
}
.inventory-card {
    background: #fff;
    border: 1px solid #eff3f4;
    border-radius: 12px;
    overflow: hidden;
    transition: all 0.2s;
}
.inventory-card:hover {
    border-color: #cfd9de;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
    transform: translateY(-2px);
}
.card-image {
    position: relative;
    width: 100%;
    aspect-ratio: 1;
    background: #f7f9f9;
    overflow: hidden;
}
.card-image img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.3s;
}
.inventory-card:hover .card-image img {
    transform: scale(1.03);
}
.card-status {
    position: absolute;
    top: 10px;
    right: 10px;
    padding: 3px 10px;
    border-radius: 9999px;
    font-size: 11px;
    font-weight: 600;
}
.card-status.online {
    background: #f0fdf4;
    color: #15803d;
    border: 1px solid #bbf7d0;
}
.card-status.offline {
    background: #fff7e6;
    color: #b45309;
    border: 1px solid #fde68a;
}
.card-body {
    padding: 14px;
}
.card-title {
    margin: 0 0 4px 0;
    font-size: 14px;
    font-weight: 700;
    color: #0f1419;
}
.card-desc {
    margin: 0 0 10px 0;
    font-size: 12px;
    color: #536471;
    line-height: 1.5;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}
.card-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
}
.stock-count {
    font-size: 13px;
    font-weight: 700;
    color: #0f1419;
}
.stock-count.low {
    color: #f4212e;
}

@media (max-width: 640px) {
    .inventory-grid {
        grid-template-columns: repeat(2, 1fr);
        gap: 10px;
    }
}
</style>
