<template>
    <div class="page-container">
        <h1>数据分析</h1>

        <!-- Overview Cards -->
        <div class="stat-grid">
            <div class="stat-card">
                <div class="stat-icon">📦</div>
                <div class="stat-body">
                    <div class="stat-value">{{ stats.totalOrders ?? '-' }}</div>
                    <div class="stat-label">总订单数</div>
                </div>
            </div>
            <div class="stat-card">
                <div class="stat-icon">👥</div>
                <div class="stat-body">
                    <div class="stat-value">{{ stats.totalUsers ?? '-' }}</div>
                    <div class="stat-label">总用户数</div>
                </div>
            </div>
            <div class="stat-card">
                <div class="stat-icon">💰</div>
                <div class="stat-body">
                    <div class="stat-value">¥{{ formatMoney(stats.totalRevenue) }}</div>
                    <div class="stat-label">总营收</div>
                </div>
            </div>
            <div class="stat-card">
                <div class="stat-icon">🏭</div>
                <div class="stat-body">
                    <div class="stat-value">{{ stats.totalWarehouses ?? '-' }}</div>
                    <div class="stat-label">仓库数量</div>
                </div>
            </div>
        </div>

        <!-- Two columns: user breakdown + order status -->
        <div class="panel-row">
            <div class="panel">
                <h3>用户构成</h3>
                <div class="breakdown-list">
                    <div class="breakdown-item">
                        <span class="dot" style="background:#1d4ed8"></span>
                        <span class="breakdown-label">商户</span>
                        <span class="breakdown-value">{{ stats.merchantCount ?? 0 }}</span>
                        <div class="breakdown-bar"><div class="breakdown-fill" :style="{ width: userPct('merchantCount'), background: '#1d4ed8' }"></div></div>
                    </div>
                    <div class="breakdown-item">
                        <span class="dot" style="background:#b45309"></span>
                        <span class="breakdown-label">配送员</span>
                        <span class="breakdown-value">{{ stats.driverCount ?? 0 }}</span>
                        <div class="breakdown-bar"><div class="breakdown-fill" :style="{ width: userPct('driverCount'), background: '#b45309' }"></div></div>
                    </div>
                    <div class="breakdown-item">
                        <span class="dot" style="background:#15803d"></span>
                        <span class="breakdown-label">顾客</span>
                        <span class="breakdown-value">{{ stats.consumerCount ?? 0 }}</span>
                        <div class="breakdown-bar"><div class="breakdown-fill" :style="{ width: userPct('consumerCount'), background: '#15803d' }"></div></div>
                    </div>
                </div>
            </div>

            <div class="panel">
                <h3>订单状态分布</h3>
                <div class="breakdown-list">
                    <div v-for="(label, idx) in statusLabels" :key="idx" class="breakdown-item">
                        <span class="dot" :style="{ background: statusColors[idx] }"></span>
                        <span class="breakdown-label">{{ label }}</span>
                        <span class="breakdown-value">{{ statusCount(idx) }}</span>
                        <div class="breakdown-bar"><div class="breakdown-fill" :style="{ width: orderPct(idx), background: statusColors[idx] }"></div></div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Daily Orders Chart -->
        <div class="panel">
            <div class="chart-header">
                <h3>近7天订单趋势</h3>
            </div>
            <div class="bar-chart">
                <div v-for="day in dailyOrders" :key="day.date" class="bar-col">
                    <div class="bar-value">{{ day.count }}</div>
                    <div class="bar-track">
                        <div class="bar-fill" :style="{ height: barHeight(day.count) }"></div>
                    </div>
                    <div class="bar-label">{{ shortDate(day.date) }}</div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import request from '@/utils/request'

const stats = ref({})
const dailyOrders = ref([])

const statusLabels = ['未发货', '已发货', '已揽收', '运输中', '已到达', '已收货']
const statusColors = ['#8899a6', '#536471', '#1d4ed8', '#2563eb', '#b45309', '#15803d']

const formatMoney = (v) => {
    if (v == null) return '-'
    return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const userPct = (key) => {
    const total = stats.value.totalUsers || 1
    return Math.round(((stats.value[key] || 0) / total) * 100) + '%'
}

const statusCount = (idx) => stats.value.ordersByStatus?.[String(idx)] ?? 0
const orderPct = (idx) => {
    const total = stats.value.totalOrders || 1
    return Math.round((statusCount(idx) / total) * 100) + '%'
}

const maxDaily = computed(() => Math.max(...dailyOrders.value.map(d => d.count), 1))
const barHeight = (count) => Math.max((count / maxDaily.value) * 100, 2) + '%'

const shortDate = (dateStr) => {
    if (!dateStr) return ''
    const parts = dateStr.split('-')
    return `${parts[1]}/${parts[2]}`
}

const loadData = async () => {
    try {
        const [overviewRes, dailyRes] = await Promise.all([
            request.get('/admin/stats/overview'),
            request.get('/admin/stats/daily-orders', { params: { days: 7 } })
        ])
        if (overviewRes.code === 200) stats.value = overviewRes.data || {}
        if (dailyRes.code === 200) dailyOrders.value = dailyRes.data || []
    } catch (e) { console.error(e) }
}

onMounted(loadData)
</script>

<style scoped>
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 16px; }
.stat-card {
    display: flex; align-items: center; gap: 14px;
    background: #fff; border: 1px solid #eff3f4; border-radius: 12px; padding: 18px 16px;
}
.stat-icon { font-size: 28px; flex-shrink: 0; }
.stat-body { flex: 1; }
.stat-value { font-size: 24px; font-weight: 800; color: #0f1419; line-height: 1.1; }
.stat-label { font-size: 12px; color: #8899a6; margin-top: 2px; }

.panel-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 16px; }
.panel { background: #fff; border: 1px solid #eff3f4; border-radius: 12px; padding: 20px; }
.panel h3 { font-size: 15px; font-weight: 700; color: #0f1419; margin: 0 0 16px 0; }

.breakdown-list { display: flex; flex-direction: column; gap: 12px; }
.breakdown-item { display: flex; align-items: center; gap: 10px; }
.dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.breakdown-label { font-size: 13px; color: #536471; width: 50px; }
.breakdown-value { font-size: 14px; font-weight: 700; color: #0f1419; width: 36px; text-align: right; }
.breakdown-bar { flex: 1; height: 6px; background: #eff3f4; border-radius: 3px; overflow: hidden; }
.breakdown-fill { height: 100%; border-radius: 3px; transition: width 0.5s; }

.chart-header { display: flex; justify-content: space-between; align-items: center; }
.bar-chart { display: flex; align-items: flex-end; gap: 8px; height: 180px; padding-top: 8px; }
.bar-col { flex: 1; display: flex; flex-direction: column; align-items: center; height: 100%; }
.bar-value { font-size: 11px; font-weight: 700; color: #0f1419; margin-bottom: 4px; }
.bar-track { flex: 1; width: 100%; max-width: 48px; background: #eff3f4; border-radius: 6px 6px 0 0; position: relative; overflow: hidden; }
.bar-fill { position: absolute; bottom: 0; left: 0; right: 0; background: #0f1419; border-radius: 6px 6px 0 0; transition: height 0.5s; }
.bar-label { font-size: 11px; color: #8899a6; margin-top: 6px; }

@media (max-width: 768px) {
    .stat-grid { grid-template-columns: repeat(2, 1fr); }
    .panel-row { grid-template-columns: 1fr; }
}
</style>
