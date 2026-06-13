<!--
  ResultView.vue - 结算页面（v2 - 小清新绿色主题）
  功能：冠军展示、排名列表、MVP、个人数据、彩纸特效
-->
<template>
  <div class="result-container">
    <!-- 彩纸 -->
    <div class="confetti" v-if="showConfetti">
      <span v-for="i in 25" :key="i" class="particle" :style="getParticleStyle(i)"></span>
    </div>

    <div class="result-card" v-if="resultData">
      <!-- 标题 -->
      <div class="result-header">
        <h1>{{ isSingle ? '🧘 单人无尽结算' : '🏆 对局结算' }}</h1>
        <p class="game-id">对局ID：{{ resultData.gameId }}</p>
      </div>

      <!-- 冠军区域（仅多人） -->
      <div class="champion-area" v-if="!isSingle && rankings[0]">
        <div class="champion-crown">👑</div>
        <el-avatar :size="72" :icon="UserFilled" class="champion-avatar">
          {{ rankings[0].nickname?.charAt(0) }}
        </el-avatar>
        <h2 class="champion-name">{{ rankings[0].nickname }}</h2>
        <p class="champion-score">{{ rankings[0].score }} 分</p>
        <el-tag v-if="rankings[0].isMe" type="success" size="large">🏆 就是你！</el-tag>
        <el-tag v-if="rankings[0] === mvpPlayer" type="warning" size="small" effect="plain"
          >MVP</el-tag
        >
      </div>

      <!-- 单人模式成绩 -->
      <div class="single-result" v-if="isSingle && rankings[0]">
        <div class="single-big-score">{{ rankings[0].score }}</div>
        <div class="single-label">最终得分</div>
        <div class="single-stats">
          <span>🐍 最大长度：{{ rankings[0].length }}</span>
          <span>⏱ 存活时间：{{ formatSurvival(rankings[0].survivalTime || 0) }}</span>
        </div>
      </div>

      <!-- 排名列表（仅多人） -->
      <div class="rankings-list" v-if="!isSingle">
        <h3>📊 完整排名</h3>
        <TransitionGroup name="rank-reveal" tag="div">
          <div
            v-for="(player, index) in visibleRankings"
            :key="player.id"
            :class="[
              'ranking-item',
              { 'is-me': player.isMe, 'is-mvp': player === mvpPlayer, 'is-champion': index === 0 },
            ]"
          >
            <span class="rank-num">
              <template v-if="index === 0">🥇</template>
              <template v-else-if="index === 1">🥈</template>
              <template v-else-if="index === 2">🥉</template>
              <template v-else>#{{ index + 1 }}</template>
            </span>
            <div class="player-col">
              <span class="p-name">
                {{ player.nickname }}
                <el-tag v-if="player.isMe" type="success" size="small">我</el-tag>
                <el-tag v-if="player === mvpPlayer" type="warning" size="small" effect="plain"
                  >MVP</el-tag
                >
              </span>
              <div class="p-stats">
                <span>⚔ {{ player.kills || 0 }} 击杀</span>
                <span>🐍 长度 {{ player.length || 0 }}</span>
                <span>⏱ {{ formatSurvival(player.survivalTime || 0) }}</span>
              </div>
            </div>
            <span class="p-score">{{ player.score }} 分</span>
          </div>
        </TransitionGroup>
      </div>

      <!-- 个人数据 -->
      <div class="personal-card" v-if="myData && isOwner">
        <h4>📋 你的本局数据</h4>
        <div class="data-grid">
          <div class="data-item">
            <span class="data-val">{{ myData.score }}</span>
            <span class="data-label">总积分</span>
          </div>
          <div class="data-item" v-if="!isSingle">
            <span class="data-val">{{ myData.kills }}</span>
            <span class="data-label">击杀数</span>
          </div>
          <div class="data-item">
            <span class="data-val">{{ myData.length }}</span>
            <span class="data-label">最大长度</span>
          </div>
          <div class="data-item">
            <span class="data-val">{{ formatSurvival(myData.survivalTime || 0) }}</span>
            <span class="data-label">存活时间</span>
          </div>
          <div class="data-item" v-if="!isSingle">
            <span class="data-val">{{ myRank }}</span>
            <span class="data-label">最终排名</span>
          </div>
        </div>
      </div>

      <!-- 按钮：仅对局玩家显示操作按钮 -->
      <div class="action-buttons" v-if="isOwner">
        <el-button size="large" @click="$router.push('/lobby')">🏠 返回大厅</el-button>
        <el-button type="primary" size="large" @click="playAgain">🔄 再来一局</el-button>
        <el-button size="large" @click="openShareDialog" :icon="Share" plain>分享结果</el-button>
      </div>

      <!-- 分享链接访问者：仅显示分享按钮 -->
      <div class="action-buttons" v-else>
        <el-button size="large" @click="openShareDialog" :icon="Share" plain>分享此战绩</el-button>
      </div>

      <!-- 来源提示 -->
      <div class="shared-notice" v-if="!isOwner">
        <span>📤 此战绩由玩家分享</span>
      </div>
    </div>

    <div v-else-if="loading" class="no-data">
      <el-empty description="正在加载战绩..." :image-size="100" />
    </div>

    <div v-else class="no-data">
      <el-empty :description="loadError || '暂无结算数据'" :image-size="100">
        <el-button v-if="isOwner" type="primary" @click="$router.push('/lobby')"
          >返回大厅</el-button
        >
      </el-empty>
    </div>

    <!-- 分享弹窗 -->
    <el-dialog
      v-model="showShareDialog"
      title="📤 分享战绩"
      width="440px"
      center
      :close-on-click-modal="true"
    >
      <div class="share-content">
        <!-- 战绩摘要 -->
        <div class="share-preview">
          <div class="share-game-mode">{{ isSingle ? '🧘 单人无尽' : '🏆 多人对战' }}</div>
          <div class="share-my-line" v-if="myData">
            <template v-if="!isSingle"
              >排名 <strong>#{{ myRank }}</strong> ·
            </template>
            <strong>{{ myData.score }} 分</strong>
            <template v-if="!isSingle"> · ⚔ {{ myData.kills || 0 }} 击杀</template>
            <template v-if="myData.length"> · 🐍 长度 {{ myData.length }}</template>
          </div>
          <div class="share-game-id">对局ID：{{ resultData.gameId }}</div>
        </div>

        <!-- 复制链接 -->
        <div class="share-action">
          <div class="share-label">🔗 分享链接</div>
          <div class="share-input-row">
            <el-input :model-value="shareLink" readonly size="small" class="share-link-input" />
            <el-button type="primary" size="small" @click="copyLink" :icon="copyLinkIcon">
              {{ linkCopied ? '已复制' : '复制' }}
            </el-button>
          </div>
        </div>

        <!-- 复制文字 -->
        <div class="share-action">
          <div class="share-label">📝 分享文字</div>
          <div class="share-text-preview">{{ shareText }}</div>
          <el-button size="small" @click="copyText" :icon="copyTextIcon" style="margin-top: 6px">
            {{ textCopied ? '已复制' : '复制文字' }}
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UserFilled, Share, CopyDocument, Check } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getGameResultApi } from '@/api/game'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const resultData = ref(null)
const rankings = computed(() => resultData.value?.rankings || [])
const visibleRankings = ref([])
const showConfetti = ref(false)
const isSingle = computed(() => resultData.value?.gameMode === 'single')

/** 是否为本局玩家（本人刚打完）：sessionStorage 中 gameId 与 URL 一致 */
const isOwner = computed(() => {
  try {
    const raw = sessionStorage.getItem('game_result')
    if (!raw) return false
    const cached = JSON.parse(raw)
    return String(cached.gameId) === String(route.params.gameId)
  } catch {
    return false
  }
})

const myData = computed(() => rankings.value.find((r) => r.isMe))
const myRank = computed(() => rankings.value.findIndex((r) => r.isMe) + 1)

const mvpPlayer = computed(() => {
  if (!rankings.value.length) return null
  return rankings.value.reduce((best, p) => {
    const score = (p.score || 0) + (p.kills || 0) * 20 + (p.length || 0) * 2
    const bestScore = (best.score || 0) + (best.kills || 0) * 20 + (best.length || 0) * 2
    return score > bestScore ? p : best
  }, rankings.value[0])
})

function revealRankings() {
  showConfetti.value = true
  const all = rankings.value
  let i = 0
  const timer = setInterval(() => {
    if (i < all.length) {
      visibleRankings.value.push(all[i])
      i++
    } else clearInterval(timer)
  }, 400)
}

const loading = ref(false)
const loadError = ref('')

async function loadResult() {
  const gameId = route.params.gameId

  // 1. 优先使用当前标签页 sessionStorage 中的对局数据（本人刚打完）
  const raw = sessionStorage.getItem('game_result')
  if (raw) {
    try {
      const cached = JSON.parse(raw)
      // 确保缓存的 gameId 与 URL 中的一致
      if (String(cached.gameId) === String(gameId) && cached.rankings?.length) {
        resultData.value = cached
        return
      }
    } catch {
      /* noop */
    }
  }

  // 2. 从后端 API 获取真实游戏数据
  if (gameId && gameId !== 'latest') {
    loading.value = true
    try {
      const detail = await getGameResultApi(gameId)
      resultData.value = transformGameDetail(detail)
      return
    } catch (e) {
      console.error('加载游戏结果失败:', e)
      loadError.value = '游戏记录不存在或已过期'
    } finally {
      loading.value = false
    }
  }

  // 3. 没有任何数据时显示空状态
  if (!loadError.value) {
    loadError.value = '暂无结算数据'
  }
}

/**
 * 将后端 /api/games/{id} 返回格式转换为前端结算页期望格式
 * 后端: { id, gameMode, duration, players: [{ userId, score, kills, snakeLength, survivalTime, isAlive, isBot, rank, nickname }] }
 * 前端: { gameId, gameMode, duration, rankings: [{ id, nickname, score, kills, length, survivalTime, isAlive, isMe }] }
 */
function transformGameDetail(detail) {
  if (!detail) return null

  const currentId = String(userStore.userInfo.id || '')
  const rankings = (detail.players || []).map((p) => ({
    id: String(p.userId || ''),
    nickname: p.nickname || p.userId || '?',
    score: p.score || 0,
    kills: p.kills || 0,
    length: p.snakeLength || 0,
    survivalTime: p.survivalTime || 0,
    isAlive: p.isAlive !== false,
    isMe: String(p.userId) === currentId,
  }))

  return {
    gameId: String(detail.id || ''),
    gameMode: detail.gameMode || 'multi',
    duration: detail.duration || 0,
    rankings,
  }
}

function formatSurvival(s) {
  const m = Math.floor(s / 60)
  const sec = Math.floor(s % 60)
  return `${m}:${sec.toString().padStart(2, '0')}`
}

function getParticleStyle(i) {
  return {
    left: `${Math.random() * 100}%`,
    animationDelay: `${Math.random() * 2}s`,
    animationDuration: `${2 + Math.random() * 3}s`,
    backgroundColor: ['#66bb6a', '#a5d6a7', '#81c784', '#ffa726', '#ef5350'][i % 5],
  }
}

function playAgain() {
  router.push('/lobby')
}

// ---- 分享功能 ----
const showShareDialog = ref(false)
const linkCopied = ref(false)
const textCopied = ref(false)
const copyLinkIcon = computed(() => (linkCopied.value ? Check : CopyDocument))
const copyTextIcon = computed(() => (textCopied.value ? Check : CopyDocument))

/** 分享链接 */
const shareLink = computed(() => {
  const gameId = resultData.value?.gameId || 'latest'
  return `${window.location.origin}/result/${gameId}`
})

/** 分享文字 */
const shareText = computed(() => {
  const mode = isSingle.value ? '单人无尽' : '多人对战'
  const my = myData.value
  if (!my) return `我刚刚在贪吃蛇大作战中完成了一局${mode}！快来挑战我吧！`
  const parts = [`我刚刚在贪吃蛇大作战（${mode}）中`]
  if (!isSingle.value) parts.push(`获得了第 ${myRank.value} 名！`)
  parts.push(`得分 ${my.score} 分`)
  if (!isSingle.value) parts.push(`⚔ ${my.kills || 0} 击杀`)
  parts.push(`🐍 长度 ${my.length || 0}`)
  parts.push('快来挑战我吧！')
  return parts.join(' · ')
})

function openShareDialog() {
  linkCopied.value = false
  textCopied.value = false
  showShareDialog.value = true
}

async function copyLink() {
  try {
    await navigator.clipboard.writeText(shareLink.value)
    linkCopied.value = true
    ElMessage.success('链接已复制到剪贴板！')
    setTimeout(() => {
      linkCopied.value = false
    }, 3000)
  } catch (e) {
    // 回退方案：使用传统方法
    fallbackCopy(shareLink.value, () => {
      linkCopied.value = true
      ElMessage.success('链接已复制到剪贴板！')
      setTimeout(() => {
        linkCopied.value = false
      }, 3000)
    })
  }
}

async function copyText() {
  try {
    await navigator.clipboard.writeText(shareText.value)
    textCopied.value = true
    ElMessage.success('战绩文字已复制！')
    setTimeout(() => {
      textCopied.value = false
    }, 3000)
  } catch (e) {
    fallbackCopy(shareText.value, () => {
      textCopied.value = true
      ElMessage.success('战绩文字已复制！')
      setTimeout(() => {
        textCopied.value = false
      }, 3000)
    })
  }
}

/** 回退复制方案（兼容非 HTTPS 或不支持 clipboard API 的环境） */
function fallbackCopy(text, callback) {
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.style.position = 'fixed'
  textarea.style.left = '-9999px'
  textarea.style.top = '-9999px'
  document.body.appendChild(textarea)
  textarea.focus()
  textarea.select()
  try {
    document.execCommand('copy')
    callback?.()
  } catch (e) {
    ElMessage.error('复制失败，请手动复制')
  }
  document.body.removeChild(textarea)
}

onMounted(() => {
  loadResult()
  setTimeout(() => revealRankings(), 500)
})
</script>

<style scoped>
.result-container {
  width: 100%;
  height: 100vh;
  background: linear-gradient(135deg, #e8f5e9, #f1f8e9, #dcedc8, #f5f9f0);
  overflow-y: auto;
  padding: 20px;
  position: relative;
  display: flex;
  justify-content: center;
  /* 确保内容超长时可以滚动 */
  -webkit-overflow-scrolling: touch;
}
.confetti {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 1;
  overflow: hidden;
}
.particle {
  position: absolute;
  width: 8px;
  height: 8px;
  border-radius: 3px;
  top: -10px;
  animation: fall linear infinite;
}
@keyframes fall {
  to {
    transform: translateY(100vh) rotate(720deg);
    opacity: 0;
  }
}

.result-card {
  background: #fff;
  border: 1px solid #dcedc8;
  border-radius: 18px;
  padding: 28px 36px;
  max-width: 700px;
  width: 100%;
  position: relative;
  z-index: 2;
  box-shadow: 0 12px 40px rgba(46, 59, 46, 0.08);
  margin: 20px auto;
}

.result-header {
  text-align: center;
  margin-bottom: 16px;
}
.result-header h1 {
  font-size: 24px;
  color: #43a047;
  margin: 0;
}
.game-id {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
}

.champion-area {
  text-align: center;
  padding: 18px;
  background: #f1f8e9;
  border: 1px solid #c8e6c9;
  border-radius: 14px;
  margin-bottom: 16px;
}
.champion-crown {
  font-size: 32px;
}
.champion-avatar {
  margin: 6px 0;
  border: 3px solid #66bb6a;
}
.champion-name {
  font-size: 20px;
  color: #2e7d32;
  margin: 6px 0 2px;
}
.champion-score {
  font-size: 16px;
  color: var(--text-primary);
}

/* 单人模式 */
.single-result {
  text-align: center;
  padding: 20px;
}
.single-big-score {
  font-size: 56px;
  font-weight: 900;
  color: #43a047;
}
.single-label {
  font-size: 16px;
  color: var(--text-muted);
  margin-bottom: 16px;
}
.single-stats {
  display: flex;
  justify-content: center;
  gap: 24px;
  font-size: 14px;
  color: var(--text-secondary);
}

/* 排名列表 */
.rankings-list {
  margin-bottom: 16px;
}
.rankings-list h3 {
  color: var(--text-secondary);
  font-size: 14px;
  margin-bottom: 8px;
}
.ranking-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 10px;
  margin-bottom: 4px;
  background: #fafdf7;
  transition: background 0.3s;
}
.ranking-item.is-me {
  background: #e8f5e9;
  border: 1px solid #a5d6a7;
}
.ranking-item.is-champion {
  background: #f1f8e9;
}
.ranking-item.is-mvp {
  border-left: 3px solid #ffa726;
}
.rank-num {
  width: 36px;
  font-size: 16px;
  text-align: center;
}
.player-col {
  flex: 1;
  min-width: 0;
}
.p-name {
  font-weight: 600;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 6px;
}
.p-stats {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}
.p-score {
  font-size: 16px;
  font-weight: 700;
  color: #43a047;
  min-width: 60px;
  text-align: right;
}

.personal-card {
  background: #f1f8e9;
  border: 1px solid #c8e6c9;
  border-radius: 12px;
  padding: 14px 18px;
  margin-bottom: 20px;
}
.personal-card h4 {
  color: #43a047;
  margin: 0 0 10px;
  font-size: 14px;
}
.data-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 10px;
}
.data-item {
  text-align: center;
  background: #fff;
  padding: 10px 6px;
  border-radius: 8px;
  border: 1px solid #dcedc8;
}
.data-val {
  display: block;
  font-size: 20px;
  font-weight: 700;
  color: #43a047;
}
.data-label {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 2px;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 16px;
  flex-wrap: wrap;
  margin-top: 4px;
}

.shared-notice {
  text-align: center;
  margin-top: 12px;
  font-size: 12px;
  color: var(--text-muted);
}

.rank-reveal-enter-active {
  animation: slideRight 0.4s ease-out;
}
@keyframes slideRight {
  from {
    transform: translateX(-30px);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

.no-data {
  position: relative;
  z-index: 2;
}

/* ---- 分享弹窗 ---- */
.share-content {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.share-preview {
  background: linear-gradient(135deg, #e8f5e9, #f1f8e9);
  border: 1px solid #c8e6c9;
  border-radius: 12px;
  padding: 16px;
  text-align: center;
}
.share-game-mode {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 6px;
}
.share-my-line {
  font-size: 18px;
  font-weight: 700;
  color: var(--primary-dark);
  margin-bottom: 4px;
}
.share-my-line strong {
  color: #2e7d32;
}
.share-game-id {
  font-size: 11px;
  color: var(--text-muted);
}
.share-action {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  padding: 14px;
}
.share-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}
.share-input-row {
  display: flex;
  gap: 8px;
}
.share-link-input {
  flex: 1;
}
.share-text-preview {
  background: var(--bg-main);
  border: 1px solid var(--border-light);
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
  word-break: break-all;
}
</style>
