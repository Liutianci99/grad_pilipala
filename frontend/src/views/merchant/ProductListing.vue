<template>
    <div class="page-container">
        <h1>商品上架</h1>

        <div v-if="offlineProducts.length === 0" class="empty-state">
            <p>暂无未上架商品</p>
        </div>

        <div v-else style="display: flex; flex-direction: column; gap: 16px;">
            <div class="product-item" v-for="product in offlineProducts" :key="product.id">
                <div class="product-image" style="width: 100px; height: 100px;">
                    <img :src="product.image" :alt="product.name" />
                </div>
                <div style="flex: 1;">
                    <h4 class="product-name">{{ product.name }}</h4>
                    <p class="product-description">{{ product.description }}</p>
                    <div class="product-meta">
                        <span class="meta-item">库存: {{ product.stock }} 件</span>
                    </div>
                </div>
                <div style="display: flex; flex-direction: column; gap: 8px; min-width: 280px;">
                    <div class="form-group">
                        <label class="form-label">商品介绍</label>
                        <textarea v-model="product.description" placeholder="请输入商品介绍" class="form-textarea" rows="2"></textarea>
                    </div>
                    <div class="form-row">
                        <div class="form-group">
                            <label class="form-label">上架数量</label>
                            <input type="number" v-model.number="product.listingQuantity" class="form-input" min="1" :max="product.stock" />
                        </div>
                        <div class="form-group">
                            <label class="form-label">定价 (¥)</label>
                            <input type="number" v-model.number="product.listingPrice" class="form-input" min="0" step="0.01" />
                        </div>
                    </div>
                    <button class="btn btn-primary" @click="listProduct(product)">上架</button>
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
/* All shared classes come from design.css */
</style>
