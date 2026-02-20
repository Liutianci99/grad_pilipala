<template>
    <div class="home-container">
        <!-- 侧边栏 -->
        <aside class="sidebar" :class="{ collapsed: isCollapsed }">
            <div class="logo">
                <div class="logo-mark" v-if="!isCollapsed">
                    <Boxes :size="22" :stroke-width="1.5" />
                </div>
                <Boxes v-else :size="22" :stroke-width="1.5" class="logo-icon" />
                <div class="logo-text" v-if="!isCollapsed">
                    <h2>物流管理</h2>
                    <span class="logo-sub">E-commerce Logistics</span>
                </div>
            </div>
            <nav class="nav-menu">
                <div class="nav-section" v-if="!isCollapsed">
                    <span class="nav-label">{{ getRoleLabel(currentUser.role) }}</span>
                </div>
                <ul>
                    <li v-for="item in visibleMenu" :key="item.path">
                        <a href="#" @click.prevent="selectMenu(item)" :class="{ active: activeMenu === item.path }">
                            <component :is="item.icon" :size="22" :stroke-width="1.5" class="icon" />
                            <div class="nav-text" v-if="!isCollapsed">
                                <span class="title">{{ item.title }}</span>
                                <span class="subtitle">{{ item.desc }}</span>
                            </div>
                        </a>
                    </li>
                </ul>
            </nav>
            <div class="sidebar-footer">
                <div class="user-card" v-if="!isCollapsed">
                    <div class="user-avatar">{{ getUserName().charAt(0) }}</div>
                    <div class="user-detail">
                        <span class="user-card-name">{{ getUserName() }}</span>
                        <span class="user-card-role">{{ getRoleLabel(currentUser.role) }}</span>
                    </div>
                </div>
                <a href="#" @click.prevent="logout" class="logout-link">
                    <LogOut :size="20" :stroke-width="1.5" class="icon" />
                    <span class="title" v-if="!isCollapsed">退出登录</span>
                </a>
            </div>
        </aside>

        <!-- 主内容区域 -->
        <div class="main-container">
            <!-- 顶部导航栏 -->
            <header class="top-header">
                <div class="header-left">
                    <button class="toggle-btn" @click="toggleSidebar">
                        <PanelLeft v-if="!isCollapsed" :size="18" :stroke-width="1.5" />
                        <PanelLeftClose v-else :size="18" :stroke-width="1.5" />
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
                        <span class="user-badge">{{ getRoleLabel(currentUser.role) }}</span>
                        <span class="user-name">{{ getUserName() }}</span>
                        <span v-if="currentUser.role === 'driver' && currentUser.warehouseName" class="warehouse-info">
                            · {{ currentUser.warehouseName }}
                        </span>
                    </div>
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
import { Home, Store, Truck, ShoppingBag, Shield, Settings, PanelLeft, PanelLeftClose, Package, LogOut, BarChart3, Users, MapPin, ClipboardList, History, PackageSearch, Boxes, Warehouse, ScanLine, PackageCheck, Route, ShoppingCart, MapPinned, Navigation } from 'lucide-vue-next'

const router = useRouter()

const isCollapsed = ref(false)

const currentUser = ref({
    id: '',
    username: '',
    role: 'merchant',
    warehouseId: null,
    warehouseName: ''
})

const activeMenu = ref('')

onMounted(() => {
    const userInfoStr = sessionStorage.getItem('userInfo')
    if (!userInfoStr) {
        router.push('/')
        return
    }
    
    const userInfo = JSON.parse(userInfoStr)
    currentUser.value = userInfo
    
    const firstMenu = visibleMenu.value[0]
    if (firstMenu) {
        activeMenu.value = firstMenu.path
        router.push(firstMenu.path)
    }
})

const menuConfig = [
    { title: '用户管理', desc: '管理系统用户', path: '/admin/user-management', icon: Users, roles: ['admin'] },
    { title: '订单管理', desc: '查看所有订单', path: '/admin/order-management', icon: ClipboardList, roles: ['admin'] },
    { title: '数据分析', desc: '统计与报表', path: '/admin/data-analysis', icon: BarChart3, roles: ['admin'] },
    { title: '商城', desc: '浏览全部商品', path: '/general/mall', icon: ShoppingCart, roles: ['merchant'] },
    { title: '库存管理', desc: '商品与库存', path: '/merchant/inventory-management', icon: Warehouse, roles: ['merchant'] },
    { title: '订单管理', desc: '处理商户订单', path: '/merchant/order-management', icon: ClipboardList, roles: ['merchant'] },
    { title: '物流查询', desc: '追踪物流状态', path: '/merchant/logistics-query', icon: Navigation, roles: ['merchant'] },
    { title: '商城', desc: '浏览全部商品', path: '/general/mall', icon: ShoppingCart, roles: ['consumer'] },
    { title: '我的订单', desc: '查看购买记录', path: '/consumer/my-orders', icon: ShoppingBag, roles: ['consumer'] },
    { title: '地址管理', desc: '收货地址设置', path: '/consumer/address-management', icon: MapPinned, roles: ['consumer'] },
    { title: '物流查询', desc: '追踪包裹位置', path: '/consumer/logistics-query', icon: Navigation, roles: ['consumer'] },
    { title: '待揽收', desc: '等待揽收的包裹', path: '/driver/pending-pickup', icon: ScanLine, roles: ['driver'] },
    { title: '待送货', desc: '准备配送的订单', path: '/driver/pending-delivery', icon: PackageCheck, roles: ['driver'] },
    { title: '运输批次', desc: '批次路线管理', path: '/driver/delivery-batch', icon: Route, roles: ['driver'] },
    { title: '历史任务', desc: '已完成的配送', path: '/driver/history-tasks', icon: History, roles: ['driver'] }
]

const visibleMenu = computed(() => {
    return menuConfig.filter(item => item.roles.includes(currentUser.value.role))
})

const getRoleLabel = (role) => {
    const roleLabels = { merchant: '商户', driver: '配送员', consumer: '顾客', admin: '管理员' }
    return roleLabels[role] || role
}

const getUserName = () => currentUser.value.username || '未知用户'

const selectMenu = (item) => {
    activeMenu.value = item.path
    router.push(item.path)
}

const logout = () => {
    sessionStorage.removeItem('userInfo')
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('isLoggedIn')
    router.push('/')
}

const getBreadcrumb = () => {
    const currentPath = router.currentRoute.value.path
    if (currentPath === '/merchant/product-delisting') return [{ title: '商城', path: '/general/mall' }, { title: '商品下架', path: null }]
    if (currentPath === '/merchant/product-listing') return [{ title: '库存管理', path: '/merchant/inventory-management' }, { title: '商品上架', path: null }]
    if (currentPath === '/merchant/stock-in') return [{ title: '库存管理', path: '/merchant/inventory-management' }, { title: '商品入库', path: null }]
    if (currentPath.startsWith('/driver/delivery-batch-detail')) return [{ title: '运输批次', path: '/driver/delivery-batch' }, { title: '批次详情', path: null }]
    const currentMenu = menuConfig.find(item => item.path === currentPath)
    return currentMenu ? [{ title: currentMenu.title, path: null }] : [{ title: '首页', path: null }]
}

const toggleSidebar = () => { isCollapsed.value = !isCollapsed.value }
</script>

<style scoped>
.home-container {
    display: flex;
    height: 100vh;
    background: #ffffff;
}

/* ── Sidebar (X.com style — light, clean) ── */
.sidebar {
    width: 260px;
    background: #ffffff;
    color: #0f1419;
    display: flex;
    flex-direction: column;
    transition: width 0.2s ease;
    border-right: 1px solid #eff3f4;
}
.sidebar.collapsed { width: 68px; }

.logo {
    padding: 20px 16px 16px;
    display: flex;
    align-items: center;
    gap: 12px;
}
.logo-mark {
    width: 36px;
    height: 36px;
    background: #0f1419;
    color: #ffffff;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
}
.logo-text { display: flex; flex-direction: column; }
.logo h2 {
    margin: 0;
    font-size: 16px;
    font-weight: 700;
    color: #0f1419;
    letter-spacing: -0.02em;
    line-height: 1.2;
}
.logo-sub {
    font-size: 11px;
    color: #536471;
    letter-spacing: 0.02em;
}
.logo-icon { color: #0f1419; }

.nav-section { padding: 16px 16px 6px; }
.nav-label {
    font-size: 11px;
    font-weight: 600;
    color: #536471;
    text-transform: uppercase;
    letter-spacing: 0.05em;
}

.nav-menu {
    flex: 1;
    padding: 0;
    overflow-y: auto;
}
.nav-menu ul { list-style: none; padding: 0; margin: 0; }
.nav-menu li { margin: 1px 8px; }

.nav-menu a {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 10px 12px;
    color: #536471;
    text-decoration: none;
    border-radius: 12px;
    transition: all 0.15s ease;
    cursor: pointer;
}
.sidebar.collapsed .nav-menu a {
    justify-content: center;
    padding: 12px;
}
.nav-menu a:hover {
    background: #f7f9f9;
    color: #0f1419;
}
.nav-menu a.active {
    background: #eff3f4;
    color: #0f1419;
}
.nav-menu a.active .icon { color: #0f1419; }
.nav-menu a.active .title { font-weight: 700; }

.icon { min-width: 22px; flex-shrink: 0; }
.nav-text { display: flex; flex-direction: column; min-width: 0; }
.title { font-size: 14px; white-space: nowrap; line-height: 1.3; }
.subtitle {
    font-size: 11px;
    color: #8899a6;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    line-height: 1.3;
}
.nav-menu a.active .subtitle { color: #536471; }

.sidebar-footer {
    padding: 12px;
    border-top: 1px solid #eff3f4;
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.user-card {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 8px;
    border-radius: 12px;
    background: #f7f9f9;
}
.user-avatar {
    width: 34px;
    height: 34px;
    background: #0f1419;
    color: #ffffff;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    font-weight: 600;
    flex-shrink: 0;
}
.user-detail { display: flex; flex-direction: column; min-width: 0; }
.user-card-name {
    font-size: 13px;
    font-weight: 600;
    color: #0f1419;
    line-height: 1.3;
}
.user-card-role {
    font-size: 11px;
    color: #536471;
    line-height: 1.3;
}

.logout-link {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 10px 12px;
    color: #536471;
    text-decoration: none;
    border-radius: 12px;
    transition: all 0.15s ease;
    cursor: pointer;
    font-size: 14px;
}
.logout-link:hover {
    background: #f7f9f9;
    color: #e0245e;
}

/* ── Main ── */
.main-container {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    min-width: 0;
}

.top-header {
    height: 52px;
    background: #ffffff;
    border-bottom: 1px solid #e5e5e5;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 20px;
    flex-shrink: 0;
}

.header-left { display: flex; align-items: center; gap: 12px; }

.toggle-btn {
    padding: 6px;
    background: transparent;
    border: none;
    color: #737373;
    border-radius: 6px;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.15s;
}
.toggle-btn:hover { background: #f5f5f5; color: #0a0a0a; }

.breadcrumb { display: flex; align-items: center; gap: 6px; }
.breadcrumb-link {
    font-size: 13px;
    color: #737373;
    cursor: pointer;
    transition: color 0.15s;
}
.breadcrumb-link:hover { color: #0a0a0a; }
.breadcrumb-current { font-size: 13px; color: #0a0a0a; font-weight: 500; }
.breadcrumb-separator { font-size: 13px; color: #d4d4d4; margin: 0 2px; }

.header-right { display: flex; align-items: center; gap: 12px; }
.user-info { display: flex; align-items: center; gap: 8px; font-size: 13px; color: #737373; }
.user-badge {
    padding: 2px 8px;
    background: #f5f5f5;
    border: 1px solid #e5e5e5;
    border-radius: 4px;
    font-size: 11px;
    font-weight: 500;
    color: #525252;
}
.user-name { color: #0a0a0a; font-weight: 500; }
.warehouse-info { color: #a3a3a3; }

.content-area {
    flex: 1;
    overflow-y: auto;
    padding: 24px;
    background: #fafafa;
}
</style>
