<template>
    <div class="home-container">
        <!-- 侧边栏 -->
        <aside class="sidebar" :class="{ collapsed: isCollapsed }">
            <div class="logo">
                <h2 v-if="!isCollapsed">电商物流管理系统</h2>
                <Package v-else :size="24" :stroke-width="2" class="logo-icon" />
            </div>
            <nav class="nav-menu">
                <ul>
                    <li v-for="item in visibleMenu" :key="item.path">
                        <a href="#" @click.prevent="selectMenu(item)" :class="{ active: activeMenu === item.path }">
                            <component :is="item.icon" :size="20" :stroke-width="2" class="icon" />
                            <span class="title" v-if="!isCollapsed">{{ item.title }}</span>
                        </a>
                    </li>
                </ul>
            </nav>
        </aside>

        <!-- 主内容区域 -->
        <div class="main-container">
            <!-- 顶部导航栏 -->
            <header class="top-header">
                <div class="header-left">
                    <button class="toggle-btn" @click="toggleSidebar">
                        <PanelLeft v-if="!isCollapsed" :size="20" :stroke-width="2" />
                        <PanelLeftClose v-else :size="20" :stroke-width="2" />
                    </button>
                    <div class="breadcrumb">
                        <span v-for="(crumb, index) in getBreadcrumb()" :key="index" class="breadcrumb-item-wrapper">
                            <span 
                                v-if="crumb.path" 
                                @click="router.push(crumb.path)" 
                                class="breadcrumb-link"
                            >
                                {{ crumb.title }}
                            </span>
                            <span v-else class="breadcrumb-current">{{ crumb.title }}</span>
                            <span v-if="index < getBreadcrumb().length - 1" class="breadcrumb-separator">/</span>
                        </span>
                    </div>
                </div>
                <div class="header-right">
                    <div class="user-info">
                        <span>用户：{{ getUserName() }}（{{ getRoleLabel(currentUser.role) }}）</span>
                        <span v-if="currentUser.role === 'driver' && currentUser.warehouseName" class="warehouse-info">
                            &nbsp;&nbsp;所属仓库：{{ currentUser.warehouseName }}
                        </span>
                    </div>
                    <button class="logout-btn" @click="logout">退出登录</button>
                </div>
            </header>

            <!-- 页面内容 -->
            <section class="content-area">
                <router-view />
            </section>
        </div>
    </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Home, Store, Truck, ShoppingBag, Shield, Settings, PanelLeft, PanelLeftClose, Package } from 'lucide-vue-next'

const router = useRouter()

// 侧边栏折叠状态
const isCollapsed = ref(false)

// 当前用户信息 - 登录时从后端获取
const currentUser = ref({
    id: '',
    username: '',
    role: 'merchant', // 'merchant' | 'driver' | 'consumer' | 'admin'
    warehouseId: null,
    warehouseName: ''
})

// 当前选中的菜单项
const activeMenu = ref('')

// 页面加载时从sessionStorage读取用户信息
onMounted(() => {
    const userInfoStr = sessionStorage.getItem('userInfo')
    if (!userInfoStr) {
        // 未登录，跳转到登录页
        router.push('/')
        return
    }
    
    const userInfo = JSON.parse(userInfoStr)
    currentUser.value = userInfo
    
    // 设置默认菜单项为该角色的第一个菜单
    const firstMenu = visibleMenu.value[0]
    if (firstMenu) {
        activeMenu.value = firstMenu.path
        router.push(firstMenu.path)
    }
})

// 菜单配置 - 每项指定可见的角色
const menuConfig = [
    // 管理员菜单
    {
        title: '用户管理',
        path: '/admin/user-management',
        icon: Shield,
        roles: ['admin']
    },
    {
        title: '订单管理',
        path: '/admin/order-management',
        icon: ShoppingBag,
        roles: ['admin']
    },
    {
        title: '数据分析',
        path: '/admin/data-analysis',
        icon: Home,
        roles: ['admin']
    },
    // 商家菜单
    {
        title: '商城',
        path: '/general/mall',
        icon: Store,
        roles: ['merchant']
    },
    {
        title: '库存管理',
        path: '/merchant/inventory-management',
        icon: Store,
        roles: ['merchant']
    },
    {
        title: '订单管理',
        path: '/merchant/order-management',
        icon: ShoppingBag,
        roles: ['merchant']
    },
    {
        title: '物流查询',
        path: '/merchant/logistics-query',
        icon: Truck,
        roles: ['merchant']
    },
    // 顾客菜单
    {
        title: '商城',
        path: '/general/mall',
        icon: Store,
        roles: ['consumer']
    },
    {
        title: '我的订单',
        path: '/consumer/my-orders',
        icon: ShoppingBag,
        roles: ['consumer']
    },
    {
        title: '地址管理',
        path: '/consumer/address-management',
        icon: Store,
        roles: ['consumer']
    },
    {
        title: '物流查询',
        path: '/consumer/logistics-query',
        icon: Truck,
        roles: ['consumer']
    },
    // 配送员菜单
    {
        title: '待揽收',
        path: '/driver/pending-pickup',
        icon: Home,
        roles: ['driver']
    },
    {
        title: '待送货',
        path: '/driver/pending-delivery',
        icon: Truck,
        roles: ['driver']
    },
    {
        title: '运输批次',
        path: '/driver/delivery-batch',
        icon: Truck,
        roles: ['driver']
    },
    {
        title: '历史任务',
        path: '/driver/history-tasks',
        icon: Settings,
        roles: ['driver']
    }
]

// 根据当前用户角色过滤菜单
const visibleMenu = computed(() => {
    return menuConfig.filter(item => item.roles.includes(currentUser.value.role))
})

// 获取角色中文标签
const getRoleLabel = (role) => {
    const roleLabels = {
        merchant: '商户',
        driver: '配送员',
        consumer: '顾客',
        admin: '管理员'
    }
    return roleLabels[role] || role
}

// 获取当前用户名 - 直接从后端数据获取
const getUserName = () => {
    return currentUser.value.username || '未知用户'
}

// 选择菜单项
const selectMenu = (item) => {
    activeMenu.value = item.path
    router.push(item.path)
}

// 设置用户信息 - 登录成功后调用
const setUserInfo = (userInfo) => {
    currentUser.value = userInfo // { id: '1', username: '刘天赐', role: 'merchant' }
}

// 四种角色的菜单生成函数
const getMerchantMenu = () => {
    return menuConfig.filter(item => item.roles.includes('merchant'))
}

const getDriverMenu = () => {
    return menuConfig.filter(item => item.roles.includes('driver'))
}

const getConsumerMenu = () => {
    return menuConfig.filter(item => item.roles.includes('consumer'))
}

const getAdminMenu = () => {
    return menuConfig.filter(item => item.roles.includes('admin'))
}

// 根据角色设置菜单的函数
const setMenuByRole = (role) => {
    currentUser.value.role = role
    activeMenu.value = '/dashboard'
}

// 退出登录
const logout = () => {
    sessionStorage.removeItem('userInfo')
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('isLoggedIn')
    router.push('/')
}

// 获取面包屑导航
const getBreadcrumb = () => {
    const currentPath = router.currentRoute.value.path

    // 商品下架页面特殊处理
    if (currentPath === '/merchant/product-delisting') {
        return [
            { title: '商城', path: '/general/mall' },
            { title: '商品下架', path: null }
        ]
    }

    // 商品上架页面
    if (currentPath === '/merchant/product-listing') {
        return [
            { title: '库存管理', path: '/merchant/inventory-management' },
            { title: '商品上架', path: null }
        ]
    }

    // 商品入库页面
    if (currentPath === '/merchant/stock-in') {
        return [
            { title: '库存管理', path: '/merchant/inventory-management' },
            { title: '商品入库', path: null }
        ]
    }

    // 运输批次详情页面
    if (currentPath.startsWith('/driver/delivery-batch-detail')) {
        return [
            { title: '运输批次', path: '/driver/delivery-batch' },
            { title: '批次详情', path: null }
        ]
    }

    // 其他页面显示单层导航
    const currentMenu = menuConfig.find(item => item.path === currentPath)
    return currentMenu ? [{ title: currentMenu.title, path: null }] : [{ title: '首页', path: null }]
}

// 切换侧边栏折叠状态
const toggleSidebar = () => {
    isCollapsed.value = !isCollapsed.value
}
</script>

<style scoped>
/* 全局容器 */
.home-container {
    display: flex;
    height: 100vh;
    background-color: #ffffff;
}

/* 侧边栏 */
.sidebar {
    width: 250px;
    background-color: #f8f9fa;
    color: #1f2937;
    display: flex;
    flex-direction: column;
    box-shadow: 1px 0 3px rgba(0, 0, 0, 0.05);
    transition: width 0.3s ease;
}

.sidebar.collapsed {
    width: 80px;
}

.logo {
    padding: 24px 16px;
    display: flex;
    align-items: center;
    justify-content: center;
}

.logo h2 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
    color: #1f2937;
}

.logo-icon {
    color: #1f2937;
}

/* 导航菜单 */
.nav-menu {
    flex: 1;
    padding: 16px 0;
    overflow-y: auto;
}

.nav-menu ul {
    list-style: none;
    padding: 0;
    margin: 0;
}

.nav-menu li {
    margin: 4px 8px;
}

.nav-menu a {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 16px;
    color: #1f2937;
    text-decoration: none;
    border-radius: 6px;
    transition: all 0.2s ease;
    cursor: pointer;
    justify-content: flex-start;
}

.sidebar.collapsed .nav-menu a {
    justify-content: center;
    padding: 12px;
}

.nav-menu a:hover {
    background-color: #e5e7eb;
    color: #2563eb;
}

.nav-menu a.active {
    background-color: #e5e7eb;
    color: #1f2937;
    font-weight: 500;
}

.icon {
    min-width: 20px;
    flex-shrink: 0;
}

.title {
    font-size: 14px;
    white-space: nowrap;
}

.nav-menu {
    flex: 1;
    padding: 16px 0;
    overflow-y: auto;
}

/* 主容器 */
.main-container {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
}

/* 顶部导航栏 */
.top-header {
    height: 60px;
    background-color: #ffffff;
    border-bottom: 1px solid #e5e7eb;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 24px;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.header-left {
    display: flex;
    align-items: center;
    gap: 16px;
}

.toggle-btn {
    padding: 8px;
    background-color: transparent;
    border: none;
    color: #6b7280;
    border-radius: 6px;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s;
}

.toggle-btn:hover {
    background-color: #f3f4f6;
    color: #1f2937;
}

.breadcrumb {
    display: flex;
    align-items: center;
    gap: 8px;
}

.breadcrumb-link {
    font-size: 16px;
    color: #1f2937;
    font-weight: 500;
    cursor: pointer;
    transition: color 0.2s;
}

.breadcrumb-link:hover {
    color: #3b82f6;
}

.breadcrumb-current {
    font-size: 16px;
    color: #1f2937;
    font-weight: 500;
}

.breadcrumb-separator {
    font-size: 16px;
    color: #9ca3af;
    margin: 0 4px;
}

.breadcrumb-item {
    font-size: 16px;
    color: #1f2937;
    font-weight: 500;
}

.header-right {
    display: flex;
    align-items: center;
    gap: 16px;
}

.user-info {
    font-size: 14px;
    color: #6b7280;
    display: flex;
    align-items: center;
}

.warehouse-info {
    color: #6b7280;
    font-weight: normal;
}

.logout-btn {
    padding: 8px 16px;
    background-color: #ffffff;
    border: 1px solid #e5e7eb;
    color: #374151;
    border-radius: 6px;
    cursor: pointer;
    font-size: 14px;
    transition: all 0.2s;
}

.logout-btn:hover {
    background-color: #f9fafb;
    border-color: #d1d5db;
}

/* 内容区域 */
.content-area {
    flex: 1;
    overflow-y: auto;
    padding: 24px;
    background-color: #ffffff;
}
</style>