<template>
    <div class="login-container">
        <div class="login-box">
            <div class="login-header">
                <h1 class="brand">物流管理</h1>
                <p class="subtitle">创建新账号</p>
            </div>

            <form class="login-form" @submit.prevent="handleRegister">
                <div class="form-group">
                    <label class="form-label">身份</label>
                    <select v-model="form.role" class="form-select" @change="handleRoleChange">
                        <option value="">请选择身份</option>
                        <option value="merchant">商家</option>
                        <option value="consumer">顾客</option>
                        <option value="driver">配送员</option>
                    </select>
                </div>

                <div v-if="form.role === 'driver'" class="form-group">
                    <label class="form-label">所属仓库 <span style="color: #f4212e;">*</span></label>
                    <select v-model="form.warehouseId" class="form-select">
                        <option value="">请选择仓库</option>
                        <option v-for="wh in warehouses" :key="wh.id" :value="wh.id">{{ wh.name }} - {{ wh.city }}</option>
                    </select>
                </div>

                <div class="form-group">
                    <label class="form-label">用户名</label>
                    <input v-model="form.username" type="text" class="form-input" placeholder="请输入用户名" />
                </div>

                <div class="form-group">
                    <label class="form-label">密码</label>
                    <input v-model="form.password" type="password" class="form-input" placeholder="请输入密码" />
                </div>

                <div class="form-group">
                    <label class="form-label">确认密码</label>
                    <input v-model="form.confirmPassword" type="password" class="form-input" placeholder="请再次输入密码" />
                </div>

                <template v-if="form.role === 'consumer'">
                    <div class="section-divider">
                        <span class="section-title">默认收货地址</span>
                    </div>
                    <div class="form-row">
                        <div class="form-group">
                            <label class="form-label">收货人 <span style="color: #f4212e;">*</span></label>
                            <input v-model="form.receiverName" type="text" class="form-input" placeholder="姓名" />
                        </div>
                        <div class="form-group">
                            <label class="form-label">电话 <span style="color: #f4212e;">*</span></label>
                            <input v-model="form.receiverPhone" type="tel" class="form-input" placeholder="手机号" />
                        </div>
                    </div>
                    <div class="form-row form-row-3">
                        <div class="form-group">
                            <label class="form-label">省份 <span style="color: #f4212e;">*</span></label>
                            <input v-model="form.province" type="text" class="form-input" placeholder="省" />
                        </div>
                        <div class="form-group">
                            <label class="form-label">城市 <span style="color: #f4212e;">*</span></label>
                            <input v-model="form.city" type="text" class="form-input" placeholder="市" />
                        </div>
                        <div class="form-group">
                            <label class="form-label">区县 <span style="color: #f4212e;">*</span></label>
                            <input v-model="form.district" type="text" class="form-input" placeholder="区" />
                        </div>
                    </div>
                    <div class="form-group">
                        <label class="form-label">详细地址 <span style="color: #f4212e;">*</span></label>
                        <input v-model="form.detailAddress" type="text" class="form-input" placeholder="街道、门牌号" />
                    </div>
                </template>

                <button type="submit" class="sign-in-btn" :disabled="loading">
                    {{ loading ? '注册中...' : '注册' }}
                </button>
            </form>
        </div>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const router = useRouter()
const warehouses = ref([])

const form = reactive({
    role: '',
    username: '',
    password: '',
    confirmPassword: '',
    warehouseId: '',
    receiverName: '',
    receiverPhone: '',
    province: '',
    city: '',
    district: '',
    detailAddress: ''
})

const handleRoleChange = () => {
    form.warehouseId = ''
    form.receiverName = ''
    form.receiverPhone = ''
    form.province = ''
    form.city = ''
    form.district = ''
    form.detailAddress = ''
}

const fetchWarehouses = async () => {
    try {
        const response = await request.get('/warehouse/list')
        if (response.success) {
            warehouses.value = response.data
        }
    } catch (e) {
        console.error('Failed to fetch warehouses:', e)
    }
}

const AMAP_KEY = 'a5f853c1746013a3e5ff1d3b083137e5'

const getLocation = async () => {
    try {
        const fullAddress = `${form.province}${form.city}${form.district}${form.detailAddress}`
        const response = await fetch(`https://restapi.amap.com/v3/geocode/geo?address=${encodeURIComponent(fullAddress)}&city=${encodeURIComponent(form.city)}&key=${AMAP_KEY}`)
        const data = await response.json()
        if (data.status === '1' && data.geocodes?.length > 0) {
            const [lng, lat] = data.geocodes[0].location.split(',')
            return { longitude: parseFloat(lng), latitude: parseFloat(lat) }
        }
        return null
    } catch { return null }
}

const handleRegister = async () => {
    if (!form.role) return ElMessage.warning('请选择身份')
    if (form.role === 'driver' && !form.warehouseId) return ElMessage.warning('请选择所属仓库')
    if (!form.username) return ElMessage.warning('请输入用户名')
    if (!form.password) return ElMessage.warning('请输入密码')
    if (form.password !== form.confirmPassword) return ElMessage.warning('两次密码不一致')

    if (form.role === 'consumer') {
        if (!form.receiverName) return ElMessage.warning('请输入收货人姓名')
        if (!form.receiverPhone) return ElMessage.warning('请输入收货人电话')
        if (!form.province || !form.city || !form.district) return ElMessage.warning('请填写完整的省市区')
        if (!form.detailAddress) return ElMessage.warning('请输入详细地址')
    }

    loading.value = true
    try {
        const payload = {
            username: form.username,
            password: form.password,
            role: form.role
        }
        if (form.role === 'driver') {
            payload.warehouseId = form.warehouseId
        }

        const data = await request.post('/auth/register', payload)

        if (!data.success) {
            ElMessage.error(data.message || '注册失败')
            return
        }

        if (form.role === 'consumer' && data.data?.token) {
            sessionStorage.setItem('token', data.data.token)
            const location = await getLocation()
            await request.post('/address', {
                receiverName: form.receiverName,
                receiverPhone: form.receiverPhone,
                province: form.province,
                city: form.city,
                district: form.district,
                detailAddress: form.detailAddress,
                isDefault: 1,
                latitude: location?.latitude || null,
                longitude: location?.longitude || null
            })
            sessionStorage.clear()
        }

        ElMessage.success('注册成功，请登录')
        await router.push('/')
    } catch (e) { /* handled */ } finally {
        loading.value = false
    }
}

onMounted(() => {
    fetchWarehouses()
})
</script>

<style scoped>
.login-container {
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #fafafa;
    padding: 20px;
}

.login-box {
    background: #ffffff;
    border: 1px solid #e5e5e5;
    border-radius: 12px;
    padding: 40px;
    width: 100%;
    max-width: 480px;
}

.login-header {
    text-align: center;
    margin-bottom: 32px;
}

.brand {
    font-size: 24px;
    font-weight: 600;
    color: #0a0a0a;
    margin: 0 0 6px 0;
    letter-spacing: -0.02em;
}

.subtitle {
    font-size: 13px;
    color: #a3a3a3;
    margin: 0;
}

.login-form {
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.form-group {
    display: flex;
    flex-direction: column;
    gap: 6px;
}

.form-label {
    font-size: 13px;
    font-weight: 500;
    color: #0a0a0a;
}

.form-select,
.form-input {
    width: 100%;
    height: 40px;
    padding: 0 12px;
    border: 1px solid #e5e5e5;
    border-radius: 8px;
    font-size: 14px;
    color: #0a0a0a;
    background: #ffffff;
    transition: border-color 0.15s;
    box-sizing: border-box;
}

.form-select:focus,
.form-input:focus {
    outline: none;
    border-color: #0a0a0a;
}

.form-input::placeholder { color: #a3a3a3; }

.sign-in-btn {
    width: 100%;
    height: 40px;
    margin-top: 8px;
    background: #0a0a0a;
    color: #ffffff;
    border: none;
    border-radius: 8px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: background 0.15s;
}

.sign-in-btn:hover:not(:disabled) { background: #262626; }
.sign-in-btn:disabled { cursor: not-allowed; opacity: 0.5; }

.section-divider {
    padding-top: 8px;
    border-top: 1px solid #eff3f4;
}
.section-title {
    font-size: 13px;
    font-weight: 600;
    color: #536471;
}
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.form-row-3 { grid-template-columns: 1fr 1fr 1fr; }

@media (max-width: 640px) {
    .form-row, .form-row-3 { grid-template-columns: 1fr; }
}
</style>
