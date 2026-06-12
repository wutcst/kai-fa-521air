/**
 * Pinia 用户状态管理
 * 管理：登录状态、用户信息、token
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  // ---- 状态 ----
  const token = ref(localStorage.getItem('snake_token') || '')
  const userInfo = ref({
    id: '',
    username: '',
    nickname: '',
    avatar: '',
    level: 1,
    score: 0,
  })
  const isLoggedIn = computed(() => !!token.value)

  // ---- 操作 ----
  /** 登录操作，保存 token 和用户信息 */
  function login(userData, authToken) {
    token.value = authToken
    userInfo.value = { ...userInfo.value, ...userData }
    localStorage.setItem('snake_token', authToken)
    localStorage.setItem('snake_user', JSON.stringify(userData))
  }

  /** 退出登录，清除所有状态 */
  function logout() {
    token.value = ''
    userInfo.value = {
      id: '',
      username: '',
      nickname: '',
      avatar: '',
      level: 1,
      score: 0,
    }
    localStorage.removeItem('snake_token')
    localStorage.removeItem('snake_user')
  }

  /** 初始化时从 localStorage 恢复登录状态 */
  function restoreSession() {
    const savedToken = localStorage.getItem('snake_token')
    const savedUser = localStorage.getItem('snake_user')
    if (savedToken && savedUser) {
      token.value = savedToken
      try {
        userInfo.value = { ...userInfo.value, ...JSON.parse(savedUser) }
      } catch (e) {
        // 数据损坏则清除
        logout()
      }
    }
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    login,
    logout,
    restoreSession,
  }
})
