/**
 * 房间 API 模块
 * 封装房间相关 HTTP 请求
 */
import axios from 'axios'

const BASE_URL = '/api/rooms'

const request = axios.create()
request.interceptors.request.use(function(config) {
  var token = localStorage.getItem('snake_token')
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
