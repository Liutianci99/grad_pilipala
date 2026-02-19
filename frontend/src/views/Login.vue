<template>
 <div class="login-container">
    <div class="login-box">

        <h1 class="welcome-title">Welcome</h1>

  

        <form class="login-form" @submit.prevent="handleLogin">
            <div class="form-group">
                <label class="form-label"></label>
                <select
                    v-model="loginForm.role"
                    class="form-select"
                >
                    <option value="">请选择身份</option>
                    <option value="merchant">商家</option>
                    <option value="consumer">顾客</option> 
                    <option value="driver">配送员</option>
                    <option value="admin">管理员</option>      
                </select>       
            </div>

            <hr class="divider">

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
                <div class="password-input-wrapper">
                    <input
                        v-model="loginForm.password"
                        :type="showPassword ? 'text' : 'password'"
                        class="form-input"
                        placeholder="请输入密码"
                    />
                </div>
            </div>
            <button type="submit" class="sign-in-btn" :disabled="loading">
                {{ loading ? '登录中...' : '登录' }}
            </button>
        </form>
    </div>
 </div>
</template>

<script setup>
import {ref, reactive} from 'vue'
import {useRouter} from 'vue-router'

//响应式变量
const loading = ref(false)
const showPassword = ref(false)
const router = useRouter()

//表单数据
const loginForm = reactive({
    role: '',
    username: '',
    password: ''
})

//登录处理函数
const handleLogin = async () => {
    //验证是否选择身份
    if (!loginForm.role) {
        alert('请选择身份')
        return
    }

    //验证用户名和密码是否填写
    if (!loginForm.username || !loginForm.password) {
        alert('请输入用户名和密码')
        return
    }

    //显示登录信息
    const roleText = {
        merchant: '商家',
        consumer: '顾客',
        driver: '配送员',
        admin: '管理员'
    }

    // 登录加载
    loading.value = true
    try {
      const res = await fetch('/api/auth/login', {
          method: 'POST',
          headers: {
              'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            username: loginForm.username,
            password: loginForm.password,
            role: loginForm.role
          })
      })
      const data = await res.json()
      console.log('登录响应数据:', data)
      console.log('data.success:', data.success)
      console.log('data.data:', data.data)
      console.log('完整响应:', JSON.stringify(data, null, 2))
      
      if(!data.success) {
          alert(data.message || '登录失败，请检查用户名和密码')
          loading.value = false
          return
      }
      // 保存用户信息和登录状态
      if (data.data) {
          const userInfo = {
              id: data.data.id,
              username: data.data.username,
              role: data.data.role,
              token: data.data.token,
              warehouseId: data.data.warehouseId,
              warehouseName: data.data.warehouseName
          }
          console.log('保存用户信息:', userInfo)
          // 使用 sessionStorage 而不是 localStorage，每个标签页独立会话
          sessionStorage.setItem('userInfo', JSON.stringify(userInfo))
          sessionStorage.setItem('token', data.data.token)
          sessionStorage.setItem('isLoggedIn', 'true')
          console.log('准备跳转到 /demo')
          await router.push('/demo')
          console.log('跳转完成')
      } else {
          console.error('data.data 不存在，完整数据:', data)
          alert('登录失败：未获取到用户信息，请检查控制台')
          loading.value = false
      }
    } catch (e) {
      console.error('登录错误:', e)
      alert('登录请求失败，请稍后重试')
      loading.value = false
    } finally {
      loading.value = false
    }
}
</script>

<style scoped>
/* 外层容器：占满屏幕，内容居中 */
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  padding: 20px;
}
/* 登录框 */
.login-box {
  background: white;
  padding: 48px 40px;
  width: 100%;
  max-width: 430px;
}
/* 标题样式 */
.welcome-title {
  font-size: 32px;
  font-weight: 600;
  color: #000000;
  margin: 0 0 20px 0;
  text-align: center;
}
/* 灰色分隔线 */
.divider {
  border: none;
  height: 1px;
  background-color: #e5e7eb;
  margin: 0;
  width: 100%;
}
/* 表单容器：用 gap 统一垂直间距 */
.login-form {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
/* 表单组（每个输入框的容器） */
.form-group {
  margin: 0; 
}
/* 标签样式 */
.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #000000;
  margin-bottom: 8px;
}
/* 下拉选择框样式 */
.form-select {
  width: 100%;
  height: 44px;
  padding: 0 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 15px;
  color: #000000;
  background: #f9fafb;
  transition: all 0.2s;
  box-sizing: border-box;
  cursor: pointer;
}
.form-select:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}
/* 输入框样式 */
.form-input {
  width: 100%;
  height: 44px;
  padding: 0 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 15px;
  color: #000000;
  background: #ffffff;
  transition: all 0.2s;
  box-sizing: border-box;
}

.form-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}
.form-input::placeholder {
  color: #9ca3af;
}
/* 密码输入框包装器 */
.password-input-wrapper {
  position: relative;
}

.password-input-wrapper .form-input {
  padding-right: 48px;
}
/* 登录按钮 */
.sign-in-btn {
  width: 100%;
  height: 48px;
  background: #000000;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  margin: 5px 0 0 0 ;
}

.sign-in-btn:hover:not(:disabled) {
  background: #333333;
}
.sign-in-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}
</style>
