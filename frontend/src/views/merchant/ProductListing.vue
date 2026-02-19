<template>
    <div class="page-container">
        <h1>商品上架</h1>
        
        <!-- 未上架商品列表 -->
        <div class="product-list">
            <div class="product-item" v-for="product in offlineProducts" :key="product.id">
                <div class="product-image">
                    <img :src="product.image" :alt="product.name" />
                </div>
                <div class="product-info">
                    <h3 class="product-name">{{ product.name }}</h3>
                    <p class="product-description">{{ product.description }}</p>
                    <div class="product-meta">
                        <span class="meta-item">当前库存: {{ product.stock }} 件</span>
                    </div>
                </div>
                <div class="product-actions">
                    <div class="action-group full-width">
                        <label>商品介绍</label>
                        <textarea 
                            v-model="product.description" 
                            placeholder="请输入商品介绍"
                            class="action-textarea"
                            rows="3"
                        ></textarea>
                    </div>
                    <div class="action-row">
                        <div class="action-group">
                            <label>上架数量</label>
                            <input 
                                type="number" 
                                v-model.number="product.listingQuantity" 
                                placeholder="数量"
                                class="action-input"
                                min="1"
                                :max="product.stock"
                            />
                        </div>
                        <div class="action-group">
                            <label>定价</label>
                            <div class="price-input-wrapper">
                                <span class="currency-symbol">¥</span>
                                <input 
                                    type="number" 
                                    v-model.number="product.listingPrice" 
                                    placeholder="价格"
                                    class="action-input price-input"
                                    min="0"
                                    step="0.01"
                                />
                            </div>
                        </div>
                    </div>
                    <button @click="listProduct(product)" class="listing-btn">上架</button>
                </div>
            </div>
        </div>
        
        <div v-if="offlineProducts.length === 0" class="empty-state">
            <p>暂无未上架商品</p>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '../../utils/request'

const router = useRouter()

// 未上架的商品数据
const offlineProducts = ref([])
const loading = ref(false)

// 获取未上架商品
const fetchOfflineProducts = async () => {
    try {
        loading.value = true
        
        // 获取登录用户ID
        const userInfo = JSON.parse(sessionStorage.getItem('userInfo') || '{}')
        const userId = userInfo.id || 1
        
        const response = await request.get('/mall/offline-products', {
            params: { userId }
        })
        
        if (response.code === 200) {
            // 转换数据格式
            offlineProducts.value = response.data.map(item => ({
                id: item.productId,
                name: item.productName,
                description: item.description || '',
                stock: item.quantity,
                listingQuantity: 1,
                listingPrice: 0,
                image: item.imageUrl || 'https://via.placeholder.com/200'
            }))
        } else {
            alert('获取商品失败：' + response.message)
        }
    } catch (error) {
        console.error('获取商品失败', error)
        alert('获取商品失败：' + (error.response?.data?.message || error.message || '网络错误'))
    } finally {
        loading.value = false
    }
}

// 页面加载时获取数据
onMounted(() => {
    fetchOfflineProducts()
})

// 上架商品
const listProduct = async (product) => {
    // 验证
    if (!product.listingQuantity || product.listingQuantity <= 0) {
        alert('请输入有效的上架数量')
        return
    }
    if (product.listingQuantity > product.stock) {
        alert('上架数量不能超过库存数量')
        return
    }
    if (!product.listingPrice || product.listingPrice <= 0) {
        alert('请输入有效的定价')
        return
    }
    
    try {
        const response = await request.post('/mall/publish', {
            productId: product.id,
            description: product.description,
            quantity: product.listingQuantity,
            price: product.listingPrice
        })
        
        if (response.code === 200) {
            alert(`商品上架成功！\n商品：${product.name}\n数量：${product.listingQuantity}件\n定价：¥${product.listingPrice}`)
            // 从列表中移除已上架商品
            const index = offlineProducts.value.findIndex(p => p.id === product.id)
            if (index !== -1) {
                offlineProducts.value.splice(index, 1)
            }
        } else {
            alert('上架失败：' + response.message)
        }
    } catch (error) {
        console.error('上架失败', error)
        alert('上架失败：' + (error.response?.data?.message || error.message || '网络错误'))
    }
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

/* 商品列表样式 */
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
    margin: 0 0 12px 0;
    color: #6b7280;
    font-size: 14px;
    line-height: 1.5;
}

.product-meta {
    display: flex;
    gap: 16px;
}

.meta-item {
    font-size: 14px;
    color: #374151;
}

.product-actions {
    display: flex;
    flex-direction: column;
    gap: 12px;
    flex-shrink: 0;
    min-width: 300px;
}

.action-group {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.action-group label {
    font-size: 12px;
    color: #6b7280;
}

.action-group.full-width {
    width: 100%;
}

.action-row {
    display: flex;
    gap: 12px;
}

.action-textarea {
    width: 100%;
    padding: 8px 12px;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    font-size: 14px;
    color: #374151;
    resize: vertical;
    font-family: inherit;
    transition: border-color 0.2s;
}

.action-textarea:focus {
    outline: none;
    border-color: #3b82f6;
}

.price-input-wrapper {
    position: relative;
    display: flex;
    align-items: center;
}

.currency-symbol {
    position: absolute;
    left: 12px;
    color: #6b7280;
    font-size: 14px;
    pointer-events: none;
}

.action-input {
    width: 100px;
    padding: 8px 12px;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    font-size: 14px;
    color: #374151;
    transition: border-color 0.2s;
}

.price-input {
    padding-left: 28px;
}

.action-input:focus {
    outline: none;
    border-color: #3b82f6;
}

.listing-btn {
    padding: 8px 24px;
    background: #ffffff;
    color: #374151;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
    white-space: nowrap;
    height: 36px;
}

.listing-btn:hover {
    background: #f9fafb;
    border-color: #9ca3af;
}

.empty-state {
    text-align: center;
    padding: 48px 24px;
    color: #6b7280;
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
}

.empty-state p {
    margin: 0;
    font-size: 16px;
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
        margin-bottom: 16px;
    }

    .product-actions {
        flex-direction: column;
        align-items: stretch;
    }

    .action-input {
        width: 100%;
    }
}
</style>
