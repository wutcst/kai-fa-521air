/**
 * 房间 API 模块
 * 封装房间相关HTTP请求（创建、加入、退出、列表、设置）
 * 当前为占位，后续对接后端
 */
import axios from 'axios'

// 后端API基础路径
const BASE_URL = '/api/rooms'

// 创建axios实例（自动携带token）
const request = axios.create()
request.interceptors.request.use(config => {
  const token = localStorage.getItem('snake_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/**
 * 获取房间列表
 * @param {object} params 筛选条件 { page, size, status, keyword }
 */
export async function getRoomListApi(params = {}) {
  // ---- Mock 占位 ----
  await new Promise(resolve => setTimeout(resolve, 300))
  return {
    list: [],
    total: 0,
    page: params.page || 1,
    size: params.size || 10
  }

  // const response = await request.get(`${BASE_URL}/list`, { params })
  // return response.data
}

/**
 * 创建房间
 * @param {object} roomData { name, maxPlayers, hasPassword, password, gameDuration }
 */
export async function createRoomApi(roomData) {
  // ---- Mock 占位 ----
  await new Promise(resolve => setTimeout(resolve, 500))
  return {
    roomId: 'room_' + Date.now(),
    ...roomData,
    hostId: 'mock_host',
    status: 'waiting'
  }

  // const response = await request.post(BASE_URL, roomData)
  // return response.data
}

/**
 * 加入房间
 * @param {string} roomId 房间ID
 * @param {string} password 密码（可选）
 */
export async function joinRoomApi(roomId, password = '') {
  // ---- Mock 占位 ----
  await new Promise(resolve => setTimeout(resolve, 500))
  return { roomId, success: true }

  // const response = await request.post(`${BASE_URL}/${roomId}/join`, { password })
  // return response.data
}

/**
 * 退出房间
 * @param {string} roomId 房间ID
 */
export async function leaveRoomApi(roomId) {
  await new Promise(resolve => setTimeout(resolve, 300))
  return { success: true }

  // const response = await request.post(`${BASE_URL}/${roomId}/leave`)
  // return response.data
}

/**
 * 房主开始游戏
 * @param {string} roomId 房间ID
 */
export async function startGameApi(roomId) {
  await new Promise(resolve => setTimeout(resolve, 300))
  return { success: true, gameId: 'game_' + Date.now() }

  // const response = await request.post(`${BASE_URL}/${roomId}/start`)
  // return response.data
}

/**
 * 更新房间设置（仅房主）
 * @param {string} roomId 房间ID
 * @param {object} settings 房间设置
 */
export async function updateRoomApi(roomId, settings) {
  await new Promise(resolve => setTimeout(resolve, 300))
  return { success: true }

  // const response = await request.put(`${BASE_URL}/${roomId}`, settings)
  // return response.data
}
