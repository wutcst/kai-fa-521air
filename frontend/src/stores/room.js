/**
 * Pinia 房间状态管理
 * 管理：当前房间信息、玩家列表、准备状态、聊天消息
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useUserStore } from './user'

export const useRoomStore = defineStore('room', () => {
  // ---- 状态 ----
  const roomId = ref('')
  const roomInfo = ref({
    name: '',
    hostId: '',
    playerCount: 0,
    maxPlayers: 6,
    status: 'waiting', // waiting | playing | finished
    hasPassword: false,
    gameDuration: 300 // 默认5分钟（秒）
  })
  const playerList = ref([])         // 房间内玩家列表
  const chatMessages = ref([])       // 聊天消息列表
  const isHost = computed(() => {
    const userStore = useUserStore()
    return roomInfo.value.hostId === userStore.userInfo.id
  })

  // ---- 操作 ----
  /** 设置房间信息 */
  function setRoomInfo(info) {
    roomId.value = info.roomId || roomId.value
    roomInfo.value = { ...roomInfo.value, ...info }
  }

  /** 更新玩家列表 */
  function setPlayerList(list) {
    playerList.value = list
  }

  /** 添加聊天消息 */
  function addMessage(msg) {
    chatMessages.value.push({
      ...msg,
      timestamp: Date.now()
    })
    // 只保留最近200条消息
    if (chatMessages.value.length > 200) {
      chatMessages.value.splice(0, chatMessages.value.length - 200)
    }
  }

  /** 重置房间状态（退出房间时） */
  function resetRoom() {
    roomId.value = ''
    roomInfo.value = {
      name: '',
      hostId: '',
      playerCount: 0,
      maxPlayers: 6,
      status: 'waiting',
      hasPassword: false,
      gameDuration: 300
    }
    playerList.value = []
    chatMessages.value = []
  }

  return {
    roomId,
    roomInfo,
    playerList,
    chatMessages,
    isHost,
    setRoomInfo,
    setPlayerList,
    addMessage,
    resetRoom
  }
})
