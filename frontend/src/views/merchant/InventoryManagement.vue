<template>
    <div class="page-container">
        <h1>库存管理</h1>

        <div class="filter-bar">
            <div class="search-box">
                <input type="text" v-model="searchKeyword" placeholder="搜索商品名称..." class="search-input" @keyup.enter="handleSearch" />
                <button class="btn btn-ghost" @click="handleSearch">搜索</button>
            </div>
            <div style="display: flex; align-items: center; gap: 12px; flex-wrap: wrap;">
                <label class="form-label" style="margin: 0;">库存：</label>
                <input type="number" v-model.number="minStock" placeholder="最少" class="form-input" style="width: 100px;" />
                <span style="color: #8899a6;">-</span>
                <input type="number" v-model.number="maxStock" placeholder="最多" class="form-input" style="width: 100px;" />
                <label class="form-label" style="margin: 0 0 0 12px;">状态：</label>
                <select v-model="statusFilter" class="form-select" style="width: 120px;">
                    <option value="all">全部</option>
                    <option value="online">已上架</option>
                    <option value="offline">未上架</option>
                </select>
                <button class="btn btn-ghost" @click="handleSearch">筛选</button>
                <button class="btn btn-ghost" @click="clearFilters">清除</button>
                <div style="margin-left: auto; display: flex; gap: 8px;">
                    <button class="btn btn-ghost" @click="$router.push('/merchant/stock-in')">商品入库</button>
                    <button class="btn btn-primary" @click="$router.push('/merchant/product-listing')">商品上架</button>
                </div>
            </div>
        </div>

        <div style="display: flex; flex-direction: column; gap: 12px;">
            <div class="inventory-item" v-for="item in inventory" :key="item.id">
                <div class="product-image" style="width: 80px; height: 80px;">
                    <img :src="item.image" :alt="item.name" />
                </div>
                <div style="flex: 1;">
                    <h4 class="product-name">{{ item.name }}</h4>
                </div>
                <div class="product-meta">
                    <span class="meta-item">{{ item.warehouse }}</span>
                    <span class="meta-item">{{ item.stock }} 件</span>
                    <span class="meta-item">{{ item.status === 'online' ? '已上架' : '未上架' }}</span>
                </div>
            </div>
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
/* All shared classes come from design.css */
</style>
