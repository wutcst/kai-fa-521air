<!--
  PlayerList.vue - 玩家列表组件（占位）
  显示房间内玩家状态：头像、昵称、准备状态、网络状态、段位
-->
<template>
  <div class="player-list game-card">
    <h4 class="list-title">👥 玩家列表 ({{ players.length }}/{{ maxPlayers }})</h4>
    <div class="list-body">
      <el-empty v-if="!players.length" description="等待玩家加入..." :image-size="40" />
      <div
        v-for="player in players"
        :key="player.id"
        :class="['player-item', { 'is-ready': player.isReady, 'is-host': player.isHost }]"
      >
        <div class="player-avatar">
          <el-avatar :size="32" :src="player.avatar">
            {{ player.nickname?.charAt(0) || '?' }}
          </el-avatar>
        </div>
        <div class="player-info">
          <span class="player-name">
            {{ player.nickname }}
            <el-tag v-if="player.isHost" size="small" type="warning">房主</el-tag>
          </span>
          <span class="player-status">
            {{ player.isReady ? '✅ 已准备' : '⏳ 未准备' }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  players: { type: Array, default: () => [] },
  maxPlayers: { type: Number, default: 6 },
})
</script>

<style scoped>
.player-list {
  padding: 12px;
  min-width: 220px;
}
.list-title {
  text-align: center;
  color: #ffd740;
  margin: 0 0 10px 0;
  font-size: 14px;
}
.list-body {
  max-height: 400px;
  overflow-y: auto;
}
.player-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  border-radius: 6px;
  margin-bottom: 6px;
  background: rgba(255, 255, 255, 0.04);
}
.player-item.is-ready {
  background: rgba(0, 230, 118, 0.08);
}
.player-item.is-host {
  border-left: 2px solid #ffd740;
}
.player-info {
  display: flex;
  flex-direction: column;
  font-size: 13px;
}
.player-name {
  color: #c0c0d0;
  display: flex;
  align-items: center;
  gap: 6px;
}
.player-status {
  font-size: 11px;
  color: #7a7a9a;
}
</style>
