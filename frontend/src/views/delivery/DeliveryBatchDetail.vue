<template>
    <div class="detail-container">
        <div class="page-header">
            <h2>运输批次详情</h2>
            <div class="batch-info">
                <span>批次时间：{{ formatTime(batchInfo.deliveryTime) }}</span>
                <span>订单数量：{{ batchInfo.orderCount }}</span>
            </div>
        </div>

        <!-- 地图容器 -->
        <div class="map-section">
            <div id="map-container" class="map-container"></div>
            <div v-if="routeInfo" class="route-info">
                <div class="route-info-row">
                    <div class="info-item">
                        <span class="label">总距离：</span>
                        <span class="value">{{ routeInfo.distance }}</span>
                    </div>
                    <div class="info-item">
                        <span class="label">预计时间：</span>
                        <span class="value">{{ routeInfo.duration }}</span>
                    </div>
                </div>
                <!-- 当前位置显示 -->
                <div v-if="isDelivering || deliveryCompleted" class="route-info-row">
                    <div class="info-item">
                        <span class="label">当前位置：</span>
                        <span class="value">{{ currentLocation.address }}</span>
                    </div>
                    <div v-if="isDelivering && currentLocation.remainingTime" class="info-item">
                        <span class="label">剩余时间：</span>
                        <span class="value">{{ formatRemainingTime(currentLocation.remainingTime) }}</span>
                    </div>
                </div>
            </div>
        </div>

        <!-- 配送点列表 -->
        <div class="delivery-points">
            <h3>配送路线</h3>
            <div class="point-list">
                <!-- 起点 - 仓库 -->
                <div class="point-item start-point">
                    <div class="point-marker">起</div>
                    <div class="point-content">
                        <div class="point-title">{{ warehouseInfo.name }}</div>
                        <div class="point-address">{{ warehouseInfo.address }}</div>
                    </div>
                </div>

                <!-- 配送点 -->
                <div
                    v-for="(order, index) in orders"
                    :key="order.orderId"
                    class="point-item"
                >
                    <div class="point-marker">{{ index + 1 }}</div>
                    <div class="point-content">
                        <div class="point-title">
                            订单号：{{ order.orderId }} - {{ order.customerName }}
                        </div>
                        <div class="point-address">
                            {{ order.address?.province }} {{ order.address?.city }}
                            {{ order.address?.district }} {{ order.address?.detailAddress }}
                        </div>
                        <div class="point-product">{{ order.productName }} × {{ order.quantity }}</div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
/* global TMap */
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()

// 批次信息
const batchInfo = ref({
    deliveryTime: '',
    orderCount: 0
})

// 仓库信息
const warehouseInfo = ref({
    id: null,
    name: '',
    address: '',
    longitude: null,
    latitude: null
})

// 订单列表
const orders = ref([])

// 路线信息
const routeInfo = ref(null)

// 地图实例
let map = null
let startMarker = null
let deliveryMarkers = []
let routePolyline = null
let deliveryTruckMarker = null  // 配送员货车marker

// 配送动画控制
let animationTimer = null
let currentPathIndex = 0
let routePath = []  // 存储完整路径点

// 配送状态
const isDelivering = ref(false)
const deliveryCompleted = ref(false)
const currentLocation = ref({
    address: '准备中...',
    lat: 0,
    lng: 0,
    remainingTime: 0
})

// 获取配送员所属仓库ID
const getCurrentUserWarehouseId = () => {
    const userInfoStr = sessionStorage.getItem('userInfo')
    if (userInfoStr) {
        const userInfo = JSON.parse(userInfoStr)
        if (userInfo.role === 'driver' && userInfo.warehouseId) {
            return userInfo.warehouseId
        }
    }
    return null
}

// 获取当前配送员ID
const getCurrentDeliveryPersonnelId = () => {
    const userInfoStr = sessionStorage.getItem('userInfo')
    if (userInfoStr) {
        const userInfo = JSON.parse(userInfoStr)
        if (userInfo.role === 'driver' && userInfo.id) {
            return userInfo.id
        }
    }
    return null
}

// 获取仓库详情
const fetchWarehouseInfo = async (warehouseId) => {
    try {
        const response = await request.get(`/warehouse/${warehouseId}`)
        if (response.success && response.data) {
            warehouseInfo.value = response.data
            return response.data
        }
    } catch (error) {
        console.error('获取仓库信息失败:', error)
    }
    return null
}

// 获取批次订单列表
const fetchBatchOrders = async () => {
    try {
        // 首先尝试从路由状态中获取批次数据
        const batchData = window.history.state.batchData

        if (batchData && batchData.orders && batchData.orders.length > 0) {
            // 如果有批次数据，直接使用
            orders.value = batchData.orders
            batchInfo.value = {
                deliveryTime: batchData.startedAt || batchData.createdAt || '未知',
                orderCount: batchData.orders.length
            }
            console.log('使用路由状态中的批次数据:', batchData)
            return batchData.orders
        }

        // 如果没有路由状态数据，尝试通过API获取
        const batchId = route.params.batchId || route.query.batchId
        if (!batchId) {
            console.error('缺少批次ID参数，返回批次列表')
            router.push('/driver/delivery-batch')
            return []
        }

        // 调用API获取批次列表，然后找到对应的批次
        const deliveryPersonnelId = getCurrentDeliveryPersonnelId()
        if (!deliveryPersonnelId) {
            console.error('无法获取配送员ID')
            router.push('/')
            return []
        }

        console.log('通过API获取批次数据，deliveryPersonnelId:', deliveryPersonnelId, 'batchId:', batchId)
        const response = await request.get(`/orders/delivery-batches-with-status?deliveryPersonnelId=${deliveryPersonnelId}`)

        if (response.code === 200 && response.data) {
            // 找到对应的批次
            const batch = response.data.find(b => b.batchId === parseInt(batchId))
            if (batch && batch.orders && batch.orders.length > 0) {
                orders.value = batch.orders
                batchInfo.value = {
                    deliveryTime: batch.startedAt || batch.createdAt || '未知',
                    orderCount: batch.orders.length
                }
                console.log('通过API获取到批次订单:', batch.orders.length, '个')
                return batch.orders
            } else {
                console.error('未找到对应的批次，batchId:', batchId)
                router.push('/driver/delivery-batch')
                return []
            }
        } else {
            console.error('API返回错误:', response)
            router.push('/driver/delivery-batch')
            return []
        }
    } catch (error) {
        console.error('获取批次订单失败:', error)
        return []
        throw error
    }
}

// 初始化地图
const initMap = (warehouse) => {
    if (!warehouse || !warehouse.longitude || !warehouse.latitude) {
        console.error('仓库经纬度信息不完整')
        return
    }

    // 创建地图实例
    const center = new window.TMap.LatLng(warehouse.latitude, warehouse.longitude)
    map = new window.TMap.Map(document.getElementById('map-container'), {
        zoom: 12,
        center: center
    })

    console.log('地图初始化成功', map)

    // 添加仓库标记（起点）- 使用简单的红色圆点
    startMarker = new window.TMap.MultiMarker({
        map: map,
        styles: {
            start: new window.TMap.MarkerStyle({
                width: 40,
                height: 40,
                anchor: { x: 20, y: 40 },
                // 使用简单的红色圆点，避免中文字符编码问题
                src: 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(`
                    <svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" viewBox="0 0 40 40">
                        <circle cx="20" cy="20" r="18" fill="#FF6B6B" stroke="white" stroke-width="3"/>
                        <text x="20" y="26" text-anchor="middle" fill="white" font-size="16" font-weight="bold">起</text>
                    </svg>
                `)
            })
        },
        geometries: [{
            id: 'start',
            styleId: 'start',
            position: center
        }]
    })

    console.log('起点标记添加成功', startMarker)
}

// 添加配送点标记
const addDeliveryMarkers = (orderList) => {
    if (!map || !orderList || orderList.length === 0) return

    console.log('开始添加配送点标记', orderList.length)

    const geometries = []
    const styles = {}

    orderList.forEach((order, index) => {
        if (order.address && order.address.longitude && order.address.latitude) {
            const styleId = `marker-${index}`

            // 为每个标记创建样式 - 使用encodeURIComponent而不是btoa
            styles[styleId] = new window.TMap.MarkerStyle({
                width: 32,
                height: 32,
                anchor: { x: 16, y: 32 },
                src: 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(`
                    <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 32 32">
                        <circle cx="16" cy="16" r="14" fill="#000000" stroke="white" stroke-width="3"/>
                        <text x="16" y="21" text-anchor="middle" fill="white" font-size="14" font-weight="bold">${index + 1}</text>
                    </svg>
                `)
            })

            geometries.push({
                id: `marker-${index}`,
                styleId: styleId,
                position: new window.TMap.LatLng(order.address.latitude, order.address.longitude),
                properties: {
                    title: `配送点${index + 1}`,
                    customerName: order.customerName,
                    productName: order.productName
                }
            })
        }
    })

    if (geometries.length > 0) {
        const markerLayer = new window.TMap.MultiMarker({
            map: map,
            styles: styles,
            geometries: geometries
        })
        deliveryMarkers.push(markerLayer)
        console.log('配送点标记添加成功', geometries.length)
    }
}

// 创建配送员货车marker
const createDeliveryTruck = (startPosition) => {
    if (deliveryTruckMarker) {
        deliveryTruckMarker.setMap(null)
    }

    deliveryTruckMarker = new window.TMap.MultiMarker({
        map: map,
        styles: {
            truck: new window.TMap.MarkerStyle({
                width: 48,
                height: 48,
                anchor: { x: 24, y: 24 },
                src: 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(`
                    <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 48 48">
                        <rect x="8" y="16" width="24" height="16" fill="#FF6B6B" stroke="white" stroke-width="2" rx="2"/>
                        <rect x="32" y="20" width="8" height="12" fill="#FF6B6B" stroke="white" stroke-width="2" rx="1"/>
                        <circle cx="16" cy="34" r="3" fill="#333" stroke="white" stroke-width="1"/>
                        <circle cx="32" cy="34" r="3" fill="#333" stroke="white" stroke-width="1"/>
                        <rect x="12" y="18" width="6" height="5" fill="#87CEEB" stroke="white" stroke-width="1"/>
                        <rect x="20" y="18" width="6" height="5" fill="#87CEEB" stroke="white" stroke-width="1"/>
                    </svg>
                `)
            })
        },
        geometries: [{
            id: 'delivery-truck',
            styleId: 'truck',
            position: startPosition
        }]
    })

    console.log('配送员货车marker创建成功')
}

// 更新货车位置
const updateTruckPosition = (position) => {
    if (deliveryTruckMarker) {
        deliveryTruckMarker.updateGeometries([{
            id: 'delivery-truck',
            styleId: 'truck',
            position: position
        }])
    }
}

// 逆地理编码 - 获取地址
const getAddressFromLocation = async (lat, lng) => {
    try {
        const callbackName = 'geocoderCallback_' + Date.now()
        const url = `https://apis.map.qq.com/ws/geocoder/v1/?location=${lat},${lng}&key=SEWBZ-W4R6N-V7AFD-SWAI5-E7BRZ-FKBAH&output=jsonp&callback=${callbackName}`

        await new Promise((resolve, reject) => {
            window[callbackName] = (data) => {
                delete window[callbackName]
                document.body.removeChild(script)

                if (data.status === 0 && data.result) {
                    currentLocation.value = {
                        address: data.result.address,
                        lat: lat,
                        lng: lng
                    }
                    resolve()
                } else {
                    console.error('逆地理编码失败:', data)
                    reject(new Error('逆地理编码失败'))
                }
            }

            const script = document.createElement('script')
            script.src = url
            script.onerror = () => {
                delete window[callbackName]
                document.body.removeChild(script)
                reject(new Error('逆地理编码请求失败'))
            }
            document.body.appendChild(script)
        })
    } catch (error) {
        console.error('获取地址失败:', error)
    }
}

// 开始配送动画
const startDeliveryAnimation = () => {
    if (!routePath || routePath.length === 0) {
        console.error('路径数据为空')
        return
    }

    isDelivering.value = true
    currentPathIndex = 0

    // 创建货车marker
    createDeliveryTruck(routePath[0])

    // 计算每个点的间隔时间（毫秒）
    const totalPoints = routePath.length
    const totalDurationMinutes = parseInt(routeInfo.value.duration) || 60
    const intervalTime = (totalDurationMinutes * 60 * 1000) / totalPoints

    console.log(`开始配送动画，总点数：${totalPoints}，间隔时间：${intervalTime}ms`)

    // 定时器控制移动
    animationTimer = setInterval(() => {
        if (currentPathIndex >= routePath.length - 1) {
            // 到达终点
            stopDeliveryAnimation()
            completeDelivery()
            return
        }

        currentPathIndex++
        const currentPosition = routePath[currentPathIndex]

        // 更新marker位置
        updateTruckPosition(currentPosition)

        // 每10个点更新一次地址（避免频繁调用API）
        if (currentPathIndex % 10 === 0) {
            const lat = currentPosition.getLat()
            const lng = currentPosition.getLng()
            getAddressFromLocation(lat, lng)
        }

        // 地图中心跟随货车
        map.setCenter(currentPosition)

    }, intervalTime)
}

// 停止配送动画
const stopDeliveryAnimation = () => {
    if (animationTimer) {
        clearInterval(animationTimer)
        animationTimer = null
    }
}

// 开始运输
const handleStartDelivery = async () => {
    if (isDelivering.value || deliveryCompleted.value) {
        return
    }

    try {
        // 调用后端API开始配送
        const response = await request.post('/delivery-batch/start', {
            deliveryTime: route.query.deliveryTime,
            warehouseId: getCurrentUserWarehouseId()
        })

        if (response.code !== 200) {
            alert(response.message || '开始运输失败')
            return
        }

        console.log('开始运输成功，路线ID:', response.data)

        // 从后端获取路径数据并绘制
        await fetchAndDrawRoute()

        // 开始位置轮询
        startLocationPolling()

        console.log('开始运输')
    } catch (error) {
        console.error('开始运输失败:', error)
        alert('开始运输失败: ' + (error.message || '未知错误'))
    }
}

// 完成配送
const completeDelivery = async () => {
    try {
        deliveryCompleted.value = true
        isDelivering.value = false

        // 更新最终位置
        const finalPosition = routePath[routePath.length - 1]
        const lat = finalPosition.getLat()
        const lng = finalPosition.getLng()
        await getAddressFromLocation(lat, lng)

        // TODO: 调用后端API更新批次状态为"已完成"
        // const response = await request.post('/delivery-batch/complete', { batchId: xxx })

        alert('配送完成！')
        console.log('配送完成')
    } catch (error) {
        console.error('完成配送失败:', error)
    }
}

// 从后端获取路径并绘制
const fetchAndDrawRoute = async () => {
    try {
        const response = await request.get('/delivery-batch/route-detail', {
            params: {
                deliveryTime: route.query.deliveryTime,
                warehouseId: getCurrentUserWarehouseId()
            }
        })

        if (response.code !== 200 || !response.data) {
            console.error('获取路径失败:', response.message)
            return
        }

        const { polyline, totalDistance, totalDuration } = response.data
        console.log('获取到路径数据:', polyline)

        // 解压polyline
        const coors = [...polyline]
        for (let i = 2; i < coors.length; i++) {
            coors[i] = coors[i - 2] + coors[i] / 1000000
        }

        // 转换为TMap.LatLng对象
        const path = []
        for (let i = 0; i < coors.length; i += 2) {
            if (i + 1 < coors.length) {
                const lat = coors[i]
                const lng = coors[i + 1]
                path.push(new window.TMap.LatLng(lat, lng))
            }
        }

        console.log('解压后路径点数量:', path.length)
        routePath = path

        // 绘制路线
        if (routePolyline) {
            routePolyline.setMap(null)
        }

        routePolyline = new window.TMap.MultiPolyline({
            map: map,
            styles: {
                route: new window.TMap.PolylineStyle({
                    color: '#3777FF',
                    width: 6,
                    borderWidth: 2,
                    borderColor: '#FFF',
                    lineCap: 'round'
                })
            },
            geometries: [{
                id: 'route',
                styleId: 'route',
                paths: path
            }]
        })

        console.log('路线绘制成功')

        // 更新路线信息
        const distance = totalDistance + ' 公里'
        const hours = Math.floor(totalDuration / 60)
        const minutes = totalDuration % 60
        let duration = ''
        if (hours > 0 && minutes > 0) {
            duration = `${hours} 小时 ${minutes} 分钟`
        } else if (hours > 0) {
            duration = `${hours} 小时`
        } else {
            duration = `${minutes} 分钟`
        }

        routeInfo.value = {
            distance: distance,
            duration: duration
        }

        console.log('路线信息:', routeInfo.value)

    } catch (error) {
        console.error('获取路径失败:', error)
    }
}

// 规划路线(已废弃，保留用于兼容)
const planRoute = async (warehouse, orderList) => {
    console.log('规划路线函数已废弃，路径将在开始运输时从后端获取')
}

// 格式化时间
const formatTime = (time) => {
    if (!time) return '未知'
    const date = new Date(time)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
}

// 格式化剩余时间（分钟转换为易读格式）
const formatRemainingTime = (minutes) => {
    if (!minutes || minutes <= 0) return '即将到达'
    const totalMinutes = Math.ceil(minutes)
    if (totalMinutes >= 60) {
        const hours = Math.floor(totalMinutes / 60)
        const mins = totalMinutes % 60
        return mins > 0 ? `${hours}小时${mins}分钟` : `${hours}小时`
    }
    return `${totalMinutes}分钟`
}

// 页面加载
onMounted(async () => {
    console.log('页面开始加载')

    // 等待腾讯地图SDK加载
    const waitForTMap = () => {
        return new Promise((resolve, reject) => {
            if (window.TMap) {
                console.log('腾讯地图SDK已加载')
                resolve()
                return
            }

            let attempts = 0
            const maxAttempts = 50
            const checkInterval = setInterval(() => {
                attempts++
                if (window.TMap) {
                    console.log('腾讯地图SDK加载成功')
                    clearInterval(checkInterval)
                    resolve()
                } else if (attempts >= maxAttempts) {
                    clearInterval(checkInterval)
                    reject(new Error('腾讯地图SDK加载超时'))
                }
            }, 100)
        })
    }

    try {
        await waitForTMap()
    } catch (error) {
        console.error('腾讯地图SDK加载失败:', error)
        return
    }

    // 1. 获取仓库ID
    const warehouseId = getCurrentUserWarehouseId()
    if (!warehouseId) {
        console.error('无法获取仓库ID')
        router.push('/')
        return
    }
    console.log('仓库ID:', warehouseId)

    // 2. 获取仓库详情
    const warehouse = await fetchWarehouseInfo(warehouseId)
    if (!warehouse) {
        console.error('获取仓库信息失败')
        return
    }
    console.log('仓库信息:', warehouse)

    // 3. 获取批次订单
    let orderList
    try {
        orderList = await fetchBatchOrders()
    } catch (error) {
        console.error('获取批次订单失败，停止页面加载')
        return
    }

    if (!orderList || orderList.length === 0) {
        console.warn('该批次没有订单')
        router.push('/driver/delivery-batch')
        return
    }
    console.log('订单列表:', orderList)

    // 4. 初始化地图
    initMap(warehouse)

    // 5. 添加配送点标记
    addDeliveryMarkers(orderList)

    // 6. 检查是否有正在进行的配送，如果有则获取路径并开始轮询
    await checkDeliveryStatus()
})

// 检查配送状态
const checkDeliveryStatus = async () => {
    try {
        const response = await request.get('/delivery-batch/current-location', {
            params: {
                deliveryTime: route.query.deliveryTime,
                warehouseId: getCurrentUserWarehouseId()
            }
        })

        if (response.code === 200 && response.data) {
            const { status } = response.data

            if (status === 'DELIVERING') {
                // 有正在进行的配送，获取路径并开始轮询
                isDelivering.value = true
                await fetchAndDrawRoute()
                startLocationPolling()
                console.log('检测到正在进行的配送，开始轮询')
            } else if (status === 'COMPLETED') {
                // 配送已完成
                deliveryCompleted.value = true
                await fetchAndDrawRoute()
                // 显示最终位置
                fetchCurrentLocation()
            }
        }
    } catch (error) {
        console.log('暂无配送记录')
    }
}

// 开始位置轮询
const startLocationPolling = () => {
    // 立即获取一次
    fetchCurrentLocation()

    // 开始定时轮询（每3秒）
    if (animationTimer) {
        clearInterval(animationTimer)
    }

    animationTimer = setInterval(() => {
        fetchCurrentLocation()
    }, 3000)

    console.log('开始位置轮询')
}

// 获取当前位置
const fetchCurrentLocation = async () => {
    try {
        const response = await request.get('/delivery-batch/current-location', {
            params: {
                deliveryTime: route.query.deliveryTime,
                warehouseId: getCurrentUserWarehouseId()
            }
        })

        if (response.code === 200 && response.data) {
            const { latitude, longitude, address, status, remainingTime } = response.data

            // 更新位置数据
            currentLocation.value = {
                latitude,
                longitude,
                address,
                remainingTime: remainingTime || 0
            }

            // 更新或创建货车marker
            const position = new window.TMap.LatLng(latitude, longitude)
            if (!deliveryTruckMarker) {
                createDeliveryTruck(position)
            } else {
                updateTruckPosition(position)
            }

            // 地图中心跟随
            map.setCenter(position)

            // 更新状态
            if (status === 'COMPLETED') {
                deliveryCompleted.value = true
                isDelivering.value = false
                // 停止轮询
                if (animationTimer) {
                    clearInterval(animationTimer)
                    animationTimer = null
                }
                console.log('配送完成')
            }
        }
    } catch (error) {
        console.error('获取位置失败:', error)
    }
}

// 组件卸载时停止轮询
onUnmounted(() => {
    if (animationTimer) {
        clearInterval(animationTimer)
        animationTimer = null
    }
})
</script>

<style scoped>
.detail-container {
    width: 100%;
    max-width: 1400px;
    margin: 0 auto;
    padding: 20px;
}

.page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
}

h2 {
    font-size: 24px;
    font-weight: 600;
    color: #1f2937;
    margin: 0;
}

.batch-info {
    display: flex;
    gap: 24px;
    font-size: 14px;
    color: #666;
}

/* 地图区域 */
.map-section {
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    padding: 16px;
    margin-bottom: 24px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.map-container {
    width: 100%;
    height: 500px;
    border-radius: 6px;
    overflow: hidden;
}

.route-info {
    display: flex;
    flex-direction: column;
    gap: 12px;
    margin-top: 16px;
    padding-top: 16px;
    border-top: 1px solid #e5e7eb;
}

.route-info-row {
    display: flex;
    gap: 32px;
    align-items: center;
}

.info-item {
    display: flex;
    align-items: center;
    gap: 8px;
}

.info-item .label {
    font-size: 14px;
    color: #666;
    font-weight: 500;
}

.info-item .value {
    font-size: 16px;
    color: #1f2937;
    font-weight: 600;
}

/* 配送状态 */
.delivery-status {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 16px;
    background: #dbeafe;
    border-radius: 6px;
    font-size: 14px;
    font-weight: 600;
    color: #1e40af;
}

.delivery-status.completed {
    background: #d1fae5;
    color: #065f46;
}

.status-icon {
    font-size: 18px;
}

/* 配送点列表 */
.delivery-points {
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    padding: 20px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.delivery-points h3 {
    font-size: 18px;
    font-weight: 600;
    color: #1f2937;
    margin: 0 0 16px 0;
}

.point-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.point-item {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    padding: 12px;
    background: #f9fafb;
    border: 1px solid #e5e7eb;
    border-radius: 6px;
    transition: all 0.2s;
}

.point-item:hover {
    background: #f3f4f6;
    border-color: #d1d5db;
}

.point-marker {
    flex-shrink: 0;
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #ffffff;
    border: 2px solid #000000;
    border-radius: 50%;
    font-size: 14px;
    font-weight: 600;
    color: #000000;
}

.point-content {
    flex: 1;
}

.point-title {
    font-size: 14px;
    font-weight: 600;
    color: #1f2937;
    margin-bottom: 4px;
}

.point-address {
    font-size: 13px;
    color: #666;
    line-height: 1.5;
}

.point-product {
    font-size: 12px;
    color: #999;
    margin-top: 4px;
}

/* 响应式设计 */
@media (max-width: 768px) {
    .page-header {
        flex-direction: column;
        align-items: flex-start;
        gap: 12px;
    }

    .batch-info {
        flex-direction: column;
        gap: 8px;
    }

    .map-container {
        height: 400px;
    }

    .route-info {
        flex-direction: column;
        gap: 12px;
    }

    .route-info-row {
        flex-direction: column;
        gap: 12px;
        align-items: flex-start;
    }
}
</style>
