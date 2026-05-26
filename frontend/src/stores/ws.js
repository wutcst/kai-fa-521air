import { defineStore } from 'pinia'
import { ref } from 'vue'

const DEFAULT_URL = import.meta.env.VITE_WS_URL || 'ws://localhost:8080/ws/game'

export const useWsStore = defineStore('ws', () => {
  const ws = ref(null)
  const isConnected = ref(false)
  const isConnecting = ref(false)
  const handlers = new Map()
  let pendingConnect = null

  function connect(url = DEFAULT_URL) {
    if (isConnected.value) return Promise.resolve()
    if (isConnecting.value && pendingConnect) return pendingConnect

    pendingConnect = new Promise((resolve, reject) => {
      isConnecting.value = true
      ws.value = new WebSocket(url)

      ws.value.onopen = () => {
        isConnected.value = true
        isConnecting.value = false
        pendingConnect = null
        resolve()
      }

      ws.value.onclose = () => {
        isConnected.value = false
        isConnecting.value = false
        pendingConnect = null
      }

      ws.value.onerror = (err) => {
        isConnected.value = false
        isConnecting.value = false
        pendingConnect = null
        reject(err)
      }

      ws.value.onmessage = (event) => {
        try {
          const msg = JSON.parse(event.data)
          dispatch(msg.type, msg.data, msg)
        } catch (e) {
          console.error('[WS] Failed to parse message', e)
        }
      }
    })

    return pendingConnect
  }

  function send(type, data = {}) {
    if (ws.value?.readyState === WebSocket.OPEN) {
      ws.value.send(JSON.stringify({ type, data }))
      return true
    }
    return false
  }

  function on(type, handler) {
    if (!handlers.has(type)) handlers.set(type, new Set())
    handlers.get(type).add(handler)
    return () => off(type, handler)
  }

  function off(type, handler) {
    if (!handlers.has(type)) return
    handlers.get(type).delete(handler)
  }

  function dispatch(type, data, raw) {
    const typed = handlers.get(type)
    if (typed) typed.forEach(fn => fn(data, raw))
    const any = handlers.get('*')
    if (any) any.forEach(fn => fn(data, raw))
  }

  function forceReconnect(url = DEFAULT_URL) {
    disconnect()
    return connect(url)
  }

  function disconnect() {
    if (ws.value) {
      ws.value.close(1000, 'client disconnect')
      ws.value = null
    }
    isConnected.value = false
    isConnecting.value = false
    pendingConnect = null
    handlers.clear()
  }

  return {
    connect,
    forceReconnect,
    send,
    on,
    off,
    disconnect,
    isConnected,
    isConnecting
  }
})
