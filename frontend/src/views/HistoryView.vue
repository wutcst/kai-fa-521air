<!--
  HistoryView.vue - 对战记录页面（小清新绿色主题）
  功能：统计概览卡片、对战历史表格、支持筛选和展开详情
-->
<template>
  <div class="history-container">
    <!-- 顶部装饰条 -->
    <div class="top-decor"></div>

    <!-- 顶部栏 -->
    <header class="history-header">
      <div class="header-left">
        <el-button text @click="goBack" class="back-btn">
          <el-icon><ArrowLeft /></el-icon> 返回大厅
        </el-button>
        <span class="page-title">📋 对战记录</span>
      </div>
    </header>

    <!-- 统计卡片 -->
    <div class="stats-section" v-loading="statsLoading">
      <div class="stats-grid">
        <div class="stat-card">
          <span class="stat-icon">🎮</span>
          <span class="stat-val">{{ stats.totalGames }}</span>
          <span class="stat-label">总场次</span>
        </div>
        <div class="stat-card highlight-gold">
          <span class="stat-icon">🏆</span>
          <span class="stat-val">{{ stats.wins }}</span>
          <span class="stat-label">胜场</span>
        </div>
        <div class="stat-card">
          <span class="stat-icon">📊</span>
          <span class="stat-val">{{ stats.winRate }}%</span>
          <span class="stat-label">胜率</span>
        </div>
        <div class="stat-card highlight-green">
          <span class="stat-icon">⭐</span>
          <span class="stat-val">{{ stats.bestScore }}</span>
          <span class="stat-label">最高分</span>
        </div>
        <div class="stat-card">
          <span class="stat-icon">📈</span>
          <span class="stat-val">{{ stats.avgScore }}</span>
          <span class="stat-label">场均分</span>
        </div>
        <div class="stat-card">
          <span class="stat-icon">⚔️</span>
          <span class="stat-val">{{ stats.totalKills }}</span>
          <span class="stat-label">总击杀</span>
        </div>
        <div class="stat-card">
          <span class="stat-icon">🎯</span>
          <span class="stat-val">{{ stats.avgRank }}</span>
          <span class="stat-label">场均排名</span>
        </div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-select v-model="modeFilter" placeholder="游戏模式" style="width:150px" @change="handleFilterChange">
        <el-option label="全部模式" value="all" />
        <el-option label="👥 多人对战" value="multi" />
        <el-option label="🧘 单人无尽" value="single" />
      </el-select>
      <el-button :icon="RefreshRight" circle @click="fetchHistory" :loading="listLoading" />
    </div>

    <!-- 对战列表 -->
    <div class="history-table-section" v-loading="listLoading">
      <el-empty v-if="!listLoading && historyList.length === 0" description="暂无对战记录，快去开始游戏吧！" :image-size="120">
        <el-button type="primary" @click="goBack">前往大厅</el-button>
      </el-empty>

      <template v-else>
        <el-table
          :data="historyList"
          stripe
          style="width: 100%"
          row-key="id"
          @row-click="toggleExpand"
          :row-class-name="getRowClass"
          highlight-current-row
        >
          <el-table-column type="expand">
            <template #default="{ row }">
              <div class="expand-content" v-loading="row._loadingDetail">
                <template v-if="row._detail">
                  <div class="expand-game-info">
                    <span><strong>对局ID：</strong>{{ row._detail.id }}</span>
                    <span><strong>房间ID：</strong>{{ row._detail.roomId || '-' }}</span>
                    <span><strong>模式：</strong>{{ row._detail.gameMode === 'single' ? '🧘 单人无尽' : '👥 多人对战' }}</span>
                    <span><strong>时长：</strong>{{ formatDuration(row._detail.duration) }}</span>
                    <span><strong>人数：</strong>{{ row._detail.playerCount }} 人</span>
                    <span><strong>时间：</strong>{{ formatTime(row._detail.createdAt) }}</span>
                  </div>
                  <!-- 排名列表 -->
                  <div class="expand-rankings" v-if="row._detail.players && row._detail.players.length">
                    <h5>🏅 本局排名</h5>
                    <div
                      v-for="(p, idx) in row._detail.players"
                      :key="idx"
                      :class="['rank-row', { 'is-me': String(p.userId) === String(currentUserId), 'is-top': idx === 0 }]"
                    >
                      <span class="rank-pos">
                        <template v-if="idx === 0">🥇</template>
                        <template v-else-if="idx === 1">🥈</template>
                        <template v-else-if="idx === 2">🥉</template>
                        <template v-else>#{{ idx + 1 }}</template>
                      </span>
                      <span class="rank-nickname">
                        {{ p.nickname || p.userId }}
                        <el-tag v-if="String(p.userId) === String(currentUserId)" type="success" size="small">我</el-tag>
                        <el-tag v-if="p.isBot" type="info" size="small">🤖 Bot</el-tag>
                      </span>
                      <span class="rank-detail">⚔ {{ p.kills }} 击杀 | 🐍 长度 {{ p.snakeLength }}</span>
                      <span class="rank-score">{{ p.score }} 分</span>
                    </div>
                  </div>
                </template>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="id" label="对局ID" width="100" />
          <el-table-column label="模式" width="120">
            <template #default="{ row }">
              <el-tag :type="row.gameMode === 'single' ? 'success' : 'primary'" size="small" effect="plain">
                {{ row.gameMode === 'single' ? '🧘 单人' : '👥 多人' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="排名" width="80" sortable prop="rank">
            <template #default="{ row }">
              <span :class="getRankClass(row.rank)">
                <template v-if="row.rank === 1">🥇</template>
                <template v-else-if="row.rank === 2">🥈</template>
                <template v-else-if="row.rank === 3">🥉</template>
                <template v-else>#{{ row.rank }}</template>
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="score" label="得分" width="100" sortable />
          <el-table-column prop="kills" label="击杀" width="80" sortable />
          <el-table-column label="人数" width="80">
            <template #default="{ row }">{{ row.playerCount }}人</template>
          </el-table-column>
          <el-table-column label="时长" width="100">
            <template #default="{ row }">{{ formatDuration(row.duration) }}</template>
          </el-table-column>
          <el-table-column label="存活" width="80">
            <template #default="{ row }">
              <el-tag :type="row.isAlive ? 'success' : 'danger'" size="small" effect="dark">
                {{ row.isAlive ? '存活' : '淘汰' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="时间" width="180">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" size="small" @click.stop="viewDetail(row)">
                详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50]"
            :total="total"
            layout="total, sizes, prev, pager, next, jumper"
            background
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </template>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="📋 对局详情" width="600px" center :close-on-click-modal="true">
      <div v-if="detailData" v-loading="detailLoading" class="detail-dialog-content">
        <div class="detail-info-row">
          <div class="detail-info-item">
            <span class="d-label">对局ID</span>
            <span class="d-value">{{ detailData.id }}</span>
          </div>
          <div class="detail-info-item">
            <span class="d-label">游戏模式</span>
            <span class="d-value">{{ detailData.gameMode === 'single' ? '🧘 单人无尽' : '👥 多人对战' }}</span>
          </div>
          <div class="detail-info-item">
            <span class="d-label">对局时长</span>
            <span class="d-value">{{ formatDuration(detailData.duration) }}</span>
          </div>
          <div class="detail-info-item">
            <span class="d-label">参与人数</span>
            <span class="d-value">{{ detailData.playerCount }} 人</span>
          </div>
          <div class="detail-info-item">
            <span class="d-label">开始时间</span>
            <span class="d-value">{{ formatTime(detailData.startedAt) }}</span>
          </div>
          <div class="detail-info-item">
            <span class="d-label">结束时间</span>
            <span class="d-value">{{ formatTime(detailData.endedAt) }}</span>
          </div>
        </div>
        <div class="detail-rankings" v-if="detailData.players && detailData.players.length">
          <h5>🏅 玩家排名</h5>
          <div
            v-for="(p, idx) in detailData.players"
            :key="idx"
            :class="['d-rank-row', { 'is-me': String(p.userId) === String(currentUserId), 'is-champion': idx === 0 }]"
          >
            <span class="d-rank-num">
              <template v-if="idx === 0">🥇</template>
              <template v-else-if="idx === 1">🥈</template>
              <template v-else-if="idx === 2">🥉</template>
              <template v-else>#{{ idx + 1 }}</template>
            </span>
            <span class="d-rank-name">
              {{ p.nickname || p.userId }}
              <el-tag v-if="String(p.userId) === String(currentUserId)" type="success" size="small">我</el-tag>
            </span>
            <span class="d-rank-stats">
              ⚔ {{ p.kills }} 击杀 · 🐍 长度 {{ p.snakeLength }} · ⏱ {{ formatSurvival(p.survivalTime) }}
            </span>
            <span class="d-rank-score">{{ p.score }} 分</span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, RefreshRight } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getMyHistoryApi, getMyStatsApi, getGameResultApi } from '@/api/game'

const router = useRouter()
const userStore = useUserStore()

// ---- 状态 ----
const statsLoading = ref(true)
const listLoading = ref(true)
const detailLoading = ref(false)
const detailVisible = ref(false)

const stats = ref({
  totalGames: 0,
  wins: 0,
  winRate: '0.0',
  bestScore: 0,
  avgScore: 0,
  totalKills: 0,
  avgRank: '-'
})

const historyList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const modeFilter = ref('all')
const detailData = ref(null)
const expandedRowId = ref(null)

const currentUserId = computed(() => userStore.userInfo.id)

// ---- 生命周期 ----
onMounted(() => {
  fetchStats()
  fetchHistory()
})

// ---- 方法 ----
/** 获取统计概览 */
async function fetchStats() {
  statsLoading.value = true
  try {
    const data = await getMyStatsApi()
    stats.value = {
      totalGames: data.totalGames || 0,
      wins: data.wins || 0,
      winRate: data.totalGames > 0 ? ((data.wins / data.totalGames) * 100).toFixed(1) : '0.0',
      bestScore: data.bestScore || 0,
      avgScore: data.avgScore || 0,
      totalKills: data.totalKills || 0,
      avgRank: data.avgRank || '-'
    }
  } catch (e) {
    console.error('获取统计失败:', e)
    ElMessage.warning('统计加载失败')
  } finally {
    statsLoading.value = false
  }
}

/** 获取对战历史 */
async function fetchHistory() {
  listLoading.value = true
  try {
    const data = await getMyHistoryApi(currentPage.value, pageSize.value)
    historyList.value = (data.list || []).map(item => ({
      ...item,
      _detail: null,
      _loadingDetail: false
    }))
    total.value = data.total || 0
  } catch (e) {
    console.error('获取历史失败:', e)
    ElMessage.error('对战记录加载失败')
    historyList.value = []
    total.value = 0
  } finally {
    listLoading.value = false
  }
}

/** 筛选变化 */
function handleFilterChange() {
  currentPage.value = 1
  fetchHistory()
}

/** 分页变化 */
function handlePageChange(page) {
  currentPage.value = page
  fetchHistory()
}

function handleSizeChange(size) {
  pageSize.value = size
  currentPage.value = 1
  fetchHistory()
}

/** 点击行展开/折叠详情 */
async function toggleExpand(row) {
  if (expandedRowId.value === row.id) {
    expandedRowId.value = null
    return
  }
  expandedRowId.value = row.id

  if (row._detail) return // 已加载

  row._loadingDetail = true
  try {
    const detail = await getGameResultApi(row.id)
    row._detail = detail
  } catch (e) {
    console.error('获取对局详情失败:', e)
    ElMessage.warning('详情加载失败')
  } finally {
    row._loadingDetail = false
  }
}

/** 点击详情按钮查看详情弹窗 */
async function viewDetail(row) {
  detailVisible.value = true
  detailData.value = null
  detailLoading.value = true
  try {
    const detail = await getGameResultApi(row.id)
    detailData.value = detail
  } catch (e) {
    console.error('获取对局详情失败:', e)
    ElMessage.error('详情加载失败')
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

/** 返回大厅 */
function goBack() {
  router.push('/lobby')
}

// ---- 格式化工具 ----
function formatDuration(seconds) {
  if (!seconds && seconds !== 0) return '-'
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return m > 0 ? `${m}分${s}秒` : `${s}秒`
}

function formatTime(dateStr) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function formatSurvival(seconds) {
  if (!seconds && seconds !== 0) return '-'
  if (seconds < 60) return `${Math.floor(seconds)}秒`
  return `${Math.floor(seconds / 60)}分${Math.floor(seconds % 60)}秒`
}

function getRankClass(rank) {
  if (rank === 1) return 'rank-1'
  if (rank === 2) return 'rank-2'
  if (rank === 3) return 'rank-3'
  return 'rank-other'
}

function getRowClass({ row }) {
  return row.rank === 1 ? 'winner-row' : ''
}
</script>

<style scoped>
.history-container {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-main);
  overflow: hidden;
}

/* 装饰条 */
.top-decor {
  height: 3px;
  flex-shrink: 0;
  background: linear-gradient(90deg, #a5d6a7, #66bb6a, #43a047, #66bb6a, #a5d6a7);
}

/* 顶栏 */
.history-header {
  display: flex;
  align-items: center;
  padding: 12px 24px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-color);
  flex-shrink: 0;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.back-btn {
  font-size: 14px;
  color: var(--primary-dark);
}
.page-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
}

/* 统计卡片 */
.stats-section {
  padding: 20px 24px 0;
  flex-shrink: 0;
}
.stats-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 12px;
}
.stat-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  padding: 16px 12px;
  text-align: center;
  box-shadow: var(--shadow-sm);
  transition: transform 0.2s, box-shadow 0.2s;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}
.stat-icon { font-size: 22px; }
.stat-val { font-size: 22px; font-weight: 700; color: var(--text-primary); }
.stat-label { font-size: 12px; color: var(--text-secondary); }
.stat-card.highlight-gold { border-color: #ffd54f; background: #fffde7; }
.stat-card.highlight-gold .stat-val { color: #f9a825; }
.stat-card.highlight-green { border-color: #a5d6a7; background: #e8f5e9; }
.stat-card.highlight-green .stat-val { color: var(--primary-dark); }

/* 筛选栏 */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 24px;
  flex-shrink: 0;
}

/* 表格区域 */
.history-table-section {
  flex: 1;
  overflow: auto;
  padding: 0 24px 20px;
}

/* 展开内容 */
.expand-content {
  padding: 16px 24px;
  background: #f9fdf7;
  border-radius: 8px;
}
.expand-game-info {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 24px;
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 14px;
}
.expand-rankings h5 {
  font-size: 14px;
  margin-bottom: 8px;
  color: var(--text-primary);
}
.rank-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-radius: 6px;
  margin-bottom: 4px;
  background: var(--bg-card);
  font-size: 13px;
}
.rank-row.is-me { background: #e8f5e9; border-left: 3px solid var(--primary-color); }
.rank-row.is-top { background: #fffde7; }
.rank-pos { font-size: 18px; width: 32px; text-align: center; }
.rank-nickname { flex: 1; display: flex; align-items: center; gap: 6px; font-weight: 500; }
.rank-detail { color: var(--text-secondary); font-size: 12px; }
.rank-score { font-weight: 700; color: var(--primary-dark); font-size: 14px; }

/* 排名样式 */
.rank-1 { color: #f9a825; font-weight: 700; }
.rank-2 { color: #78909c; font-weight: 700; }
.rank-3 { color: #e65100; font-weight: 700; }
.rank-other { color: var(--text-secondary); }

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 20px 0 10px;
}

/* 详情弹窗 */
.detail-dialog-content {
  min-height: 200px;
}
.detail-info-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 20px;
}
.detail-info-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 8px 12px;
  background: #f9fdf7;
  border-radius: 6px;
}
.d-label { font-size: 12px; color: var(--text-muted); }
.d-value { font-size: 14px; font-weight: 600; color: var(--text-primary); }
.detail-rankings h5 {
  font-size: 14px;
  margin-bottom: 8px;
  color: var(--text-primary);
}
.d-rank-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 6px;
  margin-bottom: 4px;
  background: var(--bg-main);
  font-size: 13px;
}
.d-rank-row.is-me { background: #e8f5e9; border-left: 3px solid var(--primary-color); }
.d-rank-row.is-champion { background: #fffde7; }
.d-rank-num { font-size: 18px; width: 28px; }
.d-rank-name { flex: 1; display: flex; align-items: center; gap: 6px; }
.d-rank-stats { color: var(--text-secondary); font-size: 12px; }
.d-rank-score { font-weight: 700; color: var(--primary-dark); }

/* :deep 用于渗透 Element Plus 样式 */
:deep(.winner-row) { background: #fffde7 !important; }
:deep(.el-table__expanded-cell) { padding: 0 !important; }
</style>
