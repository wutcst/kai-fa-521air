<!--
  CountDown.vue - 倒计时组件（占位）
  游戏开始前的倒计时展示（3-2-1-GO!）
-->
<template>
  <Teleport to="body">
    <div class="countdown-overlay" v-if="visible">
      <div class="countdown-number" :key="current" :class="{ 'animate': true }">
        <template v-if="current > 0">{{ current }}</template>
        <template v-else>GO!</template>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, watch, onUnmounted } from 'vue'

const props = defineProps({
  /** 倒计时显示（从3开始倒数，0时显示GO） */
  count: { type: Number, default: -1 }
})

const emit = defineEmits(['finish'])

const visible = ref(false)
const current = ref(0)

watch(() => props.count, (val) => {
  if (val >= 0) {
    visible.value = true
    current.value = val
    if (val === 0) {
      setTimeout(() => {
        visible.value = false
        emit('finish')
      }, 800)
    }
  } else {
    visible.value = false
  }
})
</script>

<style scoped>
.countdown-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.6);
  pointer-events: none;
}
.countdown-number {
  font-size: 120px;
  font-weight: 900;
  color: #00e676;
  text-shadow: 0 0 40px rgba(0, 230, 118, 0.5);
  animation: countPop 0.6s ease-out;
}
@keyframes countPop {
  0% { transform: scale(2); opacity: 0; }
  50% { transform: scale(0.9); opacity: 1; }
  100% { transform: scale(1); opacity: 1; }
}
</style>
