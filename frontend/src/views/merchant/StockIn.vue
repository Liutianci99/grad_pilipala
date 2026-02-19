<template>
    <div class="page-container">
        <h1>商品入库</h1>
        
        <!-- 商品入库表单 -->
        <div class="form-container">
            <div class="form-section">
                <h2>入库信息</h2>
                <div class="form-group">
                    <label>选择仓库 <span class="required">*</span></label>
                    <select v-model="formData.warehouseId" class="form-select">
                        <option value="" disabled>请选择仓库</option>
                        <option v-for="warehouse in warehouses" :key="warehouse.id" :value="warehouse.id">
                            {{ warehouse.name }} - {{ warehouse.city }}
                        </option>
                    </select>
                </div>
                <div class="form-group">
                    <label>商品名称 <span class="required">*</span></label>
                    <input type="text" v-model="formData.productName" placeholder="请输入商品名称" class="form-input" />
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label>入库数量 <span class="required">*</span></label>
                        <input type="number" v-model.number="formData.quantity" placeholder="请输入入库数量" class="form-input" min="1" />
                    </div>
                    <div class="form-group">
                        <label>入库日期 <span class="required">*</span></label>
                        <input type="date" v-model="formData.date" class="form-input" />
                    </div>
                </div>
                <div class="form-group">
                    <label>备注</label>
                    <textarea v-model="formData.note" placeholder="请输入备注信息" class="form-textarea" rows="4"></textarea>
                </div>
            </div>

            <div class="form-section">
                <h2>商品图片</h2>
                <div class="form-group">
                    <label>上传图片</label>
                    <input 
                        type="file" 
                        @change="handleImageUpload" 
                        accept="image/*"
                        class="form-input-file"
                    />
                </div>
                <div class="image-preview" v-if="imagePreview">
                    <img :src="imagePreview" alt="商品图片预览" />
                </div>
            </div>

            <div class="form-actions">
                <button @click="handleSubmit" class="submit-btn">确认入库</button>
                <button @click="handleCancel" class="cancel-btn">取消</button>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '../../utils/request'

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

// 获取仓库列表
const fetchWarehouses = async () => {
    try {
        const response = await request.get('/warehouse/list')
        if (response.success) {
            warehouses.value = response.data
        } else {
            console.error('获取仓库列表失败:', response.message)
        }
    } catch (error) {
        console.error('获取仓库列表失败:', error)
    }
}

// 页面加载时获取仓库列表
onMounted(() => {
    fetchWarehouses()
})

// 处理图片上传
const handleImageUpload = (event) => {
    const file = event.target.files[0]
    if (file) {
        // 验证文件类型
        if (!file.type.startsWith('image/')) {
            alert('请选择图片文件')
            return
        }
        
        // 验证文件大小（例如限制在5MB以内）
        if (file.size > 5 * 1024 * 1024) {
            alert('图片大小不能超过5MB')
            return
        }
        
        // 保存文件对象
        formData.value.imageFile = file
        
        // 生成预览
        const reader = new FileReader()
        reader.onload = (e) => {
            imagePreview.value = e.target.result
        }
        reader.readAsDataURL(file)
    }
}

const handleSubmit = async () => {
    // 验证表单
    if (!formData.value.warehouseId) {
        alert('请选择仓库')
        return
    }
    
    if (!formData.value.productName || !formData.value.quantity || !formData.value.date) {
        alert('请填写必填项')
        return
    }
    
    if (!formData.value.imageFile) {
        alert('请上传商品图片')
        return
    }
    
    try {
        // 准备FormData对象用于文件上传
        const submitData = new FormData()
        
        // 获取登录用户ID（从sessionStorage）
        const userInfo = JSON.parse(sessionStorage.getItem('userInfo') || '{}')
        const userId = userInfo.id || 1 // 如果没有用户信息，默认使用1
        
        submitData.append('userId', userId)
        submitData.append('warehouseId', formData.value.warehouseId)
        submitData.append('productName', formData.value.productName)
        submitData.append('quantity', formData.value.quantity)
        
        // 将日期转换为 yyyy-MM-dd HH:mm:ss 格式
        const stockInDate = formData.value.date + ' 00:00:00'
        submitData.append('stockInDate', stockInDate)
        submitData.append('image', formData.value.imageFile)
        
        // 调用后端API
        const response = await request.post('/inventory/stock-in', submitData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        })
        
        if (response.code === 200) {
            alert(`入库成功！\n商品：${formData.value.productName}\n入库数量：${formData.value.quantity}件\n入库日期：${formData.value.date}`)
            router.push('/merchant/inventory-management')
        } else {
            alert('入库失败：' + (response.message || '未知错误'))
        }
    } catch (error) {
        console.error('入库失败', error)
        alert('入库失败：' + (error.response?.data?.message || error.message || '网络错误'))
    }
}

const handleCancel = () => {
    router.push('/merchant/inventory-management')
}
</script>

<style scoped>
.page-container {
    width: 100%;
    max-width: 800px;
}

h1 {
    margin: 0 0 24px 0;
    color: #1f2937;
    font-size: 28px;
}

.form-container {
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    padding: 24px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.form-section {
    margin-bottom: 32px;
}

.form-section:last-of-type {
    margin-bottom: 24px;
}

h2 {
    margin: 0 0 16px 0;
    color: #1f2937;
    font-size: 18px;
    font-weight: 600;
    padding-bottom: 8px;
    border-bottom: 2px solid #e5e7eb;
}

.form-group {
    margin-bottom: 16px;
}

.form-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
}

label {
    display: block;
    margin-bottom: 8px;
    color: #374151;
    font-size: 14px;
    font-weight: 500;
}

.required {
    color: #ef4444;
}

.form-input,
.form-input,
.form-select {
    width: 100%;
    padding: 10px 12px;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    font-size: 14px;
    color: #374151;
    transition: border-color 0.2s;
    background: #ffffff;
}

.form-input:focus,
.form-select:focus {
    outline: none;
    border-color: #3b82f6;
}
.form-input-file {
    width: 100%;
    padding: 8px 12px;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    font-size: 14px;
    color: transparent;
    background: #ffffff;
    cursor: pointer;
    transition: border-color 0.2s;
}

.form-input-file:focus {
    outline: none;
    border-color: #3b82f6;
}

.form-input-file::-webkit-file-upload-button {
    padding: 6px 12px;
    background: #f9fafb;
    border: 1px solid #d1d5db;
    border-radius: 4px;
    color: #374151;
    font-size: 14px;
    cursor: pointer;
    transition: all 0.2s;
}

.form-input-file::-webkit-file-upload-button:hover {
    background: #f3f4f6;
}
.form-textarea {
    width: 100%;
    padding: 10px 12px;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    font-size: 14px;
    color: #374151;
    resize: vertical;
    transition: border-color 0.2s;
}

.form-textarea:focus {
    outline: none;
    border-color: #3b82f6;
}

.image-preview {
    margin-top: 16px;
    width: 200px;
    height: 200px;
    border: 1px solid #e5e7eb;
    border-radius: 6px;
    overflow: hidden;
    background: #f3f4f6;
}

.image-preview img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.form-actions {
    display: flex;
    gap: 12px;
    justify-content: flex-end;
    padding-top: 16px;
    border-top: 1px solid #e5e7eb;
}

.submit-btn {
    padding: 10px 24px;
    background: #ffffff;
    color: #374151;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
}

.submit-btn:hover {
    background: #f9fafb;
    border-color: #9ca3af;
}

.cancel-btn {
    padding: 10px 24px;
    background: #ffffff;
    color: #374151;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
}

.cancel-btn:hover {
    background: #f9fafb;
    border-color: #9ca3af;
}

@media (max-width: 768px) {
    .form-row {
        grid-template-columns: 1fr;
    }
}
</style>
