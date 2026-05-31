<!--
  RoomView.vue - 游戏房间页面（v2 - 小清新主题）
  功能：玩家列表、聊天、准备状态、房主控制、倒计时
  修复：isHost 检测逻辑、AI玩家全部自动准备、游戏模式传递
-->
<template>
  <div class="room-container">
    <!-- 顶部装饰条 -->
    <div class="top-decor"></div>

    <!-- 连接错误提示 -->
    <div v-if="connectionError" class="connection-banner">
      <span>⚠️ {{ connectionError }}</span>
      <el-button size="small" @click="retryConnect" :loading="isRetrying">重新连接</el-button>
    </div>

    <!-- 顶部信息栏 -->
    <header class="room-header">
      <el-button @click="handleLeaveRoom" text :icon="ArrowLeft" class="back-btn">返回大厅</el-button>
      <div class="room-title-area">
        <h3>
          🏠 {{ roomInfo.name }}
          <el-icon v-if="roomInfo.hasPassword"><Lock /></el-icon>
        </h3>
        <div class="room-meta">
          <span>👥 {{ players.length }}/{{ roomInfo.maxPlayers }}</span>
          <span>⏱ {{ formatDuration(roomInfo.gameDuration) }}</span>
          <span>🎮 {{ roomInfo.gameMode === 'single' ? '单人无尽' : '多人对战' }}</span>
          <span>🆔 {{ roomInfo.id }}</span>
        </div>
      </div>
      <div class="header-actions">
        <el-tooltip content="复制房间号">
          <el-button @click="copyRoomId" :icon="CopyDocument" circle size="small" class="icon-btn" />
        </el-tooltip>
      </div>
    </header>

    <!-- 主体双栏布局 -->
    <div class="room-body">
      <!-- ====== 左侧：玩家列表 ====== -->
      <div class="room-left">
        <!-- 倒计时覆盖层 -->
        <div v-if="countdown > 0" class="countdown-overlay">
          <div class="countdown-number">{{ countdown }}</div>
        </div>

        <h4 class="section-label">🌿 玩家列表</h4>

        <div class="player-list">
          <div
            v-for="(player, index) in players" :key="player.id"
            :class="[
              'player-item',
              { 'is-host': player.isHost, 'is-me': player.id === myId, 'is-ready': player.isReady }
            ]"
          >
            <span class="player-index">{{ index + 1 }}</span>
            <el-avatar :size="40" :icon="UserFilled" class="player-avatar">
              {{ player.nickname?.charAt(0) }}
            </el-avatar>
            <div class="player-info">
              <span class="player-name">
                {{ player.nickname }}
                <el-tag v-if="player.isHost" size="small" type="warning" effect="light">👑房主</el-tag>
                <el-tag v-if="player.id === myId" size="small" type="success" effect="light">我</el-tag>
              </span>
              <span class="player-status">
                <span v-if="player.isReady" class="ready-badge">✅ 已准备</span>
                <span v-else class="unready-badge">⏳ 未准备</span>
                <span class="player-level">Lv.{{ player.level }}</span>
              </span>
            </div>
            <!-- 房主操作：踢人 + 切换准备状态 -->
            <div v-if="isHost && player.id !== myId" class="host-player-ops">
              <el-button
                @click.stop="togglePlayerReady(player)"
                :type="player.isReady ? 'warning' : 'success'"
                circle
                size="small"
                text
                :title="player.isReady ? '取消准备' : '设为准备'"
              >
                {{ player.isReady ? '❌' : '✅' }}
              </el-button>
              <el-button
                @click.stop="handleKickPlayer(player)"
                type="danger"
                :icon="Delete"
                circle
                size="small"
                text
                title="踢出玩家"
              />
            </div>
          </div>

          <!-- 空席位 -->
          <div v-for="i in emptySlots" :key="'empty-' + i" class="player-item empty-slot">
            <span class="player-index">{{ players.length + i }}</span>
            <el-avatar :size="40" :icon="UserFilled" class="player-avatar empty-avatar" />
            <div class="player-info">
              <span class="player-name empty-text">虚位以待...</span>
            </div>
          </div>
        </div>

        <!-- 底部操作栏 -->
        <div class="bottom-actions">
          <!-- ===== 加入的房间：准备按钮 + 开始按钮 ===== -->
          <template v-if="!isHost">
            <el-button
              :type="myReadyState ? 'warning' : 'success'"
              size="large"
              @click="toggleReady"
              class="action-btn"
              block
            >
              {{ myReadyState ? '取消准备' : '✅ 准备就绪' }}
            </el-button>
            <!-- 全员准备后显示开始按钮 -->
            <el-button
              type="primary"
              size="large"
              @click="handleJoinedStart"
              :disabled="!isJoinedRoomAllReady"
              class="action-btn start-btn"
              style="margin-top: 8px;"
              block
            >
              {{ isJoinedRoomAllReady ? '🎮 开始游戏' : `等待全员准备 (${readyCount}/${players.length})` }}
            </el-button>
            <p v-if="!isJoinedRoomAllReady" class="host-hint">
              {{ `还有 ${players.length - readyCount} 人未准备` }}
            </p>
          </template>

          <!-- ===== 房主：开始按钮 ===== -->
          <div v-if="isHost" class="host-actions">
            <el-button
              type="primary"
              size="large"
              @click="handleStartGame"
              :disabled="!canStart"
              class="action-btn start-btn"
            >
              🎮 开始游戏
            </el-button>
            <p v-if="!canStart && !isSingleMode" class="host-hint">
              {{ `需要全员准备 (${readyCount}/${players.length})` }}
            </p>
          </div>
        </div>
      </div>

      <!-- ====== 右侧：聊天区 ====== -->
      <div class="room-right">
        <h4 class="section-label">💬 房间聊天</h4>

        <div class="chat-messages" ref="chatListRef" @scroll="onChatScroll">
          <div v-for="(msg, i) in chatMessages" :key="i" :class="['chat-msg', msg.type]">
            <template v-if="msg.type === 'system'">
              <span class="system-msg">{{ msg.text }}</span>
            </template>
            <template v-else>
              <span class="msg-sender" :class="{ 'is-me': msg.senderId === myId }">{{ msg.senderName }}</span>
              <span class="msg-colon">:</span>
              <span class="msg-text">{{ msg.text }}</span>
            </template>
          </div>
          <div v-if="showNewMsgHint" class="new-msg-hint" @click="scrollToBottom">↓ 新消息</div>
        </div>

        <div class="quick-emoji">
          <span v-for="emoji in quickEmojis" :key="emoji" @click="sendEmoji(emoji)" class="emoji-item">{{ emoji }}</span>
        </div>

        <div class="chat-input-area">
          <el-input
            v-model="chatInput"
            placeholder="输入消息，回车发送..."
            @keyup.enter="sendMessage"
            size="default"
            clearable
          >
            <template #append>
              <el-button @click="sendMessage" :icon="Promotion" />
            </template>
          </el-input>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 房间页面逻辑 - v2
 */
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Lock, CopyDocument, Delete, UserFilled, Promotion } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useWsStore } from '@/stores/ws'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const wsStore = useWsStore()

// ---- 基础信息 ----
const myId = ref(getPlayerId())
const isHost = ref(false)
const myReadyState = ref(false)
const countdown = ref(0)
const navigatingToGame = ref(false)
const connectionError = ref('')
const isRetrying = ref(false)

// ---- 房间信息 ----
const roomInfo = ref({
  id: route.params.roomId || 'room',
  name: '房间',
  maxPlayers: 6,
  gameDuration: 300,
  hasPassword: false,
  password: '',
  gameMode: 'multi' // 'single' | 'multi'
})

const seed = loadRoomSeed()
roomInfo.value = { ...roomInfo.value, ...seed.roomInfo }
const createdByMe = ref(seed.createdByMe)

// ---- 玩家列表 ----
const players = ref([])

// ---- 计算属性 ----
const emptySlots = computed(() =>
  Math.max(0, roomInfo.value.maxPlayers - players.value.length)
)

const readyCount = computed(() =>
  players.value.filter(p => p.isReady).length
)

const isSingleMode = computed(() => roomInfo.value.gameMode === 'single')

/** 单人模式：房主即可开始
 *  多人模式（自己创建）：全员准备即可开始
 *  多人模式（大厅加入）：房间满员+全员准备才可开始 */
const canStart = computed(() => {
  if (!isHost.value) return false
  if (isSingleMode.value) return true
  return players.value.length >= 2 && players.value.every(p => p.isReady)
})

/** 是否所有玩家都已准备（大厅加入的房间用这个判断）
 *  单人模式只需 1 人准备即可，多人模式需全员（>=2人）准备 */
const isJoinedRoomAllReady = computed(() => {
  if (isHost.value) return false
  if (isSingleMode.value) return players.value.length >= 1 && players.value.every(p => p.isReady)
  return players.value.length >= 2 && players.value.every(p => p.isReady)
})

const offHandlers = []

// ---- 聊天 ----
const chatInput = ref('')
const chatListRef = ref(null)
const showNewMsgHint = ref(false)
const chatMessages = ref([])
const quickEmojis = ['👍', '💪', '😄', '🎉', '😢', '👋', '⏰', '🏆']

function sendMessage() {
  const text = chatInput.value.trim()
  if (!text) return
  wsStore.send('chat_message', {
    roomId: roomInfo.value.id,
    playerId: myId.value,
    text
  })
  chatInput.value = ''
}
function sendEmoji(emoji) {
  wsStore.send('chat_message', {
    roomId: roomInfo.value.id,
    playerId: myId.value,
    text: emoji
  })
}
function scrollToBottom() {
  nextTick(() => {
    const el = chatListRef.value
    if (el) { el.scrollTop = el.scrollHeight; showNewMsgHint.value = false }
  })
}
function onChatScroll() {
  const el = chatListRef.value
  if (!el) return
  showNewMsgHint.value = el.scrollHeight - el.scrollTop - el.clientHeight > 40
}

// ---- 操作 ----
function toggleReady() {
  if (!wsStore.isConnected) {
    ElMessage.error('未连接到服务器，请检查后端是否运行')
    return
  }
  // 乐观更新：先切 UI，失败再回滚
  const previous = myReadyState.value
  myReadyState.value = !myReadyState.value
  const sent = wsStore.send('ready', {
    roomId: roomInfo.value.id,
    playerId: myId.value,
    ready: myReadyState.value
  })
  if (!sent) {
    myReadyState.value = previous
    ElMessage.error('发送失败，请检查网络连接')
  }
}

/** 房主切换其他玩家的准备状态 */
function togglePlayerReady(player) {
  if (!wsStore.isConnected) {
    ElMessage.error('未连接到服务器，请检查后端是否运行')
    return
  }
  const sent = wsStore.send('ready', {
    roomId: roomInfo.value.id,
    playerId: myId.value,
    targetPlayerId: player.id,
    ready: !player.isReady
  })
  if (!sent) {
    ElMessage.error('发送失败，请检查网络连接')
  }
}

function handleKickPlayer(player) {
  ElMessageBox.confirm(`确定要踢出 ${player.nickname} 吗？`, '踢出玩家', {
    confirmButtonText: '确定', type: 'warning'
  }).then(() => {
    wsStore.send('kick_player', {
      roomId: roomInfo.value.id,
      playerId: myId.value,
      targetPlayerId: player.id
    })
  }).catch(() => {})
}

/** 房主开始游戏 */
function handleStartGame() {
  if (!canStart.value) return
  wsStore.send('start_game', {
    roomId: roomInfo.value.id,
    playerId: myId.value
  })
}

/** 加入房间的玩家开始游戏 */
function handleJoinedStart() {
  if (!isJoinedRoomAllReady.value) {
    ElMessage.warning(`还有 ${players.value.length - readyCount.value} 人未准备`)
    return
  }
  if (!isHost.value) {
    ElMessage.info('等待房主开始游戏')
    return
  }
  handleStartGame()
}

function handleLeaveRoom() {
  if (countdown.value > 0) { ElMessage.warning('游戏即将开始，无法退出'); return }
  ElMessageBox.confirm('确定要退出房间吗？', '退出房间', {
    confirmButtonText: '确定', type: 'warning'
  }).then(() => {
    wsStore.send('leave_room', { roomId: roomInfo.value.id, playerId: myId.value })
    router.push('/lobby')
  }).catch(() => {})
}

function copyRoomId() {
  navigator.clipboard.writeText(roomInfo.value.id).then(() => {
    ElMessage.success('房间号已复制！')
  }).catch(() => ElMessage.info('房间号：' + roomInfo.value.id))
}
function formatDuration(s) { const m = Math.floor(s / 60); return m + '分钟' }

async function retryConnect() {
  isRetrying.value = true
  await connectAndJoin()
  isRetrying.value = false
}

function registerHandlers() {
  offHandlers.push(wsStore.on('room_update', handleRoomUpdate))
  offHandlers.push(wsStore.on('countdown', handleCountdown))
  offHandlers.push(wsStore.on('game_start', handleGameStart))
  offHandlers.push(wsStore.on('chat_broadcast', handleChatBroadcast))
  offHandlers.push(wsStore.on('error', handleError))
}

async function connectAndJoin() {
  registerHandlers()
  connectionError.value = ''
  try {
    await wsStore.connect()
    connectionError.value = ''
  } catch {
    connectionError.value = '无法连接游戏服务器，请确认后端已启动（端口 8080）'
    return
  }

  wsStore.send('join_room', {
    roomId: roomInfo.value.id,
    roomName: roomInfo.value.name,
    playerId: myId.value,
    nickname: userStore.userInfo.nickname || 'Player',
    avatar: userStore.userInfo.avatar || '',
    level: userStore.userInfo.level || 1,
    gameMode: roomInfo.value.gameMode,
    maxPlayers: roomInfo.value.maxPlayers,
    gameDuration: roomInfo.value.gameDuration,
    hasPassword: roomInfo.value.hasPassword,
    password: roomInfo.value.password || '',
    create: createdByMe.value,
    allowBots: false
  })
}

function handleRoomUpdate(data) {
  if (!data) return
  roomInfo.value = { ...roomInfo.value, ...data.roomInfo, id: data.roomId || roomInfo.value.id }
  players.value = data.players || []
  isHost.value = roomInfo.value.hostId === myId.value
  const me = players.value.find(p => p.id === myId.value)
  myReadyState.value = me?.isReady || false
}

function handleCountdown(data) {
  countdown.value = data?.seconds ?? 0
}

function handleGameStart(data) {
  navigatingToGame.value = true
  sessionStorage.setItem('current_room', JSON.stringify({
    roomId: roomInfo.value.id,
    gameMode: roomInfo.value.gameMode,
    gameDuration: roomInfo.value.gameDuration,
    maxPlayers: roomInfo.value.maxPlayers
  }))
  const mode = roomInfo.value.gameMode === 'single' ? '?mode=single' : '?mode=multi'
  router.push(`/game/${roomInfo.value.id}${mode}`)
}

function handleChatBroadcast(data) {
  if (!data) return
  if (data.type === 'system') {
    chatMessages.value.push({ type: 'system', text: data.text, time: data.time || Date.now() })
  } else {
    chatMessages.value.push({
      type: 'user',
      senderId: data.senderId,
      senderName: data.senderName,
      text: data.text,
      time: data.time || Date.now()
    })
  }
  scrollToBottom()
}

function handleError(data) {
  ElMessage.error(data?.message || '服务器错误')
}

function loadRoomSeed() {
  const savedConfigRaw = sessionStorage.getItem('new_room_config')
  const joinedRoomRaw = sessionStorage.getItem('joined_room_data')

  let savedConfig = null
  let joinedRoom = null
  if (savedConfigRaw) { try { savedConfig = JSON.parse(savedConfigRaw) } catch {} }
  if (joinedRoomRaw) { try { joinedRoom = JSON.parse(joinedRoomRaw) } catch {} }

  const isSelfCreated = !!savedConfig
  const maxPlayers = savedConfig?.maxPlayers || joinedRoom?.maxPlayers || 6
  const gameMode = savedConfig?.gameMode || joinedRoom?.gameMode || 'multi'

  const info = {
    id: savedConfig?.roomId || joinedRoom?.id || route.params.roomId || 'room_' + Date.now(),
    name: savedConfig?.name || joinedRoom?.name || '房间',
    maxPlayers: gameMode === 'single' ? 1 : maxPlayers,
    gameDuration: savedConfig?.gameDuration || joinedRoom?.gameDuration || 300,
    hasPassword: savedConfig?.hasPassword || joinedRoom?.hasPassword || false,
    password: savedConfig?.password || joinedRoom?.password || '',
    gameMode
  }

  sessionStorage.removeItem('new_room_config')
  sessionStorage.removeItem('joined_room_data')

  return { roomInfo: info, createdByMe: isSelfCreated }
}

function getPlayerId() {
  if (userStore.userInfo.id) return userStore.userInfo.id
  const cached = localStorage.getItem('snake_player_id')
  if (cached) return cached
  const id = 'player_' + Date.now() + '_' + Math.floor(Math.random() * 10000)
  localStorage.setItem('snake_player_id', id)
  return id
}

onMounted(() => {
  connectAndJoin()
})

onUnmounted(() => {
  offHandlers.forEach(off => off())
  if (!navigatingToGame.value) {
    wsStore.send('leave_room', { roomId: roomInfo.value.id, playerId: myId.value })
  }
})
</script>

<style scoped>
/* ===== 整体布局 ===== */
.room-container {
  width: 100%; height: 100vh;
  display: flex; flex-direction: column;
  background: var(--bg-main);
  overflow: hidden;
}

/* 顶部装饰条 */
.top-decor {
  height: 3px;
  background: linear-gradient(90deg, #a5d6a7, #66bb6a, #43a047, #66bb6a, #a5d6a7);
  flex-shrink: 0;
}

/* 连接错误横幅 */
.connection-banner {
  display: flex; align-items: center; justify-content: center; gap: 12px;
  padding: 10px 20px;
  background: #fff3e0; color: #e65100;
  font-size: 14px; font-weight: 600;
  border-bottom: 2px solid #ff9800;
  flex-shrink: 0;
  animation: shake 0.5s ease-in-out;
}
@keyframes shake {
  0%,100% { transform: translateX(0); }
  25% { transform: translateX(-4px); }
  75% { transform: translateX(4px); }
}

.room-header {
  display: flex; align-items: center;
  padding: 12px 20px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-color);
  gap: 16px;
  box-shadow: var(--shadow-sm);
  flex-shrink: 0;
}
.back-btn { color: var(--primary-dark); }
.room-title-area { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.room-title-area h3 {
  margin: 0; font-size: 18px; color: var(--primary-dark); font-weight: 700;
  display: flex; align-items: center; gap: 6px;
}
.room-meta {
  display: flex; gap: 14px; font-size: 12px; color: var(--text-secondary);
}
.header-actions { margin-left: auto; }
.icon-btn { border: 1px solid var(--border-color); }

/* ===== 主体双栏 ===== */
.room-body { flex: 1; display: flex; overflow: hidden; }

/* ===== 左侧 ===== */
.room-left {
  width: 340px; flex-shrink: 0;
  display: flex; flex-direction: column;
  border-right: 1px solid var(--border-color);
  background: var(--bg-card);
  position: relative;
}
.section-label {
  padding: 14px 16px 8px;
  font-size: 13px; color: var(--text-secondary); letter-spacing: 1px;
}

/* 倒计时覆盖 */
.countdown-overlay {
  position: absolute; inset: 0; z-index: 100;
  background: rgba(255,255,255,0.85);
  display: flex; align-items: center; justify-content: center;
}
.countdown-number {
  font-size: 80px; font-weight: 900;
  color: var(--primary-dark);
  text-shadow: 0 0 20px rgba(67,160,71,0.3);
  animation: countPop 0.8s ease-out;
}
@keyframes countPop {
  0% { transform: scale(2.5); opacity: 0; }
  60% { transform: scale(0.9); opacity: 1; }
  100% { transform: scale(1); }
}

/* 玩家列表 */
.player-list {
  flex: 1; overflow-y: auto; padding: 0 14px;
}
.player-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px; border-radius: 10px; margin-bottom: 6px;
  background: var(--bg-main); transition: all 0.2s;
  border: 1px solid transparent;
}
.player-item:hover { border-color: var(--border-color); }
.player-item.is-host { border-left: 3px solid #ffa726; }
.player-item.is-me { background: #e8f5e9; border-color: #a5d6a7; }
.player-item.is-ready { background: #f1f8e9; }
.player-item.empty-slot {
  opacity: 0.45; background: #fafdf7;
}
.player-index {
  width: 24px; text-align: center;
  font-size: 12px; color: var(--text-muted); font-weight: 700;
}
.player-avatar { border: 2px solid var(--border-color); flex-shrink: 0; }
.empty-avatar { opacity: 0.5; }
.player-info { flex: 1; display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.player-name {
  font-size: 14px; color: var(--text-primary); font-weight: 600;
  display: flex; align-items: center; gap: 6px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.player-status { font-size: 12px; display: flex; align-items: center; gap: 10px; }
.ready-badge { color: var(--success-color); }
.unready-badge { color: var(--warning-color); }
.player-level { color: var(--text-secondary); }
.empty-text { color: var(--text-muted); }
.host-player-ops {
  display: flex; gap: 2px; flex-shrink: 0;
}

/* 操作栏 */
.bottom-actions {
  padding: 16px; border-top: 1px solid var(--border-color);
  background: var(--bg-card);
}
.action-btn { width: 100%; height: 46px; font-size: 16px; border-radius: 10px; }
.host-actions { text-align: center; }
.start-btn { width: 100%; height: 46px; font-size: 16px; }
.host-hint {
  font-size: 12px; color: var(--text-muted);
  margin-top: 8px; margin-bottom: 0;
}

/* ===== 右侧聊天 ===== */
.room-right {
  flex: 1; display: flex; flex-direction: column;
  background: var(--bg-sidebar);
}
.chat-messages {
  flex: 1; overflow-y: auto; padding: 8px 16px; position: relative;
}
.chat-msg { padding: 4px 0; font-size: 13px; line-height: 1.6; word-break: break-all; }
.chat-msg.system { text-align: center; }
.system-msg {
  display: inline-block;
  background: #e8f5e9; color: var(--primary-dark);
  font-size: 12px; padding: 2px 10px; border-radius: 10px;
}
.msg-sender { color: var(--primary-dark); font-weight: 700; }
.msg-sender.is-me { color: #2e7d32; }
.msg-colon { color: var(--text-muted); margin: 0 2px; }
.msg-text { color: var(--text-primary); }
.new-msg-hint {
  position: sticky; bottom: 0; text-align: center;
  background: linear-gradient(to bottom, transparent, var(--bg-sidebar));
  color: var(--primary-color); font-size: 12px; padding: 8px 0; cursor: pointer;
}
.quick-emoji { display: flex; gap: 4px; padding: 6px 16px; border-top: 1px solid var(--border-light); }
.emoji-item { font-size: 18px; cursor: pointer; padding: 3px 5px; border-radius: 4px; transition: transform 0.15s; }
.emoji-item:hover { transform: scale(1.3); background: var(--bg-hover); }
.chat-input-area { padding: 6px 16px 14px; }
.chat-input-area :deep(.el-input__wrapper) {
  background: #fff !important;
  border-color: var(--border-color) !important;
}
</style>
