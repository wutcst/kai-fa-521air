/**
 * 路由配置
 * 定义应用的页面路由和导航守卫
 */
import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录 - 多人联机贪吃蛇' }
  },
  {
    path: '/lobby',
    name: 'Lobby',
    component: () => import('@/views/LobbyView.vue'),
    meta: { title: '游戏大厅', requiresAuth: true }
  },
  {
    path: '/room/:roomId',
    name: 'Room',
    component: () => import('@/views/RoomView.vue'),
    meta: { title: '游戏房间', requiresAuth: true }
  },
  {
    path: '/game/:roomId',
    name: 'Game',
    component: () => import('@/views/GameView.vue'),
    meta: { title: '游戏中', requiresAuth: true }
  },
  {
    path: '/result/:gameId',
    name: 'Result',
    component: () => import('@/views/ResultView.vue'),
    meta: { title: '结算页面', requiresAuth: true }
  },
  {
    path: '/history',
    name: 'History',
    component: () => import('@/views/HistoryView.vue'),
    meta: { title: '对战记录', requiresAuth: true }
  },
  {
    path: '/ranking',
    name: 'Ranking',
    component: () => import('@/views/RankingView.vue'),
    meta: { title: '排行榜', requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

/**
 * 全局前置守卫 - 检查登录状态
 * 当前使用 mock 模式，后续对接后端时替换为真实 token 校验
 */
router.beforeEach((to, from, next) => {
  // 设置页面标题
  document.title = to.meta.title || '多人联机贪吃蛇'

  // 检查是否需要登录权限
  if (to.meta.requiresAuth) {
    // 优先从 Pinia store（当前标签页内存）读取 token
    let token = ''
    try {
      const userStore = useUserStore()
      token = userStore.token
    } catch (_) { /* store 尚未初始化 */ }
    // 回退到 localStorage（兼容刷新后的初始化场景）
    if (!token) {
      token = localStorage.getItem('snake_token') || ''
    }
    if (!token) {
      // 未登录，跳转到登录页
      next({ name: 'Login', query: { redirect: to.fullPath } })
      return
    }
  }
  next()
})

export default router
