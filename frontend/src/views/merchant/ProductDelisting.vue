<template>
    <div class="page-container">
        <h1>商品下架</h1>
        
        <!-- 已上架商品列表 -->
        <div class="product-list">
            <div class="product-item" v-for="product in onlineProducts" :key="product.id">
                <div class="product-image">
                    <img :src="product.image" :alt="product.name" />
                </div>
                <div class="product-info">
                    <h3 class="product-name">{{ product.name }}</h3>
                    <p class="product-description">{{ product.description }}</p>
                    <div class="product-meta">
                        <span class="meta-item">当前库存: {{ product.stock }} 件</span>
                        <span class="meta-item">当前售价: ¥{{ product.price }}</span>
                    </div>
                </div>
                <div class="product-actions">
                    <button @click="delistProduct(product)" class="delisting-btn">下架</button>
                </div>
            </div>
        </div>
        
        <div v-if="onlineProducts.length === 0" class="empty-state">
            <p>暂无已上架商品</p>
        </div>
    </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// 模拟已上架的商品数据（从库存管理页获取）
const onlineProducts = ref([
    {
        id: 1,
        name: 'Apple MacBook Pro 14',
        description: 'M3 Pro芯片，18GB内存，512GB存储，Liquid Retina XDR显示屏',
        stock: 15,
        price: 15999,
        status: 'online',
        image: 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=200&h=200&fit=crop'
    },
    {
        id: 2,
        name: 'Sony WH-1000XM5 耳机',
        description: '业界领先降噪，30小时续航，Hi-Res音质，舒适佩戴',
        stock: 28,
        price: 2499,
        status: 'online',
        image: 'https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=200&h=200&fit=crop'
    },
    {
        id: 3,
        name: 'iPhone 15 Pro Max',
        description: 'A17 Pro芯片，钛金属边框，4800万像素主摄，USB-C接口',
        stock: 42,
        price: 9999,
        status: 'online',
        image: 'https://images.unsplash.com/photo-1592286927505-2fd0ee3df564?w=200&h=200&fit=crop'
    }
])

const delistProduct = (product) => {
    if (confirm(`确定要下架商品【${product.name}】吗？`)) {
        alert(`商品【${product.name}】已成功下架！`)
        // 这里可以调用API更新商品状态
        // 暂时从列表中移除
        const index = onlineProducts.value.findIndex(p => p.id === product.id)
        if (index > -1) {
            onlineProducts.value.splice(index, 1)
        }
        
        // 如果没有商品了，可以返回库存管理页
        if (onlineProducts.value.length === 0) {
            setTimeout(() => {
                router.push('/merchant/inventory-management')
            }, 1000)
        }
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

.product-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.product-item {
    display: flex;
    align-items: center;
    gap: 20px;
    padding: 20px;
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    transition: box-shadow 0.2s;
}

.product-item:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.product-image {
    width: 120px;
    height: 120px;
    flex-shrink: 0;
    border-radius: 8px;
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
    display: inline-flex;
    align-items: center;
    padding: 4px 12px;
    background: #f3f4f6;
    border-radius: 4px;
    color: #374151;
    font-size: 13px;
}

.product-actions {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-shrink: 0;
}

.delisting-btn {
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

.delisting-btn:hover {
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
</style>
