<template>
    <div class="page-container">
        <h1>商品下架</h1>

        <div v-if="onlineProducts.length === 0" class="empty-state">
            <p>暂无已上架商品</p>
        </div>

        <div v-else style="display: flex; flex-direction: column; gap: 12px;">
            <div class="product-item" v-for="product in onlineProducts" :key="product.id">
                <div class="product-image" style="width: 100px; height: 100px;">
                    <img :src="product.image" :alt="product.name" />
                </div>
                <div style="flex: 1;">
                    <h4 class="product-name">{{ product.name }}</h4>
                    <p class="product-description">{{ product.description }}</p>
                    <div class="product-meta">
                        <span class="meta-item">库存: {{ product.stock }} 件</span>
                        <span class="meta-item">¥{{ product.price }}</span>
                    </div>
                </div>
                <button class="btn btn-danger btn-sm" @click="delistProduct(product)">下架</button>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

// TODO: 从后端获取已上架商品列表（目前是模拟数据）
const onlineProducts = ref([
    { id: 1, name: 'Apple MacBook Pro 14', description: 'M3 Pro芯片，18GB内存', stock: 15, price: 15999, image: 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=200&h=200&fit=crop' },
    { id: 2, name: 'Sony WH-1000XM5 耳机', description: '业界领先降噪，30小时续航', stock: 28, price: 2499, image: 'https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=200&h=200&fit=crop' },
    { id: 3, name: 'iPhone 15 Pro Max', description: 'A17 Pro芯片，钛金属边框', stock: 42, price: 9999, image: 'https://images.unsplash.com/photo-1592286927505-2fd0ee3df564?w=200&h=200&fit=crop' }
])

const delistProduct = async (product) => {
    try {
        await ElMessageBox.confirm(`确定要下架商品【${product.name}】吗？`, '确认', { confirmButtonText: '下架', cancelButtonText: '取消', type: 'warning' })
    } catch { return }

    // TODO: 调用后端 API
    onlineProducts.value = onlineProducts.value.filter(p => p.id !== product.id)
    ElMessage.success(`${product.name} 已下架`)

    if (onlineProducts.value.length === 0) {
        setTimeout(() => router.push('/merchant/inventory-management'), 1000)
    }
}
</script>

<style scoped>
/* All shared classes come from design.css */
</style>
