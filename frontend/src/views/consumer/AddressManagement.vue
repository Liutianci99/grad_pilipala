<template>
    <div class="page-container">
        <!-- 搜索栏和添加按钮 -->
        <div class="page-header">
            <div class="search-box">
                <input 
                    v-model="searchKeyword" 
                    type="text" 
                    placeholder="搜索收货人、电话或地址..."
                    class="search-input"
                    @keyup.enter="handleSearch"
                />
                <button @click="handleSearch" class="search-btn">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="11" cy="11" r="8"/>
                        <path d="m21 21-4.35-4.35"/>
                    </svg>
                    搜索
                </button>
            </div>
            <button @click="showAddDialog" class="add-btn">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="12" y1="5" x2="12" y2="19"/>
                    <line x1="5" y1="12" x2="19" y2="12"/>
                </svg>
                添加新地址
            </button>
        </div>

        <!-- 地址列表 -->
        <div class="address-list">
            <div v-if="filteredAddresses.length === 0" class="empty-state">
                <p>{{ searchKeyword ? '没有找到匹配的地址' : '暂无收货地址，请添加新地址' }}</p>
            </div>

            <div v-else class="address-item" v-for="address in filteredAddresses" :key="address.id">
                <div class="address-content">
                    <div class="address-header">
                        <span class="receiver-name">{{ address.receiverName }}</span>
                        <span class="receiver-phone">{{ address.receiverPhone }}</span>
                        <span v-if="address.isDefault" class="default-badge">默认</span>
                    </div>
                    <div class="address-detail">
                        {{ address.province }} {{ address.city }} {{ address.district }} {{ address.detailAddress }}
                    </div>
                </div>
                <div class="address-actions">
                    <button @click="editAddress(address)" class="action-btn edit-btn">编辑</button>
                    <button @click="deleteAddress(address.id)" class="action-btn delete-btn">删除</button>
                    <button 
                        v-if="!address.isDefault" 
                        @click="setDefaultAddress(address.id)" 
                        class="action-btn default-btn"
                    >
                        设为默认
                    </button>
                </div>
            </div>
        </div>

        <!-- 添加/编辑地址对话框 -->
        <div v-if="dialogVisible" class="dialog-overlay" @click="closeDialog">
            <div class="dialog-content" @click.stop>
                <div class="dialog-header">
                    <h3>{{ isEditMode ? '编辑地址' : '添加地址' }}</h3>
                    <button @click="closeDialog" class="close-btn">&times;</button>
                </div>
                
                <form @submit.prevent="submitForm" class="address-form">
                    <div class="form-row">
                        <div class="form-group">
                            <label>收货人 <span class="required">*</span></label>
                            <input 
                                v-model="formData.receiverName" 
                                type="text" 
                                placeholder="请输入收货人姓名"
                                required
                            />
                        </div>
                        <div class="form-group">
                            <label>联系电话 <span class="required">*</span></label>
                            <input 
                                v-model="formData.receiverPhone" 
                                type="tel" 
                                placeholder="请输入联系电话"
                                pattern="[0-9]{11}"
                                required
                            />
                        </div>
                    </div>

                    <div class="form-row">
                        <div class="form-group">
                            <label>省份 <span class="required">*</span></label>
                            <input 
                                v-model="formData.province" 
                                type="text" 
                                placeholder="请输入省份"
                                required
                            />
                        </div>
                        <div class="form-group">
                            <label>城市 <span class="required">*</span></label>
                            <input 
                                v-model="formData.city" 
                                type="text" 
                                placeholder="请输入城市"
                                required
                            />
                        </div>
                        <div class="form-group">
                            <label>区/县 <span class="required">*</span></label>
                            <input 
                                v-model="formData.district" 
                                type="text" 
                                placeholder="请输入区/县"
                                required
                            />
                        </div>
                    </div>

                    <div class="form-group full-width">
                        <label>详细地址 <span class="required">*</span></label>
                        <textarea 
                            v-model="formData.detailAddress" 
                            placeholder="请输入详细地址（街道、门牌号等）"
                            rows="3"
                            required
                        ></textarea>
                    </div>

                    <div class="form-group checkbox-group">
                        <label>
                            <input 
                                v-model="formData.isDefault" 
                                type="checkbox"
                            />
                            设为默认地址
                        </label>
                    </div>

                    <div class="form-actions">
                        <button type="button" @click="closeDialog" class="cancel-btn">取消</button>
                        <button type="submit" class="submit-btn">保存</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import request from '@/utils/request'

// 地址列表
const addresses = ref([])

// 搜索关键词
const searchKeyword = ref('')

// 过滤后的地址列表
const filteredAddresses = computed(() => {
    if (!searchKeyword.value) {
        return addresses.value
    }
    const keyword = searchKeyword.value.toLowerCase()
    return addresses.value.filter(addr => {
        return addr.receiverName.toLowerCase().includes(keyword) ||
               addr.receiverPhone.includes(keyword) ||
               addr.province.toLowerCase().includes(keyword) ||
               addr.city.toLowerCase().includes(keyword) ||
               addr.district.toLowerCase().includes(keyword) ||
               addr.detailAddress.toLowerCase().includes(keyword)
    })
})

// 对话框显示控制
const dialogVisible = ref(false)
const isEditMode = ref(false)

// 表单数据
const formData = ref({
    id: null,
    receiverName: '',
    receiverPhone: '',
    province: '',
    city: '',
    district: '',
    detailAddress: '',
    isDefault: false
})

// 搜索函数
const handleSearch = () => {
    console.log('搜索关键词:', searchKeyword.value)
    // 搜索功能由 computed 属性 filteredAddresses 自动实现
}

// 加载地址列表
const loadAddresses = async () => {
    try {
        console.log('=== 开始加载地址列表 ===')
        console.log('Token:', sessionStorage.getItem('token'))
        
        const res = await request.get('/address/list')
        console.log('后端返回完整数据:', JSON.stringify(res, null, 2))
        
        if (res.success) {
            console.log('加载成功，地址数据:', res.data)
            console.log('地址数量:', res.data ? res.data.length : 0)
            addresses.value = res.data || []
            console.log('addresses.value 已更新，当前值:', addresses.value)
            console.log('filteredAddresses 计算结果:', filteredAddresses.value)
        } else {
            console.error('加载地址列表失败:', res.message)
            alert(res.message || '加载地址列表失败')
        }
    } catch (error) {
        console.error('加载地址列表失败，错误详情:', error)
        alert('加载地址列表失败')
    }
}

// 显示添加对话框
const showAddDialog = () => {
    isEditMode.value = false
    formData.value = {
        id: null,
        receiverName: '',
        receiverPhone: '',
        province: '',
        city: '',
        district: '',
        detailAddress: '',
        isDefault: false
    }
    dialogVisible.value = true
}

// 编辑地址
const editAddress = (address) => {
    isEditMode.value = true
    formData.value = {
        id: address.id,
        receiverName: address.receiverName,
        receiverPhone: address.receiverPhone,
        province: address.province,
        city: address.city,
        district: address.district,
        detailAddress: address.detailAddress,
        isDefault: address.isDefault === 1
    }
    dialogVisible.value = true
}

// 关闭对话框
const closeDialog = () => {
    dialogVisible.value = false
}

// 高德地图API Key
const AMAP_KEY = 'a5f853c1746013a3e5ff1d3b083137e5'

// 调用高德地图API获取经纬度
const getLocation = async () => {
    try {
        const fullAddress = `${formData.value.province}${formData.value.city}${formData.value.district}${formData.value.detailAddress}`
        console.log('正在获取地址经纬度:', fullAddress)
        
        const response = await fetch(
            `https://restapi.amap.com/v3/geocode/geo?address=${encodeURIComponent(fullAddress)}&city=${encodeURIComponent(formData.value.city)}&key=${AMAP_KEY}`
        )
        const data = await response.json()
        
        console.log('高德地图API响应:', data)
        
        if (data.status === '1' && data.geocodes && data.geocodes.length > 0) {
            const location = data.geocodes[0].location.split(',')
            const result = {
                longitude: parseFloat(location[0]),
                latitude: parseFloat(location[1])
            }
            console.log('获取到经纬度:', result)
            return result
        } else {
            console.warn('地理编码失败:', data.info)
            return null
        }
    } catch (error) {
        console.error('获取经纬度失败:', error)
        return null
    }
}

// 提交表单
const submitForm = async () => {
    try {
        console.log('开始提交表单，表单数据:', formData.value)
        
        // 调用高德地图API获取经纬度
        const location = await getLocation()
        
        const addressData = {
            ...formData.value,
            isDefault: formData.value.isDefault ? 1 : 0,
            latitude: location?.latitude || null,
            longitude: location?.longitude || null
        }
        
        console.log('准备发送的地址数据:', addressData)

        let res
        if (isEditMode.value) {
            // 编辑模式 - 更新地址
            console.log('更新地址，ID:', addressData.id)
            res = await request.put(`/address/${addressData.id}`, addressData)
        } else {
            // 添加模式 - 新建地址
            console.log('添加新地址')
            res = await request.post('/address', addressData)
        }
        
        console.log('后端响应:', res)

        if (res.success) {
            alert(isEditMode.value ? '地址更新成功' : '地址添加成功')
            closeDialog()
            loadAddresses()
        } else {
            alert(res.message || '操作失败')
        }
    } catch (error) {
        console.error('保存地址失败，错误详情:', error)
        alert('保存地址失败: ' + (error.message || '未知错误'))
    }
}

// 删除地址
const deleteAddress = async (id) => {
    if (!confirm('确认删除该地址吗？')) {
        return
    }

    try {
        const res = await request.delete(`/address/${id}`)
        if (res.success) {
            alert('地址删除成功')
            loadAddresses()
        } else {
            alert(res.message || '删除地址失败')
        }
    } catch (error) {
        console.error('删除地址失败:', error)
        alert('删除地址失败')
    }
}

// 设为默认地址
const setDefaultAddress = async (id) => {
    try {
        const res = await request.put(`/address/${id}/default`)
        if (res.success) {
            alert('设置默认地址成功')
            loadAddresses()
        } else {
            alert(res.message || '设置默认地址失败')
        }
    } catch (error) {
        console.error('设置默认地址失败:', error)
        alert('设置默认地址失败')
    }
}

onMounted(() => {
    loadAddresses()
})
</script>

<style scoped>
.page-container {
    padding: 24px;
    max-width: 1200px;
    margin: 0 auto;
}

.page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 16px;
    margin-bottom: 24px;
}

.search-box {
    flex: 1;
    max-width: 400px;
    display: flex;
    gap: 8px;
}

.search-input {
    flex: 1;
    height: 40px;
    padding: 0 16px;
    border: 1px solid #e5e7eb;
    border-radius: 6px;
    font-size: 14px;
    transition: border-color 0.2s;
}

.search-input:focus {
    outline: none;
    border-color: #3b82f6;
}

.search-input::placeholder {
    color: #9ca3af;
}

.search-btn {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 0 16px;
    height: 40px;
    background-color: #ffffff;
    border: 1px solid #e5e7eb;
    color: #374151;
    border-radius: 6px;
    font-size: 14px;
    cursor: pointer;
    transition: all 0.2s;
    white-space: nowrap;
}

.search-btn:hover {
    background-color: #f9fafb;
    border-color: #d1d5db;
}

.add-btn {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 16px;
    background-color: #ffffff;
    border: 1px solid #e5e7eb;
    color: #374151;
    border-radius: 6px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
}

.add-btn:hover {
    background-color: #f9fafb;
    border-color: #d1d5db;
}

.address-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.empty-state {
    text-align: center;
    padding: 60px 20px;
    color: #9ca3af;
    font-size: 16px;
}

.address-item {
    background: white;
    border: 1px solid #e5e7eb;
    border-radius: 12px;
    padding: 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    transition: box-shadow 0.2s;
}

.address-item:hover {
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.address-content {
    flex: 1;
}

.address-header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 8px;
}

.receiver-name {
    font-size: 16px;
    font-weight: 600;
    color: #1f2937;
}

.receiver-phone {
    font-size: 14px;
    color: #6b7280;
}

.default-badge {
    padding: 2px 8px;
    background-color: #ffffff;
    border: 1px solid #e5e7eb;
    color: #374151;
    border-radius: 4px;
    font-size: 12px;
    font-weight: 500;
}

.address-detail {
    font-size: 14px;
    color: #4b5563;
    line-height: 1.6;
}

.address-actions {
    display: flex;
    gap: 8px;
}

.action-btn {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 8px 16px;
    background-color: #ffffff;
    border: 1px solid #e5e7eb;
    color: #374151;
    border-radius: 6px;
    font-size: 14px;
    cursor: pointer;
    transition: all 0.2s;
}

.action-btn:hover {
    background-color: #f9fafb;
    border-color: #d1d5db;
}

/* 对话框样式 */
.dialog-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
}

.dialog-content {
    background: white;
    border-radius: 12px;
    width: 90%;
    max-width: 600px;
    max-height: 90vh;
    overflow-y: auto;
}

.dialog-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20px 24px;
    border-bottom: 1px solid #e5e7eb;
}

.dialog-header h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
    color: #1f2937;
}

.close-btn {
    background: none;
    border: none;
    font-size: 28px;
    color: #9ca3af;
    cursor: pointer;
    line-height: 1;
    padding: 0;
    width: 32px;
    height: 32px;
}

.close-btn:hover {
    color: #4b5563;
}

.address-form {
    padding: 24px;
}

.form-row {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
    gap: 16px;
    margin-bottom: 16px;
}

.form-group {
    display: flex;
    flex-direction: column;
}

.form-group.full-width {
    margin-bottom: 16px;
}

.form-group label {
    font-size: 14px;
    font-weight: 500;
    color: #374151;
    margin-bottom: 8px;
}

.required {
    color: #ef4444;
}

.form-group input,
.form-group textarea {
    padding: 10px 12px;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    font-size: 14px;
    transition: border-color 0.2s;
}

.form-group input:focus,
.form-group textarea:focus {
    outline: none;
    border-color: #3b82f6;
}

.checkbox-group label {
    flex-direction: row;
    align-items: center;
    gap: 8px;
    cursor: pointer;
}

.checkbox-group input[type="checkbox"] {
    width: 18px;
    height: 18px;
    cursor: pointer;
}

.form-actions {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    margin-top: 24px;
    padding-top: 20px;
    border-top: 1px solid #e5e7eb;
}

.cancel-btn,
.submit-btn {
    padding: 8px 16px;
    background-color: #ffffff;
    border: 1px solid #e5e7eb;
    color: #374151;
    border-radius: 6px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
}

.cancel-btn:hover,
.submit-btn:hover {
    background-color: #f9fafb;
    border-color: #d1d5db;
}
</style>
