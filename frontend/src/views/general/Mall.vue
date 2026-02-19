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
            <div class="price-filter">
                <label>价格区间：</label>
                <input 
                    type="number" 
                    v-model.number="minPrice" 
                    placeholder="最低价"
                    class="price-input"
                />
                <span class="price-separator">-</span>
                <input 
                    type="number" 
                    v-model.number="maxPrice" 
                    placeholder="最高价"
                    class="price-input"
                />
                <button @click="handleSearch" class="filter-btn">筛选</button>
                <button @click="clearFilters" class="clear-btn">清除结果</button>
                <button @click="goToProductManagement" class="add-product-btn" v-if="userRole === 'merchant'">商品下架</button>
            </div>
        </div>

        <!-- 商品列表 -->
        <div class="product-list">
            <div class="product-item" v-for="product in filteredProducts" :key="product.id">
                <div class="product-image">
                    <img :src="product.image" :alt="product.name" />
                </div>
                <div class="product-info">
                    <h3 class="product-name">{{ product.name }}</h3>
                    <p class="product-description">{{ product.description }}</p>
                </div>
                <div class="product-price">
                    <span class="price-label">售价</span>
                    <span class="price-value">¥{{ product.price }}</span>
                </div>
                <div class="product-quantity" v-if="userRole === 'consumer'">
                    <button @click="decreaseQuantity(product)" class="quantity-btn">−</button>
                    <input 
                        type="number" 
                        v-model.number="product.quantity" 
                        min="1" 
                        class="quantity-input"
                        @input="validateQuantity(product)"
                    />
                    <button @click="increaseQuantity(product)" class="quantity-btn">+</button>
                </div>
                <div class="product-action" v-if="userRole === 'consumer'">
                    <button @click="placeOrder(product)" class="order-btn">下单</button>
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

// 获取用户角色
const userRole = computed(() => {
    const userInfo = sessionStorage.getItem('userInfo')
    if (userInfo) {
        return JSON.parse(userInfo).role
    }
    return null
})

// 搜索和筛选 - 输入框绑定的值
const searchKeyword = ref('')
const minPrice = ref(null)
const maxPrice = ref(null)

// 实际用于筛选的值（点击搜索后更新）
const activeSearchKeyword = ref('')
const activeMinPrice = ref(null)
const activeMaxPrice = ref(null)

// 商品数据
const products = ref([])

// 从后端获取商城商品数据
const fetchMallProducts = async () => {
    try {
        const response = await request.get('/mall/products')
        if (response.code === 200) {
            // 映射后端数据到前端格式，添加quantity字段用于购物
            products.value = response.data.map(item => ({
                id: item.productId,
                name: item.productName,
                description: item.description || '暂无描述',
                price: item.price,
                quantity: 1, // 默认购买数量
                image: item.imageUrl,
                availableQuantity: item.availableQuantity // 保存可用库存
            }))
        }
    } catch (error) {
        console.error('获取商城商品失败:', error)
        alert('获取商城商品失败，请稍后重试')
    }
}

// 页面加载时获取数据
onMounted(() => {
    fetchMallProducts()
})

// 过滤后的商品列表
const filteredProducts = computed(() => {
    let result = products.value

    // 按名称搜索
    if (activeSearchKeyword.value.trim()) {
        result = result.filter(product => 
            product.name.toLowerCase().includes(activeSearchKeyword.value.toLowerCase())
        )
    }

    // 按价格筛选
    if (activeMinPrice.value !== null && activeMinPrice.value !== '') {
        result = result.filter(product => product.price >= activeMinPrice.value)
    }
    if (activeMaxPrice.value !== null && activeMaxPrice.value !== '') {
        result = result.filter(product => product.price <= activeMaxPrice.value)
    }

    return result
})

// 搜索处理
const handleSearch = () => {
    // 将输入框的值应用到实际筛选变量
    activeSearchKeyword.value = searchKeyword.value
    activeMinPrice.value = minPrice.value
    activeMaxPrice.value = maxPrice.value
}

// 清除筛选
const clearFilters = () => {
    searchKeyword.value = ''
    minPrice.value = null
    maxPrice.value = null
    // 同时清除实际筛选变量
    activeSearchKeyword.value = ''
    activeMinPrice.value = null
    activeMaxPrice.value = null
}

// 增加数量
const increaseQuantity = (product) => {
    product.quantity++
}

// 减少数量
const decreaseQuantity = (product) => {
    if (product.quantity > 1) {
        product.quantity--
    }
}

// 验证数量
const validateQuantity = (product) => {
    if (product.quantity < 1 || isNaN(product.quantity)) {
        product.quantity = 1
    }
}

// 下单
const placeOrder = async (product) => {
    try {
        // 获取当前用户信息
        const userInfo = sessionStorage.getItem('userInfo')
        if (!userInfo) {
            alert('请先登录')
            router.push('/login')
            return
        }
        
        const user = JSON.parse(userInfo)
        
        // 获取用户默认地址
        let addressId = null
        try {
            const addressResponse = await request.get(`/address/list?userId=${user.id}`)
            if (addressResponse.code === 200 && addressResponse.data && addressResponse.data.length > 0) {
                // 查找默认地址
                const defaultAddress = addressResponse.data.find(addr => addr.isDefault === 1)
                if (defaultAddress) {
                    addressId = defaultAddress.id
                }
            }
        } catch (error) {
            console.warn('获取地址信息失败:', error)
            // 地址获取失败时继续，后端会自动查询默认地址
        }
        
        // 调用后端API
        const response = await request.post('/orders/create', {
            customerId: user.id,
            productId: product.id,
            quantity: product.quantity,
            price: product.price,
            addressId: addressId
        })
        
        if (response.code === 200) {
            alert(`下单成功！\n商品：${product.name}\n数量：${product.quantity}\n总价：¥${product.price * product.quantity}`)
            // 刷新商品列表以更新库存
            await fetchMallProducts()
        } else {
            alert('下单失败：' + (response.message || '未知错误'))
        }
    } catch (error) {
        console.error('下单失败:', error)
        alert('下单失败：' + (error.response?.data?.message || error.message || '网络错误'))
    }
}

// 跳转到商品下架页面
const goToProductManagement = () => {
    router.push('/merchant/product-delisting')
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

.price-filter {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
}

.price-filter label {
    font-size: 14px;
    color: #374151;
    font-weight: 500;
}

.price-input {
    width: 120px;
    padding: 8px 12px;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    font-size: 14px;
    color: #374151;
    transition: border-color 0.2s;
}

.price-input:focus {
    outline: none;
    border-color: #3b82f6;
}

.price-separator {
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

.add-product-btn {
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

.add-product-btn:hover {
    background: #f9fafb;
    border-color: #9ca3af;
}

.product-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.product-item {
    display: flex;
    align-items: center;
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    padding: 16px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    transition: box-shadow 0.2s;
}

.product-item:hover {
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

.product-quantity {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-right: 20px;
}

.quantity-btn {
    width: 32px;
    height: 32px;
    border: 1px solid #d1d5db;
    border-radius: 4px;
    background: #ffffff;
    color: #374151;
    font-size: 18px;
    cursor: pointer;
    transition: all 0.2s;
    display: flex;
    align-items: center;
    justify-content: center;
}

.quantity-btn:hover {
    background: #f9fafb;
    border-color: #9ca3af;
}

.quantity-btn:active {
    background: #f3f4f6;
}

.quantity-input {
    width: 60px;
    height: 32px;
    text-align: center;
    border: 1px solid #d1d5db;
    border-radius: 4px;
    font-size: 14px;
    color: #374151;
}

.quantity-input:focus {
    outline: none;
    border-color: #3b82f6;
}

/* 隐藏数字输入框的上下箭头 */
.quantity-input::-webkit-inner-spin-button,
.quantity-input::-webkit-outer-spin-button {
    -webkit-appearance: none;
    margin: 0;
}

.quantity-input[type=number] {
    -moz-appearance: textfield;
}

.product-action {
    flex-shrink: 0;
}

.order-btn {
    padding: 10px 24px;
    background: #ffffff;
    color: #1f2937;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
}

.order-btn:hover {
    background: #f9fafb;
    border-color: #9ca3af;
}

.order-btn:active {
    background: #f3f4f6;
}

/* 响应式设计 */
@media (max-width: 768px) {
    .product-item {
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

    .product-quantity {
        margin-right: 0;
        margin-bottom: 12px;
        justify-content: center;
    }
}
</style>
