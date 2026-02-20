<template>
    <div class="page-container">
        <h1>商品入库</h1>

        <div class="form-container">
            <div class="form-section">
                <h2>入库信息</h2>
                <div class="form-group">
                    <label class="form-label">选择仓库 <span class="required">*</span></label>
                    <select v-model="formData.warehouseId" class="form-select">
                        <option value="" disabled>请选择仓库</option>
                        <option v-for="wh in warehouses" :key="wh.id" :value="wh.id">{{ wh.name }} - {{ wh.city }}</option>
                    </select>
                </div>
                <div class="form-group">
                    <label class="form-label">商品名称 <span class="required">*</span></label>
                    <input type="text" v-model="formData.productName" placeholder="请输入商品名称" class="form-input" />
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label class="form-label">入库数量 <span class="required">*</span></label>
                        <input type="number" v-model.number="formData.quantity" placeholder="数量" class="form-input" min="1" />
                    </div>
                    <div class="form-group">
                        <label class="form-label">入库日期 <span class="required">*</span></label>
                        <input type="date" v-model="formData.date" class="form-input" />
                    </div>
                </div>
                <div class="form-group">
                    <label class="form-label">备注</label>
                    <textarea v-model="formData.note" placeholder="请输入备注信息" class="form-textarea" rows="3"></textarea>
                </div>
            </div>

            <div class="form-section">
                <h2>商品图片</h2>
                <div class="form-group">
                    <label class="form-label">上传图片</label>
                    <input type="file" @change="handleImageUpload" accept="image/*" class="form-input" />
                </div>
                <div v-if="imagePreview" style="margin-top: 12px; width: 200px; height: 200px; border: 1px solid #eff3f4; border-radius: 8px; overflow: hidden;">
                    <img :src="imagePreview" alt="预览" style="width: 100%; height: 100%; object-fit: cover;" />
                </div>
            </div>

            <div class="form-actions">
                <button class="btn btn-ghost" @click="$router.push('/merchant/inventory-management')">取消</button>
                <button class="btn btn-primary" @click="handleSubmit">确认入库</button>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const router = useRouter()

const formData = ref({
    warehouseId: '',
    productName: '',
    quantity: null,
    date: new Date().toISOString().split('T')[0],
    note: '',
    imageFile: null
})

const warehouses = ref([])
const imagePreview = ref('')

const fetchWarehouses = async () => {
    const response = await request.get('/warehouse/list')
    if (response.success) {
        warehouses.value = response.data
    }
}

const handleImageUpload = (event) => {
    const file = event.target.files[0]
    if (!file) return
    if (!file.type.startsWith('image/')) { ElMessage.warning('请选择图片文件'); return }
    if (file.size > 5 * 1024 * 1024) { ElMessage.warning('图片大小不能超过5MB'); return }

    formData.value.imageFile = file
    const reader = new FileReader()
    reader.onload = (e) => { imagePreview.value = e.target.result }
    reader.readAsDataURL(file)
}

const handleSubmit = async () => {
    if (!formData.value.warehouseId) { ElMessage.warning('请选择仓库'); return }
    if (!formData.value.productName || !formData.value.quantity || !formData.value.date) { ElMessage.warning('请填写必填项'); return }
    if (!formData.value.imageFile) { ElMessage.warning('请上传商品图片'); return }

    const userInfo = JSON.parse(sessionStorage.getItem('userInfo') || '{}')
    const submitData = new FormData()
    submitData.append('userId', userInfo.id || 1)
    submitData.append('warehouseId', formData.value.warehouseId)
    submitData.append('productName', formData.value.productName)
    submitData.append('quantity', formData.value.quantity)
    submitData.append('stockInDate', formData.value.date + ' 00:00:00')
    submitData.append('image', formData.value.imageFile)

    const response = await request.post('/inventory/stock-in', submitData, { headers: { 'Content-Type': 'multipart/form-data' } })
    if (response.code === 200) {
        ElMessage.success(`${formData.value.productName} 入库成功`)
        router.push('/merchant/inventory-management')
    } else {
        ElMessage.error('入库失败：' + (response.message || '未知错误'))
    }
}

onMounted(() => { fetchWarehouses() })
</script>

<style scoped>
/* All shared classes come from design.css */
</style>
