<!--
  ChatBox.vue - 房间聊天框（v2 - 小清新主题）
-->
<template>
  <div class="chat-box" :class="{ collapsed: isCollapsed }">
    <div class="chat-header" @click="toggleCollapse">
      <div class="header-left">
        <span>💬 聊天</span>
        <span v-if="isCollapsed && unreadCount" class="unread-badge">{{ unreadCount }}</span>
      </div>
      <el-icon><ArrowUp v-if="!isCollapsed" /><ArrowDown v-else /></el-icon>
    </div>
    <div class="chat-messages" ref="msgListRef" v-show="!isCollapsed" @scroll="onScroll">
      <div v-if="!messages.length" class="empty-hint">暂无消息</div>
      <div v-for="(msg, i) in messages" :key="i" :class="['msg', msg.type]">
        <template v-if="msg.type === 'system'">
          <span class="system-msg">{{ msg.text }}</span>
        </template>
        <template v-else>
          <span class="msg-sender" :class="{ 'is-me': msg.senderId === 'me' }">{{ msg.senderName }}</span>
          <span class="msg-text">{{ msg.text }}</span>
        </template>
      </div>
      <div v-if="showScrollBtn" class="scroll-bottom-btn" @click="scrollToBottom">↓ 回到底部</div>
    </div>
    <div class="chat-input" v-show="!isCollapsed">
      <div class="quick-emoji">
        <span v-for="e in quickEmojis" :key="e" @click="sendEmoji(e)" class="emoji-btn">{{ e }}</span>
      </div>
      <div class="input-row">
        <el-input v-model="inputText" placeholder="输入消息..." size="small" @keyup.enter="sendMessage" @focus="onInputFocus" />
        <el-button @click="sendMessage" :icon="Promotion" size="small" type="primary" circle />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { ArrowUp, ArrowDown, Promotion } from '@element-plus/icons-vue'

const props = defineProps({ messages: { type: Array, default: () => [] } })
const emit = defineEmits(['send'])

const isCollapsed = ref(true)
const inputText = ref('')
const msgListRef = ref(null)
const unreadCount = ref(0)
const showScrollBtn = ref(false)
const quickEmojis = ['👍', '💪', '😄', '🎉', '😢', '👋', 'GG', '🏆']

watch(() => props.messages.length, (len, oldLen) => {
  if (isCollapsed.value && len > oldLen) unreadCount.value += len - oldLen
})

function toggleCollapse() {
  isCollapsed.value = !isCollapsed.value
  if (!isCollapsed.value) { unreadCount.value = 0; nextTick(() => scrollToBottom()) }
}
function sendMessage() {
  const text = inputText.value.trim()
  if (!text) return
  emit('send', text); inputText.value = ''
  nextTick(() => scrollToBottom())
}
function sendEmoji(e) { emit('send', e); nextTick(() => scrollToBottom()) }
function onInputFocus() { nextTick(() => scrollToBottom()) }
function scrollToBottom() {
  const el = msgListRef.value
  if (el) { el.scrollTop = el.scrollHeight; showScrollBtn.value = false }
}
function onScroll() {
  const el = msgListRef.value
  if (!el) return
  showScrollBtn.value = el.scrollHeight - el.scrollTop - el.clientHeight > 60
}
</script>

<style scoped>
.chat-box {
  background: #fff; border: 1px solid #dcedc8;
  border-radius: 10px; overflow: hidden; width: 260px; font-size: 12px;
  box-shadow: 0 2px 8px rgba(46,59,46,0.06);
}
.chat-box.collapsed { width: auto; min-width: 100px; }
.chat-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 8px 12px; cursor: pointer; color: #6b8a6b; user-select: none;
}
.chat-header:hover { background: #f1f8e9; }
.header-left { display: flex; align-items: center; gap: 8px; }
.unread-badge {
  background: #ef5350; color: #fff; font-size: 10px;
  padding: 1px 6px; border-radius: 8px; min-width: 18px; text-align: center;
}
.chat-messages { max-height: 180px; overflow-y: auto; padding: 6px 10px; position: relative; }
.empty-hint { text-align: center; color: var(--text-muted); padding: 20px 0; }
.msg { padding: 3px 0; line-height: 1.6; word-break: break-all; }
.msg.system { text-align: center; }
.system-msg {
  color: #43a047; font-size: 11px; background: #e8f5e9;
  padding: 1px 8px; border-radius: 8px; display: inline-block;
}
.msg-sender { color: #43a047; font-weight: 700; margin-right: 4px; }
.msg-sender.is-me { color: #2e7d32; }
.msg-text { color: var(--text-primary); }
.scroll-bottom-btn {
  text-align: center; color: #66bb6a; cursor: pointer; font-size: 11px;
  padding: 4px 0; background: linear-gradient(to bottom, transparent, #fff);
  position: sticky; bottom: 0;
}
.chat-input { border-top: 1px solid #dcedc8; padding: 6px 10px 10px; }
.quick-emoji { display: flex; gap: 4px; margin-bottom: 6px; }
.emoji-btn { font-size: 15px; cursor: pointer; padding: 2px 4px; border-radius: 3px; transition: transform 0.15s; }
.emoji-btn:hover { transform: scale(1.3); background: #e8f5e9; }
.input-row { display: flex; gap: 6px; }
.input-row :deep(.el-input__wrapper) {
  background: #f5f9f0 !important; border-color: #dcedc8 !important;
}
</style>
