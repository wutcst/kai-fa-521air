/**
 * 游戏 API 模块
 * 封装游戏相关HTTP请求（游戏结算、历史记录、玩家统计等）
 */
import axios from 'axios'
import { useUserStore } from '@/stores/user'

const BASE_URL = '/api/games'

function getToken() {
  try {
    const store = useUserStore()
    if (store.token) return store.token
  } catch (_) {
    /* store 尚未初始化 */
  }
  return localStorage.getItem('snake_token') || ''
}

const request = axios.create()
request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/**
 * 获取游戏结算数据（通过游戏ID获取详情含排名）
 * @param {string|number} gameId 游戏ID
 */
export async function getGameResultApi(gameId) {
  const response = await request.get(`${BASE_URL}/${gameId}`)
  return response.data
}

/**
 * 获取当前用户的对战历史
 * @param {number} page 页码
 * @param {number} size 每页条数
 */
export async function getMyHistoryApi(page = 1, size = 10) {
  const response = await request.get(`${BASE_URL}/my-history`, { params: { page, size } })
  return response.data
}

/**
 * 获取当前用户的统计概览
 */
export async function getMyStatsApi() {
  const response = await request.get(`${BASE_URL}/my-stats`)
  return response.data
}

/**
 * 获取所有游戏历史（全局）
 * @param {number} page 页码
 * @param {number} size 每页条数
 * @param {string} mode 模式筛选: multi/single/all
 */
export async function getGameHistoryApi(page = 1, size = 10, mode = 'all') {
  const response = await request.get(BASE_URL, { params: { page, size, mode } })
  return response.data
}

// ====== 排行榜 API ======

const RANK_BASE = '/api/ranking'

/**
 * 获取排行榜列表
 * @param {string} mode 模式: overall(总榜) / multi(多人) / single(单人)
 * @param {number} page 页码
 * @param {number} size 每页条数
 */
export async function getRankingApi(mode = 'overall', page = 1, size = 20) {
  const response = await request.get(RANK_BASE, { params: { mode, page, size } })
  return response.data
}

/**
 * 获取当前用户的排名信息
 */
export async function getMyRankApi() {
  const response = await request.get(`${RANK_BASE}/my-rank`)
  return response.data
}
