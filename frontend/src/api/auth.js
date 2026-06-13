/**
 * 认证 API 模块
 * 封装登录、注册、退出等认证相关 HTTP 请求
 */
import axios from 'axios'
import { useUserStore } from '@/stores/user'

const BASE_URL = '/api/auth'

/**
 * 获取当前有效的 token
 * 优先从 Pinia store（当前标签页内存）读取，
 * 回退到 localStorage（兼容页面刷新后的初始化场景）
 */
function getToken() {
  try {
    const store = useUserStore()
    if (store.token) return store.token
  } catch (_) {
    /* store 尚未初始化 */
  }
  return localStorage.getItem('snake_token') || ''
}

// 带 token 的 axios 实例（用于 /me）
const authRequest = axios.create()
authRequest.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = 'Bearer ' + token
  }
  return config
})

/**
 * 用户登录
 */
export async function loginApi(username, password) {
  const response = await axios.post(BASE_URL + '/login', { username, password })
  return response.data
}

/**
 * 用户注册
 */
export async function registerApi(username, password, nickname) {
  const response = await axios.post(BASE_URL + '/register', { username, password, nickname })
  return response.data
}

/**
 * 退出登录
 */
export async function logoutApi() {
  const response = await axios.post(BASE_URL + '/logout')
  return response.data
}

/**
 * 获取当前用户信息（通过 token）
 */
export async function getUserInfoApi() {
  const response = await authRequest.get(BASE_URL + '/me')
  return response.data
}
