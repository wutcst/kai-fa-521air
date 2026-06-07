/**
 * 游戏 API 模块
 * 封装游戏相关HTTP请求（获取结算数据、游戏历史等）
 * 当前为占位，后续对接后端
 */
import axios from 'axios'
import { useUserStore } from '@/stores/user'

const BASE_URL = '/api/games'

function getToken() {
  try {
    const store = useUserStore()
    if (store.token) return store.token
  } catch (_) { /* store 尚未初始化 */ }
  return localStorage.getItem('snake_token') || ''
}

const request = axios.create()
request.interceptors.request.use(config => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/**
 * 获取游戏结算数据
 * @param {string} gameId 游戏ID
 */
export async function getGameResultApi(gameId) {
  // ---- Mock 占位 ----
  await new Promise(resolve => setTimeout(resolve, 300))
  return {
    gameId,
    duration: 300,
    rankings: []
  }

  // const response = await request.get(`${BASE_URL}/${gameId}/result`)
  // return response.data
}

/**
 * 获取游戏历史记录
 * @param {number} page 页码
 * @param {number} size 每页条数
 */
export async function getGameHistoryApi(page = 1, size = 10) {
  await new Promise(resolve => setTimeout(resolve, 300))
  return { list: [], total: 0, page, size }

  // const response = await request.get(`${BASE_URL}/history`, { params: { page, size } })
  // return response.data
}
