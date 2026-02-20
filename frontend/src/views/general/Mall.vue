<template>
    <div class="page-container">
        <div class="filter-bar">
            <div class="filter-row">
                <div class="search-box">
                    <input 
                        type="text" 
                        v-model="searchKeyword" 
                        placeholder="搜索商品..."
                        class="search-input"
                        @keyup.enter="handleSearch"
                    />
                    <button @click="handleSearch" class="btn btn-ghost">搜索</button>
                </div>
                <div class="filter-actions">
                    <button @click="goToProductManagement" class="btn btn-ghost" v-if="userRole === 'merchant'">商品下架</button>
                </div>
            </div>
            <div class="price-row">
                <span class="filter-label">价格</span>
                <input type="number" v-model.number="minPrice" placeholder="最低" class="price-input" />
                <span class="separator">–</span>
                <input type="number" v-model.number="maxPrice" placeholder="最高" class="price-input" />
                <button @click="handleSearch" class="btn btn-ghost btn-sm">筛选</button>
                <button @click="clearFilters" class="btn btn-ghost btn-sm">清除</button>
            </div>
        </div>

        <div class="product-grid">
            <div v-if="filteredProducts.length === 0" class="empty-state">
                <p>暂无商品</p>
            </div>

            <div class="product-card" v-for="product in filteredProducts" :key="product.id">
                <div class="product-image">
                    <img :src="product.image" :alt="product.name" />
                </div>
                <div class="product-body">
                    <h3 class="product-name">{{ product.name }}</h3>
                    <p class="product-desc">{{ product.description }}</p>
                    <div class="product-footer">
                        <span class="product-price">¥{{ product.price }}</span>
                        <div class="quantity-control" v-if="userRole === 'consumer'">
                            <button @click="decreaseQuantity(product)" class="qty-btn">−</button>
                            <input type="number" v-model.number="product.quantity" min="1" class="qty-input" @input="validateQuantity(product)" />
                            <button @click="increaseQuantity(product)" class="qty-btn">+</button>
                        </div>
                        <button v-if="userRole === 'consumer'" @click="placeOrder(product)" class="btn btn-primary btn-sm">下单</button>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()

const userRole = computed(() => {
    const userInfo = sessionStorage.getItem('userInfo')
    return userInfo ? JSON.parse(userInfo).role : null
})

const searchKeyword = ref('')
const minPrice = ref(null)
const maxPrice = ref(null)
const activeSearchKeyword = ref('')
const activeMinPrice = ref(null)
const activeMaxPrice = ref(null)
const products = ref([])

const fetchMallProducts = async () => {
    try {
        const response = await request.get('/mall/products')
        if (response.code === 200) {
            products.value = response.data.map(item => ({
                id: item.productId,
                name: item.productName,
                description: item.description || '暂无描述',
                price: item.price,
                quantity: 1,
                image: item.imageUrl,
                availableQuantity: item.availableQuantity
            }))
        }
    } catch (error) {
        console.error('获取商城商品失败:', error)
    }
}

onMounted(() => { fetchMallProducts() })

const filteredProducts = computed(() => {
    let result = products.value
    if (activeSearchKeyword.value.trim()) {
        result = result.filter(p => p.name.toLowerCase().includes(activeSearchKeyword.value.toLowerCase()))
    }
    if (activeMinPrice.value !== null && activeMinPrice.value !== '') {
        result = result.filter(p => p.price >= activeMinPrice.value)
    }
    if (activeMaxPrice.value !== null && activeMaxPrice.value !== '') {
        result = result.filter(p => p.price <= activeMaxPrice.value)
    }
    return result
})

const handleSearch = () => {
    activeSearchKeyword.value = searchKeyword.value
    activeMinPrice.value = minPrice.value
    activeMaxPrice.value = maxPrice.value
}

const clearFilters = () => {
    searchKeyword.value = ''
    minPrice.value = null
    maxPrice.value = null
    activeSearchKeyword.value = ''
    activeMinPrice.value = null
    activeMaxPrice.value = null
}

const increaseQuantity = (p) => { p.quantity++ }
const decreaseQuantity = (p) => { if (p.quantity > 1) p.quantity-- }
const validateQuantity = (p) => { if (p.quantity < 1 || isNaN(p.quantity)) p.quantity = 1 }

const placeOrder = async (product) => {
    try {
        const userInfo = sessionStorage.getItem('userInfo')
        if (!userInfo) return
        const user = JSON.parse(userInfo)

        let addressId = null
        try {
            const addressResponse = await request.get(`/address/list?userId=${user.id}`)
            if (addressResponse.code === 200 && addressResponse.data?.length > 0) {
                const defaultAddress = addressResponse.data.find(addr => addr.isDefault === 1)
                if (defaultAddress) addressId = defaultAddress.id
            }
        } catch (error) { /* continue */ }

        const response = await request.post('/orders/create', {
            customerId: user.id,
            productId: product.id,
            quantity: product.quantity,
            price: product.price,
            addressId
        })

        if (response.code === 200) {
            await fetchMallProducts()
        }
    } catch (error) {
        console.error('下单失败:', error)
    }
}

const goToProductManagement = () => { router.push('/merchant/product-delisting') }
</script>

<style scoped>
.filter-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;
}
.price-row {
    display: flex;
    align-items: center;
    gap: 8px;
}
.filter-label {
    font-size: 13px;
    color: #737373;
    font-weight: 500;
}
.price-input {
    width: 90px;
    height: 32px;
    padding: 0 10px;
    border: 1px solid #e5e5e5;
    border-radius: 6px;
    font-size: 13px;
    color: #0a0a0a;
}
.price-input:focus { outline: none; border-color: #0a0a0a; }
.separator { color: #d4d4d4; }

.product-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 16px;
}

.product-card {
    background: #ffffff;
    border: 1px solid #e5e5e5;
    border-radius: 10px;
    overflow: hidden;
    transition: border-color 0.15s;
}
.product-card:hover { border-color: #d4d4d4; }

.product-image {
    width: 100%;
    height: 180px;
    background: #f5f5f5;
    overflow: hidden;
}
.product-image img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.product-body { padding: 14px; }
.product-name {
    margin: 0 0 4px 0;
    font-size: 15px;
    font-weight: 600;
    color: #0a0a0a;
}
.product-desc {
    margin: 0 0 12px 0;
    font-size: 13px;
    color: #737373;
    line-height: 1.4;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.product-footer {
    display: flex;
    align-items: center;
    gap: 10px;
}
.product-price {
    font-size: 16px;
    font-weight: 600;
    color: #0a0a0a;
    margin-right: auto;
}

.quantity-control {
    display: flex;
    align-items: center;
    gap: 4px;
}
.qty-btn {
    width: 28px;
    height: 28px;
    border: 1px solid #e5e5e5;
    border-radius: 6px;
    background: #ffffff;
    color: #525252;
    font-size: 14px;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.15s;
}
.qty-btn:hover { background: #f5f5f5; border-color: #d4d4d4; }
.qty-input {
    width: 40px;
    height: 28px;
    text-align: center;
    border: 1px solid #e5e5e5;
    border-radius: 6px;
    font-size: 13px;
    color: #0a0a0a;
}
.qty-input:focus { outline: none; border-color: #0a0a0a; }
.qty-input::-webkit-inner-spin-button,
.qty-input::-webkit-outer-spin-button { -webkit-appearance: none; margin: 0; }
.qty-input[type=number] { -moz-appearance: textfield; }

.filter-actions { display: flex; gap: 8px; }

@media (max-width: 640px) {
    .product-grid { grid-template-columns: 1fr; }
}
</style>
