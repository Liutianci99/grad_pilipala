<template>
    <div class="page-container">
        <h1>用户管理</h1>
        <div class="filter-bar">
            <div class="search-box">
                <input type="text" v-model="searchKeyword" placeholder="搜索用户名..." class="search-input" @keyup.enter="loadUsers" />
                <button @click="loadUsers" class="btn btn-ghost">搜索</button>
            </div>
            <div class="tabs">
                <button :class="['tab', { active: currentRole === 'all' }]" @click="changeRole('all')">全部</button>
                <button :class="['tab', { active: currentRole === 'merchant' }]" @click="changeRole('merchant')">商户</button>
                <button :class="['tab', { active: currentRole === 'driver' }]" @click="changeRole('driver')">配送员</button>
                <button :class="['tab', { active: currentRole === 'consumer' }]" @click="changeRole('consumer')">顾客</button>
            </div>
        </div>

        <div class="toolbar">
            <span class="count">共 {{ users.length }} 个用户</span>
            <button class="btn btn-primary" @click="openCreate">+ 新增用户</button>
        </div>

        <div v-if="users.length === 0" class="empty-state"><p>暂无用户</p></div>

        <div class="user-list">
            <div v-for="user in users" :key="user.id" class="user-card">
                <div class="user-avatar">{{ user.username?.charAt(0)?.toUpperCase() }}</div>
                <div class="user-info">
                    <div class="user-name">{{ user.username }}</div>
                    <div class="user-id">ID: {{ user.id }}</div>
                </div>
                <span class="badge" :class="'role-' + user.role">{{ getRoleText(user.role) }}</span>
                <div class="user-actions">
                    <button class="btn btn-ghost btn-sm" @click="openEdit(user)">编辑</button>
                    <button class="btn btn-danger btn-sm" @click="deleteUser(user)">删除</button>
                </div>
            </div>
        </div>

        <!-- Dialog -->
        <div v-if="showDialog" class="dialog-overlay" @click.self="showDialog = false">
            <div class="dialog-content">
                <div class="dialog-header">
                    <h3>{{ editingUser ? '编辑用户' : '新增用户' }}</h3>
                    <button class="close-btn" @click="showDialog = false">×</button>
                </div>
                <div style="padding: 20px;">
                    <div class="form-group">
                        <label class="form-label">用户名 <span class="required">*</span></label>
                        <input v-model="form.username" class="form-input" placeholder="请输入用户名" />
                    </div>
                    <div class="form-group">
                        <label class="form-label">密码 {{ editingUser ? '(留空不修改)' : '' }} <span v-if="!editingUser" class="required">*</span></label>
                        <input v-model="form.password" type="password" class="form-input" placeholder="请输入密码" />
                    </div>
                    <div class="form-group">
                        <label class="form-label">角色 <span class="required">*</span></label>
                        <select v-model="form.role" class="form-select">
                            <option value="">请选择角色</option>
                            <option value="merchant">商户</option>
                            <option value="driver">配送员</option>
                            <option value="consumer">顾客</option>
                            <option value="admin">管理员</option>
                        </select>
                    </div>
                    <div class="form-actions">
                        <button class="btn btn-ghost" @click="showDialog = false">取消</button>
                        <button class="btn btn-primary" @click="saveUser">保存</button>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const users = ref([])
const searchKeyword = ref('')
const currentRole = ref('all')
const showDialog = ref(false)
const editingUser = ref(null)
const form = ref({ username: '', password: '', role: '' })

const getRoleText = (r) => ({ merchant: '商户', driver: '配送员', consumer: '顾客', admin: '管理员' }[r] || r)

const loadUsers = async () => {
    try {
        const params = {}
        if (currentRole.value !== 'all') params.role = currentRole.value
        if (searchKeyword.value) params.search = searchKeyword.value
        const res = await request.get('/admin/users', { params })
        if (res.code === 200) users.value = res.data || []
    } catch (e) { /* handled */ }
}

const changeRole = (role) => { currentRole.value = role; loadUsers() }

const openCreate = () => {
    editingUser.value = null
    form.value = { username: '', password: '', role: '' }
    showDialog.value = true
}

const openEdit = (user) => {
    editingUser.value = user
    form.value = { username: user.username, password: '', role: user.role }
    showDialog.value = true
}

const saveUser = async () => {
    if (!form.value.username || !form.value.role) { ElMessage.warning('请填写必填项'); return }
    if (!editingUser.value && !form.value.password) { ElMessage.warning('请输入密码'); return }

    const data = { username: form.value.username, role: form.value.role }
    if (form.value.password) data.password = form.value.password

    if (editingUser.value) {
        const res = await request.put(`/admin/users/${editingUser.value.id}`, data)
        if (res.code !== 200) { ElMessage.error(res.message || '更新失败'); return }
    } else {
        const res = await request.post('/admin/users', data)
        if (res.code !== 200) { ElMessage.error(res.message || '创建失败'); return }
    }
    showDialog.value = false
    loadUsers()
}

const deleteUser = async (user) => {
    try {
        await ElMessageBox.confirm(`确认删除用户 "${user.username}" 吗？`, '确认', { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' })
    } catch { return }
    const res = await request.delete(`/admin/users/${user.id}`)
    if (res.code === 200) { ElMessage.success('删除成功'); loadUsers() }
    else ElMessage.error(res.message || '删除失败')
}

onMounted(loadUsers)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.count { font-size: 13px; color: #536471; }

.user-list { display: flex; flex-direction: column; gap: 8px; }
.user-card {
    display: flex; align-items: center; gap: 12px;
    background: #fff; border: 1px solid #eff3f4; border-radius: 12px;
    padding: 12px 16px; transition: border-color 0.15s;
}
.user-card:hover { border-color: #cfd9de; }

.user-avatar {
    width: 40px; height: 40px; border-radius: 50%; background: #0f1419; color: #fff;
    display: flex; align-items: center; justify-content: center;
    font-size: 16px; font-weight: 700; flex-shrink: 0;
}
.user-info { flex: 1; min-width: 0; }
.user-name { font-size: 14px; font-weight: 600; color: #0f1419; }
.user-id { font-size: 12px; color: #8899a6; margin-top: 1px; }

.role-merchant { background: #eff6ff; color: #1d4ed8; border: 1px solid #bfdbfe; }
.role-driver { background: #fef3c7; color: #b45309; border: 1px solid #fde68a; }
.role-consumer { background: #f0fdf4; color: #15803d; border: 1px solid #bbf7d0; }
.role-admin { background: #fce7f3; color: #be185d; border: 1px solid #fbcfe8; }

.user-actions { display: flex; gap: 6px; flex-shrink: 0; }
</style>
