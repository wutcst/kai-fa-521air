/**
 * 认证 API 模块
 * 封装登录、注册、退出等认证相关 HTTP 请求
 */
import axios from 'axios'

const BASE_URL = '/api/auth'

// 带 token 的 axios 实例（用于 /me）
const authRequest = axios.create()
authRequest.interceptors.request.use(config => {
  const token = localStorage.getItem('snake_token')
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
