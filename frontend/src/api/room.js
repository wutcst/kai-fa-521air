/**
 * 房间 API 模块
 * 封装房间相关 HTTP 请求
 */
import axios from 'axios'
import { useUserStore } from '@/stores/user'

const BASE_URL = '/api/rooms'

/**
 * 获取当前有效的 token
 * 优先从 Pinia store（当前标签页内存）读取，
 * 回退到 localStorage（兼容页面刷新后的初始化场景）
 */
function getToken() {
  try {
    const store = useUserStore()
    if (store.token) return store.token
  } catch (_) { /* store 尚未初始化 */ }
  return localStorage.getItem('snake_token') || ''
}

const request = axios.create()
request.interceptors.request.use(function(config) {
  var token = getToken()
  if (token) {
    config.headers.Authorization = 'Bearer ' + token
  }
  return config
})

/**
 * 获取在线人数
 */
export async function getOnlineCountApi() {
  var response = await request.get(BASE_URL + '/online-count')
  return response.data
}

/**
 * 获取房间列表
 */
export async function getRoomListApi(params) {
  if (!params) params = {}
  var response = await request.get(BASE_URL + '/list', { params: params })
  return response.data
}

/**
 * 创建房间
 */
export async function createRoomApi(roomData) {
  var response = await request.post(BASE_URL, roomData)
  return response.data
}

/**
 * 加入房间
 */
export async function joinRoomApi(roomId, password) {
  if (!password) password = ''
  var response = await request.post(BASE_URL + '/' + roomId + '/join', { password: password })
  return response.data
}

/**
 * 获取房间详情（用于快速匹配等无 sessionStorage 缓存的场景）
 */
export async function getRoomApi(roomId) {
  var response = await request.get(BASE_URL + '/' + roomId)
  return response.data
}

/**
 * 退出房间
 */
export async function leaveRoomApi(roomId) {
  var response = await request.post(BASE_URL + '/' + roomId + '/leave')
  return response.data
}

/**
 * 更新房间设置
 */
export async function updateRoomApi(roomId, settings) {
  var response = await request.put(BASE_URL + '/' + roomId, settings)
  return response.data
}

// ====== 快速匹配 API ======

const MATCH_BASE = '/api/matchmaking'

/**
 * 加入匹配队列
 */
export async function joinMatchmakingApi() {
  var response = await request.post(MATCH_BASE + '/join')
  return response.data
}

/**
 * 取消匹配
 */
export async function cancelMatchmakingApi() {
  var response = await request.post(MATCH_BASE + '/cancel')
  return response.data
}

/**
 * 查询匹配状态（轮询）
 */
export async function checkMatchStatusApi() {
  var response = await request.get(MATCH_BASE + '/status')
  return response.data
}
