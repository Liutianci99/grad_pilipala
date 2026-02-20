<template>
    <div class="login-container">
        <div class="login-box">
            <div class="login-header">
                <h1 class="brand">物流管理</h1>
                <p class="subtitle">电商物流管理系统</p>
            </div>

            <form class="login-form" @submit.prevent="handleLogin">
                <div class="form-group">
                    <label class="form-label">身份</label>
                    <select v-model="loginForm.role" class="form-select">
                        <option value="">请选择身份</option>
                        <option value="merchant">商家</option>
                        <option value="consumer">顾客</option>
                        <option value="driver">配送员</option>
                        <option value="admin">管理员</option>
                    </select>
                </div>

                <div class="form-group">
                    <label class="form-label">用户名</label>
                    <input
                        v-model="loginForm.username"
                        type="text"
                        class="form-input"
                        placeholder="请输入用户名"
                    />
                </div>

                <div class="form-group">
                    <label class="form-label">密码</label>
                    <input
                        v-model="loginForm.password"
                        :type="showPassword ? 'text' : 'password'"
                        class="form-input"
                        placeholder="请输入密码"
                    />
                </div>

                <button type="submit" class="sign-in-btn" :disabled="loading">
                    {{ loading ? '登录中...' : '登录' }}
                </button>
            </form>
        </div>
    </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const loading = ref(false)
const showPassword = ref(false)
const router = useRouter()

const loginForm = reactive({
    role: '',
    username: '',
    password: ''
})

const handleLogin = async () => {
    if (!loginForm.role) return
    if (!loginForm.username || !loginForm.password) return

    loading.value = true
    try {
        const data = await request.post('/auth/login', {
            username: loginForm.username,
            password: loginForm.password,
            role: loginForm.role
        })

        if (!data.success) {
            loading.value = false
            return
        }

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
    } catch (e) {
        loading.value = false
    } finally {
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

.form-input::placeholder,
.form-select::placeholder {
    color: #a3a3a3;
}

.sign-in-btn {
    width: 100%;
    height: 40px;
    background: #0a0a0a;
    color: #ffffff;
    border: none;
    border-radius: 8px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: background 0.15s;
    margin-top: 8px;
}

.sign-in-btn:hover:not(:disabled) {
    background: #262626;
}

.sign-in-btn:disabled {
    cursor: not-allowed;
    opacity: 0.5;
}
</style>
