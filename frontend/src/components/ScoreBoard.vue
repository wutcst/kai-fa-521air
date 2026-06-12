<!--
  ScoreBoard.vue - 实时积分榜（v2 - 小清新主题）
-->
<template>
  <div class="score-board">
    <h4 class="board-title">🏅 实时积分榜</h4>
    <div class="board-list" v-if="players.length">
      <div
        v-for="(player, index) in sortedPlayers"
        :key="player.id"
        :class="[
          'board-item',
          { 'is-me': player.isMe, 'is-dead': !player.isAlive, 'top-three': index < 3 },
        ]"
      >
        <span :class="['rank', rankClass(index)]">
          <template v-if="index === 0">🥇</template>
          <template v-else-if="index === 1">🥈</template>
          <template v-else-if="index === 2">🥉</template>
          <template v-else>{{ index + 1 }}</template>
          <span
            v-if="getRankChange(player.id)"
            :class="['rank-change', getRankChange(player.id) === '↑' ? 'up' : 'down']"
          >
            {{ getRankChange(player.id) }}
          </span>
        </span>
        <div class="player-main">
          <span class="name">{{ player.nickname }}</span>
          <div class="sub-info">
            <span v-if="!player.isAlive" class="dead-tag">💀 已淘汰</span>
            <span class="kills">⚔ {{ player.kills || 0 }}</span>
            <span class="len">🐍 {{ player.length || 0 }}</span>
          </div>
        </div>
        <span class="score">{{ player.score }}</span>
      </div>
    </div>
    <el-empty v-else description="暂无数据" :image-size="40" />
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({ players: { type: Array, default: () => [] } })
const prevRankMap = ref({})

const sortedPlayers = computed(() => {
  return [...props.players].sort((a, b) => {
    if (a.isAlive && !b.isAlive) return -1
    if (!a.isAlive && b.isAlive) return 1
    return (b.score || 0) - (a.score || 0)
  })
})

function getRankChange(playerId) {
  const prev = prevRankMap.value[playerId]
  const curr = sortedPlayers.value.findIndex((p) => p.id === playerId)
  if (prev === undefined || prev === curr) return ''
  return curr < prev ? '↑' : '↓'
}

function rankClass(index) {
  if (index === 0) return 'rank-1'
  if (index === 1) return 'rank-2'
  if (index === 2) return 'rank-3'
  return ''
}

watch(
  () => props.players,
  (newVal) => {
    const sorted = [...newVal].sort((a, b) => (b.score || 0) - (a.score || 0))
    sorted.forEach((p, i) => {
      prevRankMap.value[p.id] = i
    })
  },
  { deep: true },
)
</script>

<style scoped>
.score-board {
  padding: 12px;
  height: 100%;
  display: flex;
  flex-direction: column;
}
.board-title {
  text-align: center;
  color: #43a047;
  margin: 0 0 10px 0;
  font-size: 14px;
  flex-shrink: 0;
}
.board-list {
  flex: 1;
  overflow-y: auto;
}

.board-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 8px;
  margin-bottom: 4px;
  background: #fafdf7;
  transition: all 0.3s;
  font-size: 13px;
}
.board-item:hover {
  background: #e8f5e9;
}
.board-item.is-me {
  background: #e8f5e9;
  border: 1px solid #a5d6a7;
}
.board-item.is-dead {
  opacity: 0.5;
}
.board-item.top-three {
  background: #f1f8e9;
}

.rank {
  width: 32px;
  text-align: center;
  font-weight: 700;
  font-size: 14px;
  color: var(--text-muted);
  display: flex;
  flex-direction: column;
  align-items: center;
}
.rank-1,
.rank-2,
.rank-3 {
  font-size: 18px;
}
.rank-change {
  font-size: 10px;
  line-height: 1;
}
.rank-change.up {
  color: #66bb6a;
}
.rank-change.down {
  color: #ef5350;
}

.player-main {
  flex: 1;
  min-width: 0;
}
.name {
  color: var(--text-primary);
  font-weight: 600;
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.sub-info {
  display: flex;
  gap: 6px;
  font-size: 10px;
  color: var(--text-secondary);
  margin-top: 1px;
}
.dead-tag {
  color: #ef5350;
  font-weight: 600;
}

.score {
  color: #43a047;
  font-weight: 700;
  font-size: 14px;
  font-variant-numeric: tabular-nums;
  min-width: 40px;
  text-align: right;
}
</style>
