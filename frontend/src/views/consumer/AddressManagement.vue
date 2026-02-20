<template>
    <div class="page-container">
        <div class="page-header">
            <div class="search-box">
                <input v-model="searchKeyword" type="text" placeholder="搜索地址..." class="search-input" @keyup.enter="handleSearch" />
            </div>
            <button @click="showAddDialog" class="btn btn-primary">添加地址</button>
        </div>

        <div class="address-list">
            <div v-if="filteredAddresses.length === 0" class="empty-state">
                {{ searchKeyword ? '没有找到匹配的地址' : '暂无收货地址' }}
            </div>

            <div v-else class="address-card" v-for="address in filteredAddresses" :key="address.id">
                <div class="address-content">
                    <div class="address-top">
                        <span class="receiver-name">{{ address.receiverName }}</span>
                        <span class="receiver-phone">{{ address.receiverPhone }}</span>
                        <span v-if="address.isDefault" class="badge">默认</span>
                    </div>
                    <div class="address-detail">
                        {{ address.province }} {{ address.city }} {{ address.district }} {{ address.detailAddress }}
                    </div>
                </div>
                <div class="address-actions">
                    <button @click="editAddress(address)" class="btn btn-ghost btn-sm">编辑</button>
                    <button @click="deleteAddress(address.id)" class="btn btn-danger btn-sm">删除</button>
                    <button v-if="!address.isDefault" @click="setDefaultAddress(address.id)" class="btn btn-ghost btn-sm">设为默认</button>
                </div>
            </div>
        </div>

        <!-- Dialog -->
        <div v-if="dialogVisible" class="dialog-overlay" @click="closeDialog">
            <div class="dialog-content" @click.stop>
                <div class="dialog-header">
                    <h3>{{ isEditMode ? '编辑地址' : '添加地址' }}</h3>
                    <button @click="closeDialog" class="close-btn">×</button>
                </div>
                <form @submit.prevent="submitForm" class="dialog-body">
                    <div class="form-row">
                        <div class="form-group">
                            <label class="form-label">收货人 <span class="required">*</span></label>
                            <input v-model="formData.receiverName" type="text" class="form-input" placeholder="姓名" required />
                        </div>
                        <div class="form-group">
                            <label class="form-label">电话 <span class="required">*</span></label>
                            <input v-model="formData.receiverPhone" type="tel" class="form-input" placeholder="手机号" pattern="[0-9]{11}" required />
                        </div>
                    </div>
                    <div class="form-row form-row-3">
                        <div class="form-group">
                            <label class="form-label">省份 <span class="required">*</span></label>
                            <input v-model="formData.province" type="text" class="form-input" placeholder="省" required />
                        </div>
                        <div class="form-group">
                            <label class="form-label">城市 <span class="required">*</span></label>
                            <input v-model="formData.city" type="text" class="form-input" placeholder="市" required />
                        </div>
                        <div class="form-group">
                            <label class="form-label">区县 <span class="required">*</span></label>
                            <input v-model="formData.district" type="text" class="form-input" placeholder="区" required />
                        </div>
                    </div>
                    <div class="form-group">
                        <label class="form-label">详细地址 <span class="required">*</span></label>
                        <textarea v-model="formData.detailAddress" class="form-textarea" placeholder="街道、门牌号" rows="2" required></textarea>
                    </div>
                    <label class="checkbox-label">
                        <input v-model="formData.isDefault" type="checkbox" />
                        <span>设为默认地址</span>
                    </label>
                    <div class="form-actions">
                        <button type="button" @click="closeDialog" class="btn btn-ghost">取消</button>
                        <button type="submit" class="btn btn-primary">保存</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import request from '@/utils/request'

const addresses = ref([])
const searchKeyword = ref('')
const dialogVisible = ref(false)
const isEditMode = ref(false)

const filteredAddresses = computed(() => {
    if (!searchKeyword.value) return addresses.value
    const kw = searchKeyword.value.toLowerCase()
    return addresses.value.filter(a =>
        a.receiverName.toLowerCase().includes(kw) ||
        a.receiverPhone.includes(kw) ||
        a.detailAddress.toLowerCase().includes(kw)
    )
})

const formData = ref({
    id: null, receiverName: '', receiverPhone: '',
    province: '', city: '', district: '', detailAddress: '', isDefault: false
})

const handleSearch = () => {}

const loadAddresses = async () => {
    try {
        const res = await request.get('/address/list')
        if (res.success) addresses.value = res.data || []
    } catch (error) {
        console.error('加载地址列表失败:', error)
    }
}

const showAddDialog = () => {
    isEditMode.value = false
    formData.value = { id: null, receiverName: '', receiverPhone: '', province: '', city: '', district: '', detailAddress: '', isDefault: false }
    dialogVisible.value = true
}

const editAddress = (address) => {
    isEditMode.value = true
    formData.value = { ...address, isDefault: address.isDefault === 1 }
    dialogVisible.value = true
}

const closeDialog = () => { dialogVisible.value = false }

const AMAP_KEY = 'a5f853c1746013a3e5ff1d3b083137e5'

const getLocation = async () => {
    try {
        const fullAddress = `${formData.value.province}${formData.value.city}${formData.value.district}${formData.value.detailAddress}`
        const response = await fetch(`https://restapi.amap.com/v3/geocode/geo?address=${encodeURIComponent(fullAddress)}&city=${encodeURIComponent(formData.value.city)}&key=${AMAP_KEY}`)
        const data = await response.json()
        if (data.status === '1' && data.geocodes?.length > 0) {
            const [lng, lat] = data.geocodes[0].location.split(',')
            return { longitude: parseFloat(lng), latitude: parseFloat(lat) }
        }
        return null
    } catch { return null }
}

const submitForm = async () => {
    try {
        const location = await getLocation()
        const addressData = {
            ...formData.value,
            isDefault: formData.value.isDefault ? 1 : 0,
            latitude: location?.latitude || null,
            longitude: location?.longitude || null
        }
        const res = isEditMode.value
            ? await request.put(`/address/${addressData.id}`, addressData)
            : await request.post('/address', addressData)
        if (res.success) { closeDialog(); loadAddresses() }
    } catch (error) {
        console.error('保存地址失败:', error)
    }
}

const deleteAddress = async (id) => {
    if (!confirm('确认删除？')) return
    try {
        const res = await request.delete(`/address/${id}`)
        if (res.success) loadAddresses()
    } catch (error) {
        console.error('删除地址失败:', error)
    }
}

const setDefaultAddress = async (id) => {
    try {
        const res = await request.put(`/address/${id}/default`)
        if (res.success) loadAddresses()
    } catch (error) {
        console.error('设置默认地址失败:', error)
    }
}

onMounted(() => { loadAddresses() })
</script>

<style scoped>
.address-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.address-card {
    background: #ffffff;
    border: 1px solid #e5e5e5;
    border-radius: 10px;
    padding: 14px 16px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    transition: border-color 0.15s;
}
.address-card:hover { border-color: #d4d4d4; }

.address-top {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 4px;
}
.receiver-name { font-size: 14px; font-weight: 600; color: #0a0a0a; }
.receiver-phone { font-size: 13px; color: #737373; }
.address-detail { font-size: 13px; color: #525252; line-height: 1.5; }
.address-actions { display: flex; gap: 6px; flex-shrink: 0; }

/* Dialog form */
.dialog-body { padding: 20px; display: flex; flex-direction: column; gap: 14px; }
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.form-row-3 { grid-template-columns: 1fr 1fr 1fr; }
.form-textarea {
    padding: 8px 12px;
    border: 1px solid #e5e5e5;
    border-radius: 8px;
    font-size: 13px;
    color: #0a0a0a;
    resize: vertical;
    font-family: inherit;
}
.form-textarea:focus { outline: none; border-color: #0a0a0a; }

.checkbox-label {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 13px;
    color: #525252;
    cursor: pointer;
}
.checkbox-label input { width: 16px; height: 16px; cursor: pointer; accent-color: #0a0a0a; }

.form-actions {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    padding-top: 12px;
    border-top: 1px solid #f5f5f5;
}
</style>
