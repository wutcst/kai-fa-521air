/**
 * 认证 API 模块
 * 封装登录、注册、退出等认证相关HTTP请求
 * 当前使用 mock 数据模拟，后续替换为真实后端请求
 */
import axios from 'axios'

// 后端API基础路径（通过Vite代理转发）
const BASE_URL = '/api/auth'

// ---- Mock 数据（后续对接后端时删除）----
const MOCK_USERS = [
  { id: '1', username: 'admin', password: '123456', nickname: '管理员', avatar: '', level: 10, score: 9999 },
  { id: '2', username: 'player1', password: '123456', nickname: '玩家一号', avatar: '', level: 5, score: 3500 },
  { id: '3', username: 'test', password: '123456', nickname: '测试用户', avatar: '', level: 1, score: 100 }
]

/**
 * 模拟延迟（模拟网络请求）
 */
function mockDelay(ms = 800) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

/**
 * 用户登录
 * @param {string} username 用户名
 * @param {string} password 密码
 * @returns {Promise<{user: object, token: string}>}
 */
export async function loginApi(username, password) {
  // ---- Mock 模式 ----
  await mockDelay()
  const user = MOCK_USERS.find(u => u.username === username && u.password === password)
  if (!user) {
    throw new Error('用户名或密码错误')
  }
  const { password: _, ...userData } = user
  const token = 'mock_token_' + user.id + '_' + Date.now()
  return { user: userData, token }

  // ---- 真实请求（后续解注释）----
  // const response = await axios.post(`${BASE_URL}/login`, { username, password })
  // return response.data
}

/**
 * 用户注册
 * @param {string} username 用户名
 * @param {string} password 密码
 * @param {string} nickname 昵称
 * @returns {Promise<{user: object, token: string}>}
 */
export async function registerApi(username, password, nickname) {
  // ---- Mock 模式 ----
  await mockDelay()
  // 检查用户名是否已存在
  if (MOCK_USERS.find(u => u.username === username)) {
    throw new Error('用户名已存在')
  }
  const newUser = {
    id: String(MOCK_USERS.length + 1),
    username,
    password,
    nickname: nickname || username,
    avatar: '',
    level: 1,
    score: 0
  }
  MOCK_USERS.push(newUser)
  const { password: _, ...userData } = newUser
  const token = 'mock_token_' + newUser.id + '_' + Date.now()
  return { user: userData, token }

  // ---- 真实请求（后续解注释）----
  // const response = await axios.post(`${BASE_URL}/register`, { username, password, nickname })
  // return response.data
}

/**
 * 退出登录（通知服务端）
 */
export async function logoutApi() {
  // Mock 模式不做任何事
  await mockDelay(300)
  return { success: true }

  // ---- 真实请求（后续解注释）----
  // const response = await axios.post(`${BASE_URL}/logout`)
  // return response.data
}

/**
 * 获取当前用户信息（通过token）
 */
export async function getUserInfoApi() {
  // Mock 模式
  await mockDelay(300)
  const savedUser = localStorage.getItem('snake_user')
  if (savedUser) {
    return JSON.parse(savedUser)
  }
  throw new Error('未登录')

  // ---- 真实请求（后续解注释）----
  // const response = await axios.get(`${BASE_URL}/me`)
  // return response.data
}
