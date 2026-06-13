<!--
  LobbyView.vue - 游戏大厅页面（v2 - 小清新绿色主题 + 游戏模式选择）
  新增：单人无尽模式 / 多人对战模式选择
-->
<template>
  <div class="lobby-container">
    <!-- 顶部装饰条 -->
    <div class="top-decor"></div>

    <header class="lobby-header">
      <div class="header-left">
        <span class="logo">🐍 多人联机贪吃蛇</span>
        <span class="online-badge">
          <span class="dot"></span> {{ onlineCount }} 人在线
        </span>
      </div>
      <div class="header-right">
        <el-dropdown trigger="click">
          <span class="user-dropdown">
            <el-avatar :size="34" :icon="UserFilled" />
            <span class="username">{{ userStore.userInfo.nickname || userStore.userInfo.username }}</span>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled><span>🏆 积分：{{ userStore.userInfo.score }}</span></el-dropdown-item>
              <el-dropdown-item disabled><span>⭐ 等级：Lv.{{ userStore.userInfo.level }}</span></el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <div class="lobby-body">
      <!-- 左侧操作面板 -->
      <aside class="lobby-sidebar">
        <!-- 快速匹配 -->
        <div class="sidebar-card quick-match" @click="handleQuickMatch">
          <span class="match-icon">⚡</span>
          <div class="match-info">
            <span class="match-title">快速匹配</span>
            <span class="match-desc">自动匹配对手</span>
          </div>
        </div>
        <!-- 创建房间 -->
        <div class="sidebar-card create-room" @click="showCreateDialog = true">
          <span class="match-icon">🏠</span>
          <div class="match-info">
            <span class="match-title">创建房间</span>
            <span class="match-desc">自定义设置</span>
          </div>
        </div>

        <div class="sidebar-section">
          <h4 class="section-title">快捷入口</h4>
          <div class="quick-links">
            <div class="quick-link" @click="handleLink('history')"><el-icon><Clock /></el-icon> 对战记录</div>
            <div class="quick-link" @click="handleLink('rank')"><el-icon><Trophy /></el-icon> 排行榜</div>
            <div class="quick-link" @click="handleLink('guide')"><el-icon><QuestionFilled /></el-icon> 新手引导</div>
          </div>
        </div>

        <div class="sidebar-section" v-if="recentRooms.length">
          <h4 class="section-title">最近加入</h4>
          <div class="recent-list">
            <div v-for="room in recentRooms" :key="room.id" class="recent-item" @click="handleJoinRoom(room)">
              <span>{{ room.name }}</span>
              <el-tag size="small" :type="room.status === 'waiting' ? 'success' : 'info'">
                {{ room.status === 'waiting' ? '等待中' : '已结束' }}
              </el-tag>
            </div>
          </div>
        </div>
      </aside>

      <!-- 右侧房间列表 -->
      <main class="lobby-main">
        <!-- 筛选 -->
        <div class="filter-bar">
          <el-input v-model="searchKeyword" placeholder="搜索房间名..." :prefix-icon="Search" clearable class="search-input" />
          <el-select v-model="statusFilter" placeholder="状态" clearable>
            <el-option label="全部" value="all" />
            <el-option label="等待中" value="waiting" />
            <el-option label="游戏中" value="playing" />
            <el-option label="已满" value="full" />
          </el-select>
          <el-select v-model="modeFilter" placeholder="模式" clearable>
            <el-option label="全部" value="all" />
            <el-option label="多人对战" value="multi" />
            <el-option label="单人无尽" value="single" />
          </el-select>
          <el-switch v-model="hidePassword" active-text="隐藏密码房" />
          <el-button @click="refreshRoomList" :icon="RefreshRight" circle />
        </div>

        <!-- 房间网格 -->
        <div class="room-grid" v-loading="isLoading">
          <template v-if="filteredRooms.length">
            <div
              v-for="room in filteredRooms" :key="room.id"
              :class="['room-card', { 'is-full': room.playerCount >= room.maxPlayers || room.status === 'full' }]"
              @click="room.playerCount >= room.maxPlayers || room.status === 'full' ? null : handleJoinRoom(room)"
            >
              <div class="room-card-header">
                <span class="room-name">
                  {{ room.name }}
                  <el-icon v-if="room.hasPassword"><Lock /></el-icon>
                </span>
                <div class="room-tags">
                  <el-tag :type="room.gameMode === 'single' ? 'success' : 'primary'" size="small" effect="plain">
                    {{ room.gameMode === 'single' ? '🧘 单人无尽' : '👥 多人对战' }}
                  </el-tag>
                  <el-tag :type="getStatusType(room.status)" size="small" effect="dark">
                    {{ getStatusText(room.status) }}
                  </el-tag>
                </div>
              </div>
              <div class="room-card-body">
                <div class="room-stat">
                  <span>👥 {{ room.playerCount }}/{{ room.maxPlayers }}</span>
                  <span>⏱ {{ formatDuration(room.gameDuration) }}</span>
                </div>
                <div class="room-players">
                  <el-avatar v-for="i in Math.min(room.playerCount, 5)" :key="i" :size="26" :icon="UserFilled" class="player-avatar-mini" />
                  <span v-if="room.playerCount > 5" class="more-players">+{{ room.playerCount - 5 }}</span>
                </div>
              </div>
              <el-progress :percentage="(room.playerCount / room.maxPlayers)*100" :stroke-width="3" :show-text="false"
                :color="getProgressColor(room)" />
            </div>
          </template>
          <el-empty v-else description="暂无房间，快去创建一个吧！" :image-size="100">
            <el-button type="primary" @click="showCreateDialog = true">创建房间</el-button>
          </el-empty>
        </div>
      </main>
    </div>

    <!-- 创建房间对话框 -->
    <el-dialog v-model="showCreateDialog" title="🏠 创建房间" width="480px" :close-on-click-modal="false" center>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="90px" label-position="left">
        <!-- 游戏模式 -->
        <el-form-item label="游戏模式" prop="gameMode">
          <el-radio-group v-model="createForm.gameMode">
            <el-radio-button value="multi">
              👥 多人对战
              <div class="mode-desc">撞到其他蛇淘汰，撞墙不会淘汰</div>
            </el-radio-button>
            <el-radio-button value="single">
              🧘 单人无尽
              <div class="mode-desc">躲避障碍物，挑战最高分</div>
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="房间名称" prop="name">
          <el-input v-model="createForm.name" placeholder="请输入房间名称" maxlength="20" show-word-limit />
        </el-form-item>
        <el-form-item label="最大人数" prop="maxPlayers" v-if="createForm.gameMode === 'multi'">
          <el-slider v-model="createForm.maxPlayers" :min="2" :max="8" :marks="playerMarks" show-stops />
        </el-form-item>
        <el-form-item label="游戏时长" prop="gameDuration" v-if="createForm.gameMode === 'multi'">
          <el-select v-model="createForm.gameDuration" style="width:100%">
            <el-option label="3 分钟（快速）" :value="180" />
            <el-option label="5 分钟（标准）" :value="300" />
            <el-option label="8 分钟（持久）" :value="480" />
          </el-select>
        </el-form-item>
        <el-form-item label="房间密码" prop="password">
          <el-switch v-model="createForm.hasPassword" />
          <el-input v-if="createForm.hasPassword" v-model="createForm.password" placeholder="设置密码" style="width:160px;margin-left:10px" maxlength="10" />
        </el-form-item>
        <el-form-item label="AI陪练" v-if="createForm.gameMode === 'multi'">
          <el-switch v-model="createForm.allowBots" />
          <span class="bot-hint">开启后自动补充AI机器人至满员</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateRoom" :loading="isCreating">创建房间</el-button>
      </template>
    </el-dialog>

    <!-- 密码房 -->
    <el-dialog v-model="showPasswordDialog" title="🔒 需要密码" width="360px" center>
      <el-input v-model="joinPassword" placeholder="请输入房间密码" type="password" show-password @keyup.enter="confirmJoinWithPassword" />
      <template #footer>
        <el-button @click="showPasswordDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmJoinWithPassword">加入</el-button>
      </template>
    </el-dialog>

    <!-- 快速匹配对话框 -->
    <el-dialog v-model="showMatchingDialog" title="⚡ 快速匹配" width="400px" :close-on-click-modal="false" :show-close="false" center>
      <div class="matching-content">
        <div class="matching-animation">
          <div class="matching-pulse">
            <span class="pulse-icon">⚡</span>
          </div>
          <div class="matching-rings">
            <span class="ring r1"></span>
            <span class="ring r2"></span>
            <span class="ring r3"></span>
          </div>
        </div>
        <p class="matching-status">{{ matchingMessage }}</p>
        <p class="matching-hint">正在为您寻找旗鼓相当的对手...</p>
        <div class="matching-dots">
          <span class="mdot" v-for="i in 3" :key="i" :style="{ animationDelay: (i - 1) * 0.3 + 's' }"></span>
        </div>
      </div>
      <template #footer>
        <el-button @click="cancelMatchmaking" :loading="isCancelling">取消匹配</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UserFilled, ArrowDown, Search, Clock, Trophy, QuestionFilled, RefreshRight, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getRoomListApi, getOnlineCountApi, createRoomApi, joinRoomApi, joinMatchmakingApi, cancelMatchmakingApi, checkMatchStatusApi } from '@/api/room'

const router = useRouter()
const userStore = useUserStore()

// ---- 快速匹配状态 ----
const showMatchingDialog = ref(false)
const matchingMessage = ref('正在匹配对手...')
const isCancelling = ref(false)
let matchPollTimer = null
let matchPollActive = false

const onlineCount = ref(0)
const isLoading = ref(false)
const searchKeyword = ref('')
const statusFilter = ref('all')
const modeFilter = ref('all')
const hidePassword = ref(false)
const showCreateDialog = ref(false)
const showPasswordDialog = ref(false)
const isCreating = ref(false)
const joinPassword = ref('')
let pendingJoinRoom = null
const createFormRef = ref(null)

const createForm = reactive({
  gameMode: 'multi',  // 'multi' | 'single'
  name: '',
  maxPlayers: 6,
  gameDuration: 300,
  hasPassword: false,
  password: '',
  allowBots: false  // AI机器人陪练
})

const createRules = {
  gameMode: [{ required: true }],
  name: [
    { required: true, message: '请输入房间名称', trigger: 'blur' },
    { min: 2, max: 20, message: '房间名称2-20个字符', trigger: 'blur' }
  ]
}
const playerMarks = { 2: '2人', 4: '4人', 6: '6人', 8: '8人' }

const roomList = ref([])

const filteredRooms = computed(() => {
  let rooms = [...roomList.value]
  // 大厅默认不展示已结束的房间（游戏结束应注销）
  rooms = rooms.filter(r => r.status !== 'finished')
  if (searchKeyword.value) rooms = rooms.filter(r => r.name.includes(searchKeyword.value))
  if (statusFilter.value === 'waiting') rooms = rooms.filter(r => r.status === 'waiting' && r.playerCount < r.maxPlayers)
  if (statusFilter.value === 'playing') rooms = rooms.filter(r => r.status === 'playing')
  if (statusFilter.value === 'full') rooms = rooms.filter(r => r.playerCount >= r.maxPlayers)
  if (modeFilter.value === 'multi') rooms = rooms.filter(r => r.gameMode === 'multi')
  if (modeFilter.value === 'single') rooms = rooms.filter(r => r.gameMode === 'single')
  if (hidePassword.value) rooms = rooms.filter(r => !r.hasPassword)
  return rooms
})

const recentRooms = computed(() => {
  try {
    const recents = JSON.parse(localStorage.getItem('snake_recent_rooms') || '[]')
    // 只保留在服务器房间列表中仍然存在的房间
    const existingIds = new Set(roomList.value.map(r => r.id))
    return recents.filter(r => existingIds.has(r.id))
  } catch { return [] }
})

function getStatusType(s) { return s === 'waiting' ? 'success' : s === 'playing' ? 'warning' : s === 'finished' ? 'info' : 'danger' }
function getStatusText(s) { return s === 'waiting' ? '等待中' : s === 'playing' ? '游戏中' : s === 'finished' ? '已结束' : '已满' }
function getProgressColor(r) {
  const p = r.playerCount / r.maxPlayers
  return p >= 1 ? '#ef5350' : p >= 0.7 ? '#ffa726' : '#66bb6a'
}
function formatDuration(s) {
  if (!Number.isFinite(s) || s <= 0) return '无限'
  return Math.floor(s / 60) + '分钟'
}

async function refreshRoomList() {
  isLoading.value = true
  try {
    const data = await getRoomListApi({ page: 1, size: 100 })
    roomList.value = data.list || []
  } catch (e) {
    ElMessage.error('获取房间列表失败')
  } finally {
    isLoading.value = false
  }
}

async function fetchOnlineCount() {
  try {
    const data = await getOnlineCountApi()
    if (data && typeof data.count === 'number') {
      onlineCount.value = data.count
    }
  } catch (e) {
    // 静默失败，保持上次的值
  }
}

async function handleQuickMatch() {
  showMatchingDialog.value = true
  matchingMessage.value = '正在匹配对手...'
  isCancelling.value = false

  try {
    const result = await joinMatchmakingApi()
    if (result.matched) {
      matchingMessage.value = '匹配成功！即将进入房间...'
      setTimeout(() => {
        showMatchingDialog.value = false
        router.push('/room/' + result.roomId)
      }, 800)
      return
    }
    startMatchPolling()
  } catch (e) {
    console.error('加入匹配失败:', e)
    ElMessage.error('匹配失败，请重试')
    showMatchingDialog.value = false
  }
}

function startMatchPolling() {
  matchPollActive = true
  matchingMessage.value = '正在寻找对手...'

  async function poll() {
    if (!matchPollActive) return
    try {
      const result = await checkMatchStatusApi()
      if (result.matched) {
        matchPollActive = false
        matchingMessage.value = '匹配成功！即将进入房间...'
        setTimeout(() => {
          showMatchingDialog.value = false
          router.push('/room/' + result.roomId)
        }, 800)
        return
      }
      if (!result.inQueue) {
        matchPollActive = false
        matchingMessage.value = '匹配已取消'
        setTimeout(() => { showMatchingDialog.value = false }, 1000)
        return
      }
      matchingMessage.value = '正在寻找对手...（队列中 ' + (result.queueSize || 0) + ' 人）'
      matchPollTimer = setTimeout(poll, 1500)
    } catch (e) {
      console.error('轮询匹配状态失败:', e)
      matchPollTimer = setTimeout(poll, 2000)
    }
  }

  matchPollTimer = setTimeout(poll, 1500)
}

async function cancelMatchmaking() {
  isCancelling.value = true
  matchPollActive = false
  if (matchPollTimer) {
    clearTimeout(matchPollTimer)
    matchPollTimer = null
  }
  try {
    await cancelMatchmakingApi()
  } catch (e) {
    console.error('取消匹配失败:', e)
  }
  showMatchingDialog.value = false
  isCancelling.value = false
}

function handleJoinRoom(room) {
  // 单人模式房间，如果已满(状态full或人数已满)，不允许加入
  if (room.playerCount >= room.maxPlayers || room.status === 'full') { ElMessage.warning('房间已满，无法加入'); return }
  if (room.status === 'playing') { ElMessage.warning('游戏已开始，无法加入'); return }
  if (room.status === 'finished') { ElMessage.warning('游戏已结束，无法加入'); return }
  // 只有等待中的房间可以加入
  if (room.status !== 'waiting') { ElMessage.warning('该房间不可加入'); return }
  if (room.hasPassword) { pendingJoinRoom = room; joinPassword.value = ''; showPasswordDialog.value = true; return }
  doJoinRoom(room)
}

function confirmJoinWithPassword() {
  if (!joinPassword.value) { ElMessage.warning('请输入房间密码'); return }
  showPasswordDialog.value = false
  pendingJoinRoom = { ...pendingJoinRoom, password: joinPassword.value }
  doJoinRoom(pendingJoinRoom)
}

async function doJoinRoom(room) {
  try {
    await joinRoomApi(room.id, room.password || '')
    const recents = recentRooms.value.filter(function(r) { return r.id !== room.id })
    recents.unshift(room)
    localStorage.setItem('snake_recent_rooms', JSON.stringify(recents.slice(0, 5)))
    sessionStorage.setItem('joined_room_data', JSON.stringify(room))
    router.push('/room/' + room.id)
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '加入房间失败')
  }
}

async function handleCreateRoom() {
  if (!createFormRef.value) return
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return
  isCreating.value = true
  var result = await createRoomApi({
    name: createForm.name,
    gameMode: createForm.gameMode,
    maxPlayers: createForm.gameMode === 'single' ? 1 : createForm.maxPlayers,
    gameDuration: createForm.gameDuration,
    hasPassword: createForm.hasPassword,
    password: createForm.password || '',
    allowBots: createForm.allowBots
  })
  sessionStorage.setItem('new_room_config', JSON.stringify({
    roomId: result.roomId,
    name: result.name,
    gameMode: result.gameMode,
    maxPlayers: result.maxPlayers,
    gameDuration: result.gameDuration,
    hasPassword: result.hasPassword,
    password: result.password,
    allowBots: result.allowBots
  }))
  isCreating.value = false
  showCreateDialog.value = false
  ElMessage.success('房间创建成功！')
  router.push('/room/' + result.roomId)
}

function handleLink(type) {
  if (type === 'history') {
    router.push('/history')
  } else if (type === 'rank') {
    router.push('/ranking')
  } else if (type === 'guide') {
    router.push('/guide')
  }
}

function handleLogout() {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
  }).then(() => { userStore.logout(); router.push('/login') }).catch(() => {})
}

onMounted(() => {
  refreshRoomList()
  fetchOnlineCount()
  setInterval(() => refreshRoomList(), 5000)
  setInterval(() => fetchOnlineCount(), 15000)
})

onUnmounted(() => {
  // 清理匹配轮询
  matchPollActive = false
  if (matchPollTimer) {
    clearTimeout(matchPollTimer)
    matchPollTimer = null
  }
})
</script>

<style scoped>
.lobby-container { width: 100%; height: 100vh; display: flex; flex-direction: column; background: var(--bg-main); overflow: hidden; }

/* 装饰条 */
.top-decor {
  height: 3px; flex-shrink: 0;
  background: linear-gradient(90deg, #a5d6a7, #66bb6a, #43a047, #66bb6a, #a5d6a7);
}

/* 顶栏 */
.lobby-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 0 24px; height: 56px;
  background: var(--bg-card); border-bottom: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm); flex-shrink: 0;
}
.header-left { display: flex; align-items: center; gap: 20px; }
.logo { font-size: 18px; font-weight: 700; color: var(--primary-dark); letter-spacing: 1px; }
.online-badge { font-size: 13px; color: var(--text-secondary); display: flex; align-items: center; gap: 6px; }
.online-badge .dot { width: 8px; height: 8px; border-radius: 50%; background: var(--success-color); animation: pulse-dot 2s infinite; }
@keyframes pulse-dot { 0%,100%{opacity:1} 50%{opacity:0.4} }
.user-dropdown { display: flex; align-items: center; gap: 8px; cursor: pointer; padding: 4px 10px; border-radius: 8px; transition: background 0.2s; }
.user-dropdown:hover { background: var(--bg-hover); }
.username { color: var(--text-primary); font-size: 14px; }

/* 主体 */
.lobby-body { flex: 1; display: flex; overflow: hidden; }

/* 侧边栏 */
.lobby-sidebar {
  width: 260px; flex-shrink: 0; padding: 20px 16px;
  border-right: 1px solid var(--border-color);
  background: var(--bg-card); overflow-y: auto;
}
.sidebar-card {
  display: flex; align-items: center; gap: 14px;
  padding: 16px; border-radius: 12px; margin-bottom: 14px;
  cursor: pointer; transition: all 0.25s; border: 1px solid transparent;
}
.quick-match {
  background: linear-gradient(135deg, #e8f5e9, #f1f8e9);
  border-color: #c8e6c9;
}
.quick-match:hover { background: #c8e6c9; transform: translateY(-2px); }
.create-room {
  background: linear-gradient(135deg, #e3f2fd, #e8eaf6);
  border-color: #bbdefb;
}
.create-room:hover { background: #bbdefb; transform: translateY(-2px); }
.match-icon { font-size: 28px; }
.match-title { font-size: 15px; font-weight: 700; color: var(--text-primary); display: block; }
.match-desc { font-size: 12px; color: var(--text-secondary); margin-top: 2px; display: block; }
.sidebar-section { margin-top: 20px; }
.section-title { font-size: 12px; color: var(--text-muted); text-transform: uppercase; letter-spacing: 2px; margin-bottom: 10px; }
.quick-links { display: flex; flex-direction: column; gap: 4px; }
.quick-link {
  display: flex; align-items: center; gap: 8px;
  padding: 9px 12px; border-radius: 8px; font-size: 13px;
  color: var(--text-secondary); cursor: pointer; transition: all 0.2s;
}
.quick-link:hover { background: var(--bg-hover); color: var(--primary-dark); }
.recent-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 9px 12px; border-radius: 8px; font-size: 13px;
  color: var(--text-secondary); cursor: pointer;
}
.recent-item:hover { background: var(--bg-hover); }

/* 主区域 */
.lobby-main { flex: 1; display: flex; flex-direction: column; padding: 20px 24px; overflow: hidden; }
.filter-bar { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; flex-shrink: 0; }
.search-input { width: 200px; }

.room-grid {
  flex: 1; overflow-y: auto;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(290px, 1fr));
  gap: 16px; align-content: start;
}

.room-card {
  background: var(--bg-card); border: 1px solid var(--border-color);
  border-radius: 12px; padding: 18px; cursor: pointer;
  transition: all 0.25s; box-shadow: var(--shadow-sm);
}
.room-card:hover {
  border-color: var(--primary-light);
  transform: translateY(-3px);
  box-shadow: var(--shadow-lg);
}
.room-card.is-full { opacity: 0.55; cursor: not-allowed; }
.room-card.is-full:hover { transform: none; border-color: var(--border-color); }
.room-card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.room-name { font-size: 16px; font-weight: 700; color: var(--text-primary); display: flex; align-items: center; gap: 6px; }
.room-tags { display: flex; gap: 6px; }
.room-card-body { margin-bottom: 10px; }
.room-stat { display: flex; justify-content: space-between; font-size: 13px; color: var(--text-secondary); margin-bottom: 10px; }
.room-players { display: flex; align-items: center; gap: 4px; }
.player-avatar-mini { border: 1px solid var(--border-light); }
.more-players { font-size: 11px; color: var(--text-muted); margin-left: 4px; }

/* 模式描述 */
.mode-desc { font-size: 11px; color: var(--text-muted); margin-top: 4px; line-height: 1.3; }

/* AI陪练提示 */
.bot-hint { font-size: 12px; color: var(--text-secondary); margin-left: 10px; }

/* Element Plus 浅色覆盖 */
:deep(.el-input__wrapper), :deep(.el-select__wrapper) {
  background: #fff !important; border-color: var(--border-color) !important;
}
:deep(.el-dialog) {
  background: #fff; border: 1px solid var(--border-color); border-radius: 14px;
}
:deep(.el-dialog__title) { color: var(--text-primary); }
:deep(.el-radio-button__inner) {
  border-radius: 8px;
}

/* 快速匹配动画 */
.matching-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0;
}
.matching-animation {
  position: relative;
  width: 100px;
  height: 100px;
  margin-bottom: 20px;
}
.matching-pulse {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2;
}
.pulse-icon {
  font-size: 36px;
  animation: pulse-icon 1.2s ease-in-out infinite;
}
@keyframes pulse-icon {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.2); opacity: 0.8; }
}
.matching-rings {
  position: absolute;
  inset: 0;
}
.ring {
  position: absolute;
  inset: -8px;
  border: 2px solid var(--primary-color);
  border-radius: 50%;
  opacity: 0;
  animation: ring-expand 1.8s ease-out infinite;
}
.ring.r2 { animation-delay: 0.6s; }
.ring.r3 { animation-delay: 1.2s; }
@keyframes ring-expand {
  0% { transform: scale(0.6); opacity: 0.6; }
  100% { transform: scale(1.3); opacity: 0; }
}
.matching-status {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 6px;
}
.matching-hint {
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 14px;
}
.matching-dots {
  display: flex;
  gap: 8px;
}
.mdot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--primary-color);
  animation: dot-bounce 0.9s ease-in-out infinite;
}
@keyframes dot-bounce {
  0%, 80%, 100% { transform: translateY(0); opacity: 0.3; }
  40% { transform: translateY(-10px); opacity: 1; }
}
</style>
