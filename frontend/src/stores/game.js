/**
 * Pinia 游戏状态管理
 * 管理：游戏进行中的实时数据、所有蛇的状态、食物/道具位置、积分榜
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useGameStore = defineStore('game', () => {
  // ---- 状态 ----
  const gameStatus = ref('idle')     // idle | countdown | playing | paused | finished
  const countdown = ref(0)           // 倒计时秒数
  const gameTime = ref(0)            // 已进行时间（秒）
  const totalTime = ref(300)         // 总局时（秒）

  // 所有蛇的状态（key: playerId）
  const snakes = ref({})
  // 所有食物位置 [{ x, y, type: 'normal'|'high' }]
  const foods = ref([])
  // 道具位置 [{ x, y, type: 'speed'|'shield'|'magnet' }]
  const items = ref([])
  // 实时积分榜
  const scoreBoard = ref([])
  // 我的蛇的ID
  const mySnakeId = ref('')

  const isPlaying = computed(() => gameStatus.value === 'playing')
  const isFinished = computed(() => gameStatus.value === 'finished')
  const mySnake = computed(() => snakes.value[mySnakeId.value] || null)

  // ---- 操作 ----
  /** 更新整个游戏状态（服务端推送的帧数据） */
  function updateGameState(state) {
    snakes.value = state.snakes || {}
    foods.value = state.foods || []
    items.value = state.items || []
    scoreBoard.value = state.scoreBoard || []
    gameTime.value = state.gameTime ?? gameTime.value
    gameStatus.value = state.gameStatus || gameStatus.value
  }

  /** 设置游戏状态 */
  function setGameStatus(status) {
    gameStatus.value = status
  }

  /** 设置倒计时 */
  function setCountdown(seconds) {
    countdown.value = seconds
  }

  /** 更新自己蛇的方向（发送到服务端） */
  function setMyDirection(direction) {
    if (snakes.value[mySnakeId.value]) {
      snakes.value[mySnakeId.value].direction = direction
    }
  }

  /** 重置游戏状态 */
  function resetGame() {
    gameStatus.value = 'idle'
    countdown.value = 0
    gameTime.value = 0
    totalTime.value = 300
    snakes.value = {}
    foods.value = []
    items.value = []
    scoreBoard.value = []
    mySnakeId.value = ''
  }

  return {
    gameStatus,
    countdown,
    gameTime,
    totalTime,
    snakes,
    foods,
    items,
    scoreBoard,
    mySnakeId,
    isPlaying,
    isFinished,
    mySnake,
    updateGameState,
    setGameStatus,
    setCountdown,
    setMyDirection,
    resetGame
  }
})
