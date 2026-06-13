<!--
  RankingView.vue - 排行榜页面（小清新绿色主题）
  功能：总榜 / 多人榜 / 单人榜切换、前三名领奖台、排名表格、我的排名卡片
-->
<template>
  <div class="ranking-container">
    <!-- 顶部装饰条 -->
    <div class="top-decor"></div>

    <!-- 顶部栏 -->
    <header class="ranking-header">
      <div class="header-left">
        <el-button text @click="goBack" class="back-btn">
          <el-icon><ArrowLeft /></el-icon> 返回大厅
        </el-button>
        <span class="page-title">🏆 排行榜</span>
      </div>
    </header>

    <!-- 模式切换 Tab -->
    <div class="tabs-bar">
      <div
        v-for="tab in tabs" :key="tab.key"
        :class="['tab-item', { active: currentMode === tab.key }]"
        @click="switchMode(tab.key)"
      >
        <span class="tab-icon">{{ tab.icon }}</span>
        <span class="tab-label">{{ tab.label }}</span>
      </div>
    </div>

    <!-- 我的排名卡片 -->
    <div class="my-rank-card" v-if="myRank && myRank.overallRank > 0" @click="scrollToMyRank">
      <div class="my-rank-left">
        <el-avatar :size="40" :icon="UserFilled" />
        <div class="my-rank-info">
          <span class="my-rank-name">{{ myRank.nickname || '我' }}</span>
          <span class="my-rank-pos">
            总榜 <strong>#{{ myRank.overallRank }}</strong>
            <span class="sep">|</span>
            多人最高分 <strong>#{{ myRank.multiRank || '-' }}</strong>
          </span>
        </div>
      </div>
      <div class="my-rank-right">
        <div class="my-stat">
          <span class="my-stat-val">{{ myRank.totalScore }}</span>
          <span class="my-stat-label">总分</span>
        </div>
        <div class="my-stat">
          <span class="my-stat-val">{{ myRank.wins }}</span>
          <span class="my-stat-label">胜场</span>
        </div>
        <div class="my-stat">
          <span class="my-stat-val">{{ myRank.totalGames }}</span>
          <span class="my-stat-label">场次</span>
        </div>
        <el-icon class="arrow-right"><ArrowRight /></el-icon>
      </div>
    </div>

    <!-- 前三名领奖台 -->
    <div class="podium" v-if="topThree.length >= 3 && currentPage === 1">
      <div class="podium-item second" v-if="topThree[1]">
        <div class="podium-avatar">
          <el-avatar :size="52" :icon="UserFilled" />
        </div>
        <span class="podium-name">{{ topThree[1].nickname }}</span>
        <span class="podium-score">{{ topThree[1].totalScore }} 分</span>
        <span class="podium-score-label">{{ scoreLabel }}</span>
        <div class="podium-stand">
          <span class="podium-medal">🥈</span>
          <span class="podium-label">NO.2</span>
        </div>
      </div>
      <div class="podium-item first" v-if="topThree[0]">
        <div class="podium-avatar champion">
          <el-avatar :size="64" :icon="UserFilled" />
          <span class="crown">👑</span>
        </div>
        <span class="podium-name">{{ topThree[0].nickname }}</span>
        <span class="podium-score">{{ topThree[0].totalScore }} 分</span>
        <span class="podium-score-label">{{ scoreLabel }}</span>
        <div class="podium-stand first-stand">
          <span class="podium-medal">🥇</span>
          <span class="podium-label">NO.1</span>
        </div>
      </div>
      <div class="podium-item third" v-if="topThree[2]">
        <div class="podium-avatar">
          <el-avatar :size="48" :icon="UserFilled" />
        </div>
        <span class="podium-name">{{ topThree[2].nickname }}</span>
        <span class="podium-score">{{ topThree[2].totalScore }} 分</span>
        <span class="podium-score-label">{{ scoreLabel }}</span>
        <div class="podium-stand">
          <span class="podium-medal">🥉</span>
          <span class="podium-label">NO.3</span>
        </div>
      </div>
    </div>

    <!-- 排名表格 -->
    <div class="table-section" v-loading="loading">
      <el-empty v-if="!loading && rankingList.length === 0" description="暂无排行数据" :image-size="100" />

      <template v-else>
        <el-table
          :data="rankingList"
          stripe
          style="width: 100%"
          row-key="userId"
          highlight-current-row
          :row-class-name="getRowClass"
        >
          <el-table-column label="排名" width="80" align="center">
            <template #default="{ row }">
              <span :class="getRankClass(row.rank)">
                <template v-if="row.rank === 1">🥇</template>
                <template v-else-if="row.rank === 2">🥈</template>
                <template v-else-if="row.rank === 3">🥉</template>
                <template v-else>#{{ row.rank }}</template>
              </span>
            </template>
          </el-table-column>

          <el-table-column label="玩家" min-width="180">
            <template #default="{ row }">
              <div class="player-cell">
                <el-avatar :size="32" :icon="UserFilled" />
                <span class="player-name">
                  {{ row.nickname || row.userId }}
                  <el-tag v-if="isMe(row)" type="success" size="small" effect="dark">我</el-tag>
                </span>
              </div>
            </template>
          </el-table-column>

          <el-table-column :label="scoreLabel" width="120" sortable prop="totalScore" align="center">
            <template #default="{ row }">
              <span class="score-val">{{ row.totalScore }}</span>
            </template>
          </el-table-column>

          <el-table-column label="胜场" width="80" sortable prop="wins" align="center" />

          <el-table-column label="场次" width="80" sortable prop="totalGames" align="center" />

          <el-table-column v-if="currentMode !== 'overall'" label="击杀" width="80" sortable prop="kills" align="center" />

          <el-table-column label="胜率" width="90" align="center">
            <template #default="{ row }">
              {{ row.totalGames > 0 ? ((row.wins / row.totalGames) * 100).toFixed(1) + '%' : '-' }}
            </template>
          </el-table-column>

          <el-table-column label="等级" width="70" align="center">
            <template #default="{ row }">
              <el-tag size="small" effect="plain" type="success">Lv.{{ row.level || 1 }}</el-tag>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[20, 50]"
            :total="total"
            layout="total, prev, pager, next"
            background
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
          />
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ArrowRight, UserFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getRankingApi, getMyRankApi } from '@/api/game'

const router = useRouter()
const userStore = useUserStore()

// ---- 状态 ----
const loading = ref(true)
const currentMode = ref('overall')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const rankingList = ref([])
const myRank = ref(null)

const tabs = [
  { key: 'overall', label: '总榜', icon: '🏆' },
  { key: 'multi', label: '多人对战', icon: '👥' },
  { key: 'single', label: '单人无尽', icon: '🧘' }
]

// ---- 计算属性 ----
const topThree = computed(() => {
  if (currentPage.value !== 1) return []
  return rankingList.value.slice(0, 3)
})

const currentUserId = computed(() => userStore.userInfo.id)

/** 分数列标签：总榜显示"总分"，模式榜显示"最高分" */
const scoreLabel = computed(() => currentMode.value === 'overall' ? '总分' : '最高分')

// ---- 生命周期 ----
onMounted(() => {
  fetchRanking()
  fetchMyRank()
})

// ---- 方法 ----
async function fetchRanking() {
  loading.value = true
  try {
    const data = await getRankingApi(currentMode.value, currentPage.value, pageSize.value)
    rankingList.value = data.list || []
    total.value = data.total || 0
  } catch (e) {
    console.error('获取排行榜失败:', e)
    ElMessage.error('排行榜加载失败')
    rankingList.value = []
  } finally {
    loading.value = false
  }
}

async function fetchMyRank() {
  try {
    const data = await getMyRankApi()
    myRank.value = data
  } catch (e) {
    console.error('获取我的排名失败:', e)
    // 静默失败，不影响主列表
  }
}

function switchMode(mode) {
  currentMode.value = mode
  currentPage.value = 1
  fetchRanking()
}

function handlePageChange(page) {
  currentPage.value = page
  fetchRanking()
}

function handleSizeChange(size) {
  pageSize.value = size
  currentPage.value = 1
  fetchRanking()
}

function scrollToMyRank() {
  // 切换到总榜并尝试定位自己
  if (currentMode.value !== 'overall') {
    currentMode.value = 'overall'
    currentPage.value = 1
    fetchRanking()
  }
}

function isMe(row) {
  return String(row.userId) === String(currentUserId.value)
}

function getRankClass(rank) {
  if (rank === 1) return 'rank-gold'
  if (rank === 2) return 'rank-silver'
  if (rank === 3) return 'rank-bronze'
  return ''
}

function getRowClass({ row }) {
  if (isMe(row)) return 'my-row'
  return ''
}

function goBack() {
  router.push('/lobby')
}
</script>

<style scoped>
.ranking-container {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-main);
  overflow: hidden;
}

.top-decor {
  height: 3px;
  flex-shrink: 0;
  background: linear-gradient(90deg, #a5d6a7, #66bb6a, #43a047, #66bb6a, #a5d6a7);
}

/* 顶栏 */
.ranking-header {
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
.back-btn { font-size: 14px; color: var(--primary-dark); }
.page-title { font-size: 20px; font-weight: 700; color: var(--text-primary); }

/* Tab 切换栏 */
.tabs-bar {
  display: flex;
  justify-content: center;
  gap: 4px;
  padding: 16px 24px 0;
  flex-shrink: 0;
}
.tab-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 28px;
  border-radius: 10px 10px 0 0;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-bottom: none;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
  color: var(--text-secondary);
  user-select: none;
}
.tab-item:hover { background: var(--bg-hover); color: var(--text-primary); }
.tab-item.active {
  background: var(--primary-color);
  color: #fff;
  border-color: var(--primary-color);
  font-weight: 600;
}
.tab-icon { font-size: 18px; }

/* 我的排名卡片 */
.my-rank-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 12px 24px 0;
  padding: 14px 20px;
  background: linear-gradient(135deg, #e8f5e9, #c8e6c9);
  border: 1px solid #a5d6a7;
  border-radius: 12px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  flex-shrink: 0;
}
.my-rank-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(102, 187, 106, 0.2);
}
.my-rank-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.my-rank-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.my-rank-name { font-weight: 600; font-size: 15px; color: var(--text-primary); }
.my-rank-pos { font-size: 13px; color: var(--text-secondary); }
.my-rank-pos strong { color: var(--primary-dark); font-size: 15px; }
.sep { margin: 0 6px; color: var(--text-muted); }
.my-rank-right {
  display: flex;
  align-items: center;
  gap: 20px;
}
.my-stat { text-align: center; }
.my-stat-val { display: block; font-size: 16px; font-weight: 700; color: var(--primary-dark); }
.my-stat-label { font-size: 11px; color: var(--text-muted); }
.arrow-right { color: var(--primary-dark); font-size: 18px; }

/* 领奖台 */
.podium {
  display: flex;
  justify-content: center;
  align-items: flex-end;
  gap: 16px;
  padding: 24px 24px 0;
  flex-shrink: 0;
}
.podium-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}
.podium-item.first { order: 2; }
.podium-item.second { order: 1; }
.podium-item.third { order: 3; }
.podium-avatar { position: relative; }
.podium-avatar.champion {
  border: 3px solid #ffd54f;
  border-radius: 50%;
  padding: 2px;
}
.crown {
  position: absolute;
  top: -18px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 22px;
}
.podium-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.podium-score {
  font-size: 12px;
  color: var(--text-secondary);
}
.podium-score-label {
  font-size: 10px;
  color: var(--text-muted);
}
.podium-stand {
  text-align: center;
  padding: 8px 24px;
  border-radius: 8px 8px 0 0;
  background: #e0e0e0;
  min-width: 80px;
}
.podium-stand.first-stand {
  background: #ffd54f;
  padding: 14px 32px;
}
.podium-medal { font-size: 24px; display: block; }
.podium-label { font-size: 12px; font-weight: 700; color: var(--text-secondary); }
.podium-stand.first-stand .podium-label { color: #5d4037; }

/* 表格区域 */
.table-section {
  flex: 1;
  overflow: auto;
  padding: 12px 24px 20px;
}
.player-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.player-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
}
.score-val { font-weight: 700; color: var(--primary-dark); }

/* 排名颜色 */
.rank-gold { color: #f9a825; font-weight: 700; font-size: 16px; }
.rank-silver { color: #78909c; font-weight: 700; font-size: 16px; }
.rank-bronze { color: #e65100; font-weight: 700; font-size: 16px; }

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 16px 0 8px;
}

/* 高亮自己 */
:deep(.my-row) { background: #e8f5e9 !important; }
:deep(.my-row:hover) { background: #c8e6c9 !important; }
</style>
