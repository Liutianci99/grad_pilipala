import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Demo from '../views/demo.vue'

// Admin
import UserManagement from '../views/admin/UserManagement.vue'
import AdminOrderManagement from '../views/admin/OrderManagement.vue'
import DataAnalysis from '../views/admin/DataAnalysis.vue'

// Merchant
import InventoryManagement from '../views/merchant/InventoryManagement.vue'
import ProductListing from '../views/merchant/ProductListing.vue'
import ProductDelisting from '../views/merchant/ProductDelisting.vue'
import StockIn from '../views/merchant/StockIn.vue'
import MerchantOrderManagement from '../views/merchant/OrderManagement.vue'
import MerchantLogisticsQuery from '../views/merchant/LogisticsQuery.vue'

// Consumer
import Mall from '../views/general/Mall.vue'
import MyOrders from '../views/consumer/MyOrders.vue'
import ConsumerLogisticsQuery from '../views/consumer/LogisticsQuery.vue'
import AddressManagement from '../views/consumer/AddressManagement.vue'

// Driver
import PendingPickup from '../views/delivery/PendingPickup.vue'
import PendingDelivery from '../views/delivery/PendingDelivery.vue'
import DeliveryBatch from '../views/delivery/DeliveryBatch.vue'
import DeliveryBatchDetail from '../views/delivery/DeliveryBatchDetail.vue'
import HistoryTasks from '../views/delivery/HistoryTasks.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'login',
      component: Login,
      meta: { requiresAuth: false }
    },
    {
      path: '/demo',
      name: 'home',
      component: Demo,
      meta: { requiresAuth: true },
      children: [
        // Admin routes
        {
          path: '/admin/user-management',
          component: UserManagement
        },
        {
          path: '/admin/order-management',
          component: AdminOrderManagement
        },
        {
          path: '/admin/data-analysis',
          component: DataAnalysis
        },
        // Merchant routes
        {
          path: '/merchant/inventory-management',
          component: InventoryManagement
        },
        {
          path: '/merchant/product-listing',
          component: ProductListing
        },
        {
          path: '/merchant/product-delisting',
          component: ProductDelisting
        },
        {
          path: '/merchant/stock-in',
          component: StockIn
        },
        {
          path: '/merchant/order-management',
          component: MerchantOrderManagement
        },
        {
          path: '/merchant/logistics-query',
          component: MerchantLogisticsQuery
        },
        {
          path: '/merchant/logistics-query/:orderId',
          component: MerchantLogisticsQuery
        },
        // General routes
        {
          path: '/general/mall',
          component: Mall
        },
        // Consumer routes
        {
          path: '/consumer/my-orders',
          component: MyOrders
        },
        {
          path: '/consumer/logistics-query',
          component: ConsumerLogisticsQuery
        },
        {
          path: '/consumer/logistics-query/:orderId',
          component: ConsumerLogisticsQuery
        },
        {
          path: '/consumer/address-management',
          component: AddressManagement
        },
        // Driver routes
        {
          path: '/driver/pending-pickup',
          component: PendingPickup
        },
        {
          path: '/driver/pending-delivery',
          component: PendingDelivery
        },
        {
          path: '/driver/delivery-batch',
          component: DeliveryBatch
        },
        {
          path: '/driver/delivery-batch-detail/:batchId',
          component: DeliveryBatchDetail
        },
        {
          path: '/driver/history-tasks',
          component: HistoryTasks
        }
      ]
    }
  ]
})

// 路由守卫 - 检查登录状态
router.beforeEach((to, from, next) => {
  const token = sessionStorage.getItem('token')
  const isLoggedIn = !!token

  if (to.meta.requiresAuth !== false && !isLoggedIn) {
    // 需要登录但未登录，跳转到登录页
    next({ name: 'login' })
  } else if (to.name === 'login' && isLoggedIn) {
    // 已登录访问登录页，跳转到主页
    next({ name: 'home' })
  } else {
    next()
  }
})

export default router
