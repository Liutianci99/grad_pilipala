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
                    <select v-model="form.role" class="form-select">
                        <option value="">请选择身份</option>
                        <option value="merchant">商家</option>
                        <option value="consumer">顾客</option>
                        <option value="driver">配送员</option>
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

                <div class="btn-row">
                    <button type="button" class="sign-in-btn ghost" @click="$router.push('/')">登录</button>
                    <button type="submit" class="sign-in-btn" :disabled="loading">
                        {{ loading ? '注册中...' : '注册' }}
                    </button>
                </div>
            </form>
        </div>
    </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const router = useRouter()

const form = reactive({
    role: '',
    username: '',
    password: '',
    confirmPassword: ''
})

const handleRegister = async () => {
    if (!form.role) return ElMessage.warning('请选择身份')
    if (!form.username) return ElMessage.warning('请输入用户名')
    if (!form.password) return ElMessage.warning('请输入密码')
    if (form.password !== form.confirmPassword) return ElMessage.warning('两次密码不一致')

    loading.value = true
    try {
        const data = await request.post('/auth/register', {
            username: form.username,
            password: form.password,
            role: form.role
        })

        if (!data.success) {
            ElMessage.error(data.message || '注册失败')
            return
        }

        ElMessage.success('注册成功')
        if (data.data) {
            const userInfo = {
                id: data.data.id,
                username: data.data.username,
                role: data.data.role,
                token: data.data.token,
                warehouseId: data.data.warehouseId,
                warehouseName: data.data.warehouseName
            }
            sessionStorage.setItem('userInfo', JSON.stringify(userInfo))
            sessionStorage.setItem('token', data.data.token)
            sessionStorage.setItem('isLoggedIn', 'true')
            await router.push('/demo')
        }
    } catch (e) { /* handled */ } finally {
        loading.value = false
    }
}
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
    max-width: 380px;
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

.btn-row {
    display: flex;
    gap: 10px;
    margin-top: 8px;
}

.sign-in-btn {
    flex: 1;
    height: 40px;
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

.sign-in-btn.ghost {
    background: #ffffff;
    color: #0a0a0a;
    border: 1px solid #e5e5e5;
}
.sign-in-btn.ghost:hover { background: #f5f5f5; }
</style>
