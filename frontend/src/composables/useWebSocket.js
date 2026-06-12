/**
 * useWebSocket 组合式函数（增强版）
 * WebSocket 连接管理：支持 mock 模式、完整消息类型、连接状态指示
 *
 * 消息类型（客户端 → 服务端）：
 *   join_room, leave_room, ready, start_game, change_direction,
 *   speed_boost, use_item, chat_message, ping
 *
 * 消息类型（服务端 → 客户端）：
 *   room_update, game_state, player_eliminated, player_kill,
 *   game_over, countdown, chat_broadcast, pong, error
 */
import { ref, onUnmounted } from 'vue'

/** 是否启用 mock 模式 */
const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/**
 * @param {string} url WebSocket 服务端地址
 * @param {object} handlers 消息处理器 { type: (data) => {} }
 * @param {object} options 配置项
 */
export function useWebSocket(url, handlers = {}, options = {}) {
  const ws = ref(null)
  const isConnected = ref(false)
  const isReconnecting = ref(false)
  const reconnectCount = ref(0)
  const maxReconnect = options.maxReconnect || 5
  const reconnectInterval = options.reconnectInterval || 3000
  const heartbeatInterval = options.heartbeatInterval || 30000
  const heartbeatTimeout = options.heartbeatTimeout || 10000

  let heartbeatTimer = null
  let heartbeatTimeoutTimer = null
  let reconnectTimer = null

  // ---- 连接 ----
  function connect() {
    if (USE_MOCK) {
      // Mock 模式：直接标记为已连接
      console.log('[WebSocket] Mock 模式，跳过真实连接')
      isConnected.value = true
      isReconnecting.value = false
      reconnectCount.value = 0
      return
    }

    if (ws.value?.readyState === WebSocket.OPEN) return

    try {
      ws.value = new WebSocket(url)
    } catch (e) {
      console.error('[WebSocket] 连接失败:', e)
      scheduleReconnect()
      return
    }

    ws.value.onopen = () => {
      console.log('[WebSocket] 连接成功:', url)
      isConnected.value = true
      isReconnecting.value = false
      reconnectCount.value = 0
      startHeartbeat()
    }

    ws.value.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data)
        const { type, data } = message

        // 分发到对应处理器
        if (handlers[type]) {
          handlers[type](data)
        } else if (handlers['*']) {
          handlers['*'](message)
        }
      } catch (e) {
        console.error('[WebSocket] 消息解析失败:', e)
      }
    }

    ws.value.onclose = (event) => {
      isConnected.value = false
      stopHeartbeat()
      if (event.code !== 1000) {
        scheduleReconnect()
      }
    }

    ws.value.onerror = () => {
      isConnected.value = false
    }
  }

  // ---- 发送消息 ----
  function send(type, data = {}) {
    if (USE_MOCK) {
      // Mock 模式：通过 handlers 的回调模拟接收
      console.log('[Mock WS] 发送:', type, data)
      return
    }

    if (ws.value?.readyState === WebSocket.OPEN) {
      ws.value.send(JSON.stringify({ type, data }))
    } else {
      console.warn('[WebSocket] 未连接, 无法发送:', type)
    }
  }

  // ---- 断开 ----
  function disconnect() {
    clearTimeout(reconnectTimer)
    reconnectCount.value = maxReconnect
    stopHeartbeat()
    if (ws.value) {
      ws.value.close(1000, '主动断开')
      ws.value = null
    }
    isConnected.value = false
    isReconnecting.value = false
  }

  // ---- 心跳 ----
  function startHeartbeat() {
    stopHeartbeat()
    heartbeatTimer = setInterval(() => {
      send('ping')
      // 超时检测
      heartbeatTimeoutTimer = setTimeout(() => {
        console.warn('[WebSocket] 心跳超时，断开重连')
        if (ws.value) {
          ws.value.close(3001, '心跳超时')
        }
      }, heartbeatTimeout)
    }, heartbeatInterval)
  }

  function stopHeartbeat() {
    if (heartbeatTimer) {
      clearInterval(heartbeatTimer)
      heartbeatTimer = null
    }
    if (heartbeatTimeoutTimer) {
      clearTimeout(heartbeatTimeoutTimer)
      heartbeatTimeoutTimer = null
    }
  }

  // ---- 指数退避重连 ----
  function scheduleReconnect() {
    if (reconnectCount.value >= maxReconnect) {
      console.warn('[WebSocket] 重连次数达上限')
      return
    }
    isReconnecting.value = true
    reconnectCount.value++
    const delay = reconnectInterval * Math.pow(1.5, reconnectCount.value - 1)
    console.log(`[WebSocket] ${(delay / 1000).toFixed(1)}秒后第${reconnectCount.value}次重连...`)
    reconnectTimer = setTimeout(() => {
      connect()
    }, delay)
  }

  onUnmounted(() => disconnect())

  return {
    ws,
    isConnected,
    isReconnecting,
    connect,
    send,
    disconnect,
    reconnectCount,
  }
}

/**
 * Mock WebSocket 消息模拟器
 * 用于本地调试，模拟服务端推送
 */
export function useMockWebSocket(handlers = {}) {
  const isConnected = ref(true)

  // 模拟发送消息的服务端回执
  function send(type, data = {}) {
    console.log('[Mock WS] 发送:', type, data)
  }

  // 模拟接收消息
  function mockReceive(type, data = {}) {
    console.log('[Mock WS] 接收:', type, data)
    if (handlers[type]) {
      handlers[type](data)
    }
    if (handlers['*']) {
      handlers['*']({ type, data })
    }
  }

  return {
    isConnected,
    send,
    mockReceive,
  }
}
