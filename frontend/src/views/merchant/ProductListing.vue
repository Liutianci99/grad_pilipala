<template>
    <div class="page-container">
        <h1>商品上架</h1>

        <div v-if="offlineProducts.length === 0" class="empty-state">
            <p>暂无未上架商品</p>
        </div>

        <div v-else class="products-grid">
            <div class="product-card" v-for="product in offlineProducts" :key="product.id">
                <div class="product-image-large">
                    <img :src="product.image" :alt="product.name" />
                </div>
                
                <div class="product-info-section">
                    <h3 class="product-name">{{ product.name }}</h3>
                    <div class="product-meta">
                        <span class="meta-item">库存: {{ product.stock }} 件</span>
                    </div>
                </div>

                <div class="listing-form">
                    <div class="form-group">
                        <label class="form-label">商品介绍</label>
                        <textarea v-model="product.description" placeholder="请输入商品介绍" class="form-textarea" rows="3"></textarea>
                    </div>
                    
                    <div class="form-row">
                        <div class="form-group">
                            <label class="form-label">上架数量 <span class="required">*</span></label>
                            <input type="number" v-model.number="product.listingQuantity" class="form-input" min="1" :max="product.stock" />
                        </div>
                        <div class="form-group">
                            <label class="form-label">定价 (¥) <span class="required">*</span></label>
                            <input type="number" v-model.number="product.listingPrice" class="form-input" min="0" step="0.01" placeholder="0.00" />
                        </div>
                    </div>
                    
                    <button class="btn btn-primary" @click="listProduct(product)" style="width: 100%;">上架商品</button>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const offlineProducts = ref([])

const fetchOfflineProducts = async () => {
    const userInfo = JSON.parse(sessionStorage.getItem('userInfo') || '{}')
    const response = await request.get('/mall/offline-products', { params: { userId: userInfo.id || 1 } })
    if (response.code === 200) {
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
        ElMessage.error('获取商品失败：' + response.message)
    }
}

const listProduct = async (product) => {
    if (!product.listingQuantity || product.listingQuantity <= 0) {
        ElMessage.warning('请输入有效的上架数量'); return
    }
    if (product.listingQuantity > product.stock) {
        ElMessage.warning('上架数量不能超过库存数量'); return
    }
    if (!product.listingPrice || product.listingPrice <= 0) {
        ElMessage.warning('请输入有效的定价'); return
    }

    const response = await request.post('/mall/publish', {
        productId: product.id,
        description: product.description,
        quantity: product.listingQuantity,
        price: product.listingPrice
    })
    if (response.code === 200) {
        ElMessage.success(`${product.name} 上架成功`)
        offlineProducts.value = offlineProducts.value.filter(p => p.id !== product.id)
    } else {
        ElMessage.error('上架失败：' + response.message)
    }
}

onMounted(() => { fetchOfflineProducts() })
</script>

<style scoped>
.products-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
    gap: 20px;
}

.product-card {
    background: #fff;
    border: 1px solid #eff3f4;
    border-radius: 12px;
    overflow: hidden;
    transition: border-color 0.15s, box-shadow 0.15s;
}

.product-card:hover {
    border-color: #cfd9de;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.product-image-large {
    width: 100%;
    height: 200px;
    background: #f7f9f9;
    overflow: hidden;
}

.product-image-large img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.product-info-section {
    padding: 16px 16px 0;
}

.product-info-section .product-name {
    margin: 0 0 8px 0;
    font-size: 15px;
}

.listing-form {
    padding: 12px 16px 16px;
}

.listing-form .form-group {
    margin-bottom: 12px;
}

@media (max-width: 640px) {
    .products-grid {
        grid-template-columns: 1fr;
    }
}
</style>
