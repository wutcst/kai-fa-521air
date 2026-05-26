<!--
  GameView.vue - 游戏对战页面（v2 - 小清新主题 + 双模式支持）
  模式：'multi' 多人对战 | 'single' 单人无尽
  碰撞规则（全部模式统一）：
    撞墙: 穿墙循环，不死亡
    撞自己: 直接穿过，不死亡不阻挡
    撞障碍物: 淘汰
    撞其他蛇: 淘汰（仅多人模式有对手）
-->
<template>
  <div class="game-view" @keydown="handleKeyDown" tabindex="0" ref="gameViewRef">
    <!-- ====== 顶部 HUD ====== -->
    <header class="game-hud">
      <!-- 倒计时/生存时间 -->
      <div :class="['hud-item', 'timer', { warning: isTimeWarning && gameMode === 'multi' }]">
        <el-icon><Clock /></el-icon>
        <span v-if="gameMode === 'multi'">{{ formatTime(remainingTime) }}</span>
        <span v-else>⏱ {{ formatTime(Math.floor(currentTime)) }}</span>
      </div>

      <!-- 排名（单人模式隐藏） -->
      <div class="hud-item rank" v-if="gameMode === 'multi'">
        <el-icon><Trophy /></el-icon>
        <span>排名 #{{ myRank }}</span>
      </div>
      <div class="hud-item rank" v-else>
        <span>🧘 单人无尽模式</span>
      </div>

      <!-- 分数 -->
      <div class="hud-item score">
        <span class="score-num">{{ myScore }}</span>
        <span class="score-label">分</span>
      </div>

      <div class="hud-item kills" v-if="gameMode === 'multi'">
        <el-icon><Aim /></el-icon>
        <span>{{ myKills }} 击杀</span>
      </div>

      <div class="hud-item length">
        <span>🐍 {{ myLength }}</span>
      </div>

      <!-- 连接状态 -->
      <div class="hud-item connection">
        <span :class="['status-dot', isConnected ? 'green' : 'red']"></span>
        <span class="status-text">{{ isConnected ? '已连接' : '未连接' }}</span>
      </div>

      <!-- 退出对局 -->
      <el-button @click="handleExitGame" type="danger" size="small" plain class="exit-btn">
        🚪 退出对局
      </el-button>
    </header>

    <!-- ====== 主体区域 ====== -->
    <div class="game-body">
      <div class="canvas-area">
        <GameCanvas
          ref="gameCanvasRef"
          :gameState="gameState"
          :gridSize="20"
          :mapWidth="mapW"
          :mapHeight="mapH"
          :running="isRunning"
        />
      </div>

      <!-- 右侧积分榜（仅多人模式） -->
      <aside class="right-panel" v-if="gameMode === 'multi'">
        <ScoreBoard :players="scoreBoardData" />
      </aside>
    </div>

    <!-- 底部道具栏 -->
    <footer class="game-footer">
      <div class="item-bar">
        <div v-for="item in itemSlots" :key="item.type" :class="['item-slot', { active: item.active }]">
          <span class="item-icon">{{ item.icon }}</span>
          <span class="item-name">{{ item.label }}</span>
          <span class="item-key">{{ item.key }}</span>
        </div>
      </div>
      <div class="control-hints">
        <span>⬆⬇⬅➡/WASD 移动</span>
        <span>空格 加速</span>
        <span>1/2/3 道具</span>
      </div>
    </footer>

    <!-- 左下聊天 -->
    <div class="chat-corner" v-if="gameMode === 'multi'">
      <ChatBox :messages="chatMsgs" @send="onChatSend" />
    </div>

    <!-- 事件弹幕 -->
    <div class="event-toasts">
      <TransitionGroup name="toast">
        <div v-for="evt in activeToasts" :key="evt.id" :class="['event-toast', evt.type]">{{ evt.text }}</div>
      </TransitionGroup>
    </div>

    <!-- 玩家死亡遮罩（多人模式中死亡但游戏未结束） -->
    <div v-if="mySnakeDead && !isGameOver && gameMode === 'multi' && showDeathOverlay" class="death-overlay">
      <div class="death-card">
        <h2>💀 你已被淘汰</h2>
        <p>游戏仍在进行中，你可以观战或退出</p>
        <p class="death-stats">
          <span>🐍 长度 {{ myLength }}</span>
          <span>⚔ {{ myKills }} 击杀</span>
          <span>📊 {{ myScore }} 分</span>
        </p>
        <div class="death-btn-group">
          <el-button type="primary" size="large" @click="exitToLobby">🏠 退出对局</el-button>
          <el-button size="large" @click="dismissDeath">👀 继续观战</el-button>
        </div>
      </div>
    </div>

    <!-- 游戏结束遮罩 -->
    <div v-if="isGameOver" class="game-over-overlay">
      <div class="game-over-card">
        <h2 v-if="gameMode === 'single'">
          {{ mySnakeData?.isAlive ? '🎉 不错哦！' : '💀 游戏结束' }}
        </h2>
        <h2 v-else>🏆 游戏结束</h2>

        <p v-if="gameMode === 'single'">
          {{ mySnakeData?.isAlive ? '你还在继续！' : '你撞到了障碍物！' }}
        </p>
        <p v-else>你的排名：第 {{ myRank }} 名</p>

        <p class="final-score">总分：{{ myScore }} 分</p>
        <p class="final-time">存活时间：{{ formatTime(Math.floor(currentTime)) }}</p>

        <div class="over-btn-group">
          <el-button type="primary" size="large" @click="goToResult">查看结算</el-button>
        </div>
      </div>
    </div>

    <!-- 开始倒计时 -->
    <div v-if="showStartCountdown" class="countdown-overlay">
      <div class="countdown-big">{{ startCount }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock, Trophy, Aim } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useGameStore } from '@/stores/game'
import GameCanvas from '@/components/GameCanvas.vue'
import ScoreBoard from '@/components/ScoreBoard.vue'
import ChatBox from '@/components/ChatBox.vue'
import { useWsStore } from '@/stores/ws'
import { createMockGame } from '@/mock/gameServer'

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const gameStore = useGameStore()
const wsStore = useWsStore()

const gameViewRef = ref(null)
const gameCanvasRef = ref(null)

// ---- Mock 游戏服务器 ----
let mockGameServer = null

// ---- 游戏模式 ----
const gameMode = ref(route.query.mode || 'multi') // 'single' | 'multi'
const roomSeed = loadRoomSeed()
if (roomSeed.gameMode) gameMode.value = roomSeed.gameMode

// ---- 游戏状态 ----
const isRunning = ref(false)
const isGameOver = ref(false)
const gameState = ref({})
const mapW = ref(60)
const mapH = ref(60)
const isConnected = computed(() => USE_MOCK ? true : wsStore.isConnected)

// ---- HUD ----
const mySnakeData = computed(() => {
  if (!gameState.value?.snakes) return null
  for (const s of Object.values(gameState.value.snakes)) {
    if (s.isMe) return s
  }
  return null
})

const myScore = computed(() => mySnakeData.value?.score || 0)
const myKills = computed(() => mySnakeData.value?.kills || 0)
const myLength = computed(() => mySnakeData.value?.length || 0)
const currentTime = computed(() => gameState.value?.gameTime || 0)

// ---- 死亡状态（多人模式中玩家被淘汰但游戏未结束）----
const mySnakeDead = computed(() => {
  if (gameMode.value !== 'multi') return false
  const s = mySnakeData.value
  return s && !s.isAlive
})
const showDeathOverlay = ref(true) // 控制死亡遮罩显示/隐藏

const remainingTime = computed(() => {
  const total = gameState.value?.totalTime || 300
  const current = gameState.value?.gameTime || 0
  return Math.max(0, Math.floor(total - current))
})

const isTimeWarning = computed(() => remainingTime.value <= 30 && remainingTime.value > 0)

const myRank = computed(() => {
  const board = gameState.value?.scoreBoard || []
  const idx = board.findIndex(p => p.isMe)
  return idx >= 0 ? idx + 1 : '?'
})

const scoreBoardData = computed(() => {
  const board = gameState.value?.scoreBoard || []
  return board.map((p, i) => ({ ...p, rank: i + 1 }))
})

// ---- 道具栏 ----
const itemSlots = computed(() => {
  const s = mySnakeData.value
  return [
    { type: 'speed', icon: '⚡', label: '加速', key: '1', active: s?.speedBoost > 0 },
    { type: 'shield', icon: '🛡', label: '护盾', key: '2', active: s?.shield === true },
    { type: 'magnet', icon: '🧲', label: '磁铁', key: '3', active: s?.magnet > 0 }
  ]
})

// ---- 聊天 ----
const chatMsgs = ref([{ type: 'system', text: '🎮 游戏开始！', time: Date.now() }])

function onChatSend(text) {
  wsStore.send('chat_message', {
    roomId: roomSeed.roomId || route.params.roomId,
    playerId: getPlayerId(),
    text
  })
}

// ---- 事件弹幕 ----
const activeToasts = ref([])
let toastId = 0
function addToast(text, type = 'info') {
  const id = ++toastId
  activeToasts.value.push({ id, text, type })
  setTimeout(() => { activeToasts.value = activeToasts.value.filter(t => t.id !== id) }, 3000)
}

// ---- 倒计时 ----
const showStartCountdown = ref(false)
const startCount = ref(3)

function handleGameEvent(type, data) {
  switch (type) {
    case 'game_start':
      chatMsgs.value.push({ type: 'system', text: '🎮 游戏开始！', time: Date.now() })
      break
    case 'player_eliminated':
      addToast(`💀 ${data.nickname} ${data.reason}`, 'elimination')
      chatMsgs.value.push({ type: 'system', text: `💀 ${data.nickname} ${data.reason}淘汰`, time: Date.now() })
      break
    case 'player_kill': {
      const killer = gameState.value?.snakes?.[data.killerId]
      const victim = gameState.value?.snakes?.[data.victimId]
      addToast(`⚔ ${killer?.nickname || '?'} 击杀了 ${victim?.nickname || '?'}`, 'kill')
      break
    }
    case 'item_picked': {
      const snake = gameState.value?.snakes?.[data.snakeId]
      if (snake?.isMe) {
        const labels = { speed: '加速', shield: '护盾', magnet: '磁铁' }
        addToast(`🎁 获得${labels[data.itemType] || '道具'}！`, 'item')
      }
      break
    }
    case 'shield_used':
      if (gameState.value?.snakes?.[data.snakeId]?.isMe) addToast('🛡 护盾抵挡了一次攻击！', 'shield')
      break
    case 'game_over':
      isGameOver.value = true
      isRunning.value = false
      gameStore.resetGame()
      gameStore.updateGameState(data)
      sessionStorage.setItem('game_result', JSON.stringify({
        gameId: data.gameId,
        rankings: data.rankings,
        myRank: data.rankings?.findIndex(r => r.isMe) + 1,
        gameMode: data.gameMode || gameMode.value
      }))
      break
  }
}

// ---- 键盘输入 ----
const keyMap = {
  ArrowUp: 'up', ArrowDown: 'down', ArrowLeft: 'left', ArrowRight: 'right',
  w: 'up', W: 'up', s: 'down', S: 'down', a: 'left', A: 'left', d: 'right', D: 'right'
}
let lastDirection = null

function handleKeyDown(e) {
  const dir = keyMap[e.key]
  if (dir) {
    e.preventDefault()
    if (dir !== lastDirection) {
      lastDirection = dir
      if (USE_MOCK && mockGameServer) {
        mockGameServer.setDirection(dir)
      } else {
        wsStore.send('change_direction', {
          roomId: roomSeed.roomId || route.params.roomId,
          playerId: getPlayerId(),
          direction: dir
        })
      }
    }
    return
  }
  if (e.key === ' ') {
    e.preventDefault()
    if (USE_MOCK && mockGameServer) {
      mockGameServer.useItem('speed')
    } else {
      wsStore.send('speed_boost', { roomId: roomSeed.roomId || route.params.roomId, playerId: getPlayerId() })
    }
    addToast('⚡ 加速！', 'item')
    return
  }
  if (e.key === '1') {
    e.preventDefault()
    if (USE_MOCK && mockGameServer) {
      mockGameServer.useItem('speed')
    } else {
      wsStore.send('use_item', { roomId: roomSeed.roomId || route.params.roomId, playerId: getPlayerId(), itemType: 'speed' })
    }
    addToast('⚡ 加速道具！', 'item')
  }
  if (e.key === '2') {
    e.preventDefault()
    if (USE_MOCK && mockGameServer) {
      mockGameServer.useItem('shield')
    } else {
      wsStore.send('use_item', { roomId: roomSeed.roomId || route.params.roomId, playerId: getPlayerId(), itemType: 'shield' })
    }
    addToast('🛡 护盾！', 'item')
  }
  if (e.key === '3') {
    e.preventDefault()
    if (USE_MOCK && mockGameServer) {
      mockGameServer.useItem('magnet')
    } else {
      wsStore.send('use_item', { roomId: roomSeed.roomId || route.params.roomId, playerId: getPlayerId(), itemType: 'magnet' })
    }
    addToast('🧲 磁铁！', 'item')
  }
}

function formatTime(seconds) {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}:${s.toString().padStart(2, '0')}`
}

function goToResult() {
  leaveRoom()
  router.push(`/result/${sessionStorage.getItem('game_result') ? JSON.parse(sessionStorage.getItem('game_result')).gameId : 'latest'}`)
}

/** 死亡后退出到大厅（多人模式） */
function exitToLobby() {
  leaveRoom()
  isRunning.value = false
  router.push('/lobby')
}

/** 对局中随时退出 */
function handleExitGame() {
  ElMessageBox.confirm('确定要退出当前对局吗？进度将不会保存。', '退出对局', {
    confirmButtonText: '确定退出',
    cancelButtonText: '继续游戏',
    type: 'warning'
  }).then(() => {
    leaveRoom()
    isRunning.value = false
    router.push('/lobby')
  }).catch(() => {})
}

/** 关闭死亡遮罩，继续观战 */
function dismissDeath() {
  showDeathOverlay.value = false
}

const offHandlers = []
const leaving = ref(false)

function leaveRoom() {
  if (leaving.value) return
  leaving.value = true
  wsStore.send('leave_room', {
    roomId: roomSeed.roomId || route.params.roomId,
    playerId: getPlayerId()
  })
}

function registerHandlers() {
  offHandlers.push(wsStore.on('*', (data, raw) => {
    console.log('[GameView] WS message received:', raw?.type, raw?.type === 'game_state' ? `(snakes: ${Object.keys(data?.snakes || {}).length})` : '')
  }))
  offHandlers.push(wsStore.on('game_state', handleGameState))
  offHandlers.push(wsStore.on('game_start', (data) => handleGameEvent('game_start', data)))
  offHandlers.push(wsStore.on('player_eliminated', (data) => handleGameEvent('player_eliminated', data)))
  offHandlers.push(wsStore.on('player_kill', (data) => handleGameEvent('player_kill', data)))
  offHandlers.push(wsStore.on('item_picked', (data) => handleGameEvent('item_picked', data)))
  offHandlers.push(wsStore.on('shield_used', (data) => handleGameEvent('shield_used', data)))
  offHandlers.push(wsStore.on('game_over', (data) => handleGameEvent('game_over', data)))
  offHandlers.push(wsStore.on('countdown', handleCountdown))
  offHandlers.push(wsStore.on('chat_broadcast', handleChatBroadcast))
  offHandlers.push(wsStore.on('error', handleError))
}

function handleGameState(state) {
  console.log('[GameView] game_state received:', state?.gameStatus, state?.gameTime, Object.keys(state?.snakes || {}).length, 'snakes')
  if (!state) return
  gameState.value = state
  mapW.value = state.mapWidth || mapW.value
  mapH.value = state.mapHeight || mapH.value
  if (state.gameStatus === 'playing' || state.gameStatus === 'finished') {
    if (!isRunning.value) console.log('[GameView] Starting game loop')
    isRunning.value = true
  }
}

function handleCountdown(data) {
  const seconds = data?.seconds ?? 0
  startCount.value = seconds
  showStartCountdown.value = seconds > 0
}

function handleChatBroadcast(data) {
  if (!data) return
  if (data.type === 'system') {
    chatMsgs.value.push({ type: 'system', text: data.text, time: data.time || Date.now() })
  } else {
    chatMsgs.value.push({
      type: 'user',
      senderId: data.senderId,
      senderName: data.senderName,
      text: data.text,
      time: data.time || Date.now()
    })
  }
}

function handleError(data) {
  ElMessage.error(data?.message || '服务器错误')
}

function loadRoomSeed() {
  const raw = sessionStorage.getItem('current_room')
  if (!raw) return {}
  try { return JSON.parse(raw) } catch { return {} }
}

function getPlayerId() {
  if (userStore.userInfo.id) return userStore.userInfo.id
  const cached = localStorage.getItem('snake_player_id')
  if (cached) return cached
  const id = 'player_' + Date.now() + '_' + Math.floor(Math.random() * 10000)
  localStorage.setItem('snake_player_id', id)
  return id
}

async function connectAndJoin() {
  if (USE_MOCK) {
    ElMessage.success('使用Mock模式启动游戏')
    startMockGame()
  } else {
    registerHandlers()
    try {
      await wsStore.connect()
    } catch {
      ElMessage.error('无法连接服务器')
      return
    }

    wsStore.send('join_room', {
      roomId: roomSeed.roomId || route.params.roomId,
      roomName: roomSeed.roomName,
      playerId: getPlayerId(),
      nickname: userStore.userInfo.nickname || 'Player',
      avatar: userStore.userInfo.avatar || '',
      level: userStore.userInfo.level || 1,
      gameMode: gameMode.value,
      maxPlayers: roomSeed.maxPlayers || 6,
      gameDuration: roomSeed.gameDuration || 300,
      hasPassword: false,
      password: '',
      create: false,
      allowBots: false
    })
  }
}

function startMockGame() {
  const myId = getPlayerId()
  
  mockGameServer = createMockGame({
    playerCount: gameMode.value === 'single' ? 1 : 4,
    gameDuration: gameMode.value === 'single' ? Infinity : 300,
    gameMode: gameMode.value,
    mySnakeId: myId,
    onStateUpdate: (state) => {
      handleGameState(state)
    },
    onEvent: (eventType, data) => {
      handleGameEvent(eventType, data)
    }
  })

  mockGameServer.initGame()
}

onMounted(() => {
  nextTick(() => {
    gameViewRef.value?.focus()
  })
  connectAndJoin()
})

onUnmounted(() => {
  if (USE_MOCK && mockGameServer) {
    mockGameServer.destroy()
    mockGameServer = null
  } else {
    offHandlers.forEach(off => off())
    if (!leaving.value) {
      leaveRoom()
    }
  }
  isRunning.value = false
})
</script>

<style scoped>
.game-view {
  width: 100%; height: 100vh;
  display: flex; flex-direction: column;
  background: #f1f8e9;
  overflow: hidden; outline: none; position: relative;
}

/* HUD */
.game-hud {
  display: flex; align-items: center; justify-content: center;
  gap: 24px; padding: 8px 20px;
  background: rgba(255,255,255,0.95);
  border-bottom: 1px solid #dcedc8;
  box-shadow: 0 1px 4px rgba(46,59,46,0.06);
  flex-shrink: 0; z-index: 10;
}
.hud-item {
  display: flex; align-items: center; gap: 6px;
  font-size: 14px; color: var(--text-primary); font-weight: 600;
}
.hud-item.timer { font-size: 18px; font-variant-numeric: tabular-nums; }
.hud-item.timer.warning { color: #ef5350; animation: blink-warning 0.5s infinite; }
@keyframes blink-warning { 0%,100%{opacity:1} 50%{opacity:0.3} }
.hud-item.score .score-num { font-size: 20px; color: #43a047; }
.hud-item.score .score-label { font-size: 12px; color: var(--text-secondary); }
.status-dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
.status-dot.green { background: #66bb6a; }
.status-dot.red { background: #ef5350; }
.status-text { font-size: 11px; color: var(--text-muted); }
.exit-btn { margin-left: auto; font-size: 12px; }

/* 主体 */
.game-body { flex: 1; display: flex; overflow: hidden; }
.canvas-area { flex: 1; overflow: hidden; background: #e8f5e9; }
.right-panel {
  width: 220px; flex-shrink: 0;
  border-left: 1px solid #dcedc8;
  overflow-y: auto; background: #fafdf7;
}

/* 底部 */
.game-footer {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 20px;
  background: rgba(255,255,255,0.95);
  border-top: 1px solid #dcedc8;
  flex-shrink: 0; z-index: 10;
}
.item-bar { display: flex; gap: 10px; }
.item-slot {
  display: flex; align-items: center; gap: 6px;
  padding: 5px 14px; border-radius: 8px;
  background: #f5f9f0; border: 1px solid #dcedc8;
  font-size: 13px; opacity: 0.5; transition: all 0.3s;
}
.item-slot.active { opacity: 1; border-color: #66bb6a; background: #e8f5e9; }
.item-key {
  font-size: 10px; color: var(--text-muted);
  background: #dcedc8; padding: 1px 5px; border-radius: 3px;
}
.control-hints { display: flex; gap: 16px; font-size: 11px; color: var(--text-muted); }

/* 聊天 */
.chat-corner { position: absolute; bottom: 56px; left: 12px; z-index: 20; }

/* 弹幕 */
.event-toasts {
  position: absolute; top: 60px; left: 50%; transform: translateX(-50%);
  z-index: 30; display: flex; flex-direction: column; align-items: center;
  gap: 6px; pointer-events: none;
}
.event-toast {
  padding: 4px 16px; border-radius: 12px;
  font-size: 13px; font-weight: 600; white-space: nowrap;
}
.event-toast.elimination { background: #ffebee; color: #ef5350; border: 1px solid #ffcdd2; }
.event-toast.kill { background: #fff8e1; color: #f9a825; border: 1px solid #ffecb3; }
.event-toast.item { background: #e3f2fd; color: #42a5f5; border: 1px solid #bbdefb; }
.event-toast.shield { background: #f3e5f5; color: #ab47bc; border: 1px solid #e1bee7; }

.toast-enter-active { animation: slideInDown 0.3s ease-out; }
.toast-leave-active { animation: slideOutUp 0.3s ease-in; }
@keyframes slideInDown { from{transform:translateY(-20px);opacity:0} to{transform:translateY(0);opacity:1} }
@keyframes slideOutUp { from{transform:translateY(0);opacity:1} to{transform:translateY(-20px);opacity:0} }

/* 死亡退出遮罩 */
.death-overlay {
  position: absolute; inset: 0; z-index: 55;
  display: flex; align-items: center; justify-content: center;
  background: rgba(0,0,0,0.5);
}
.death-card {
  text-align: center; padding: 36px 50px;
  background: #fff; border: 2px solid #ef5350;
  border-radius: 18px; box-shadow: 0 8px 40px rgba(0,0,0,0.2);
}
.death-card h2 { color: #ef5350; font-size: 26px; margin-bottom: 8px; }
.death-card p { color: var(--text-primary); font-size: 14px; margin: 6px 0; }
.death-stats { display: flex; gap: 20px; justify-content: center; font-size: 13px !important; color: var(--text-secondary) !important; }
.death-btn-group { margin-top: 20px; display: flex; gap: 12px; justify-content: center; }

/* 游戏结束 */
.game-over-overlay {
  position: absolute; inset: 0; z-index: 50;
  display: flex; align-items: center; justify-content: center;
  background: rgba(241,248,233,0.85);
  backdrop-filter: blur(4px);
}
.game-over-card {
  text-align: center; padding: 40px 60px;
  background: #fff; border: 2px solid #a5d6a7;
  border-radius: 18px; box-shadow: 0 8px 40px rgba(46,59,46,0.1);
}
.game-over-card h2 { color: var(--primary-dark); font-size: 28px; margin-bottom: 12px; }
.game-over-card p { color: var(--text-primary); font-size: 16px; margin: 6px 0; }
.final-score { font-size: 32px !important; color: #43a047 !important; font-weight: 900; }
.final-time { font-size: 14px !important; color: var(--text-muted) !important; }
.over-btn-group { margin-top: 20px; }

/* 开始倒计时 */
.countdown-overlay {
  position: absolute; inset: 0; z-index: 100;
  display: flex; align-items: center; justify-content: center;
  background: rgba(255,255,255,0.7); pointer-events: none;
}
.countdown-big {
  font-size: 140px; font-weight: 900;
  color: #43a047;
  text-shadow: 0 0 40px rgba(67,160,71,0.3);
  animation: countPop 0.6s ease-out;
}
@keyframes countPop {
  0% { transform: scale(2.5); opacity: 0; }
  60% { transform: scale(0.9); opacity: 1; }
  100% { transform: scale(1); }
}
</style>
