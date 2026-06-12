<!--
  GameCanvas.vue - Canvas 游戏画布（v2 - 小清新风格背景）
-->
<template>
  <div class="game-canvas-wrapper" ref="wrapperRef">
    <canvas ref="canvasRef" class="game-canvas"></canvas>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useSnake } from '@/composables/useSnake'
import { useGameLoop } from '@/composables/useGameLoop'

const props = defineProps({
  gameState: { type: Object, default: () => ({}) },
  gridSize: { type: Number, default: 20 },
  mapWidth: { type: Number, default: 50 },
  mapHeight: { type: Number, default: 50 },
  running: { type: Boolean, default: false },
})

const canvasRef = ref(null)
const wrapperRef = ref(null)
const { initContext, render, destroy } = useSnake(canvasRef)

const { start: startLoop, stop: stopLoop } = useGameLoop(() => {
  if (props.gameState?.gameStatus === 'playing' || props.gameState?.gameStatus === 'finished') {
    render(props.gameState, true)
  }
}, 60)

function fitToWindow() {
  const wrapper = wrapperRef.value
  const canvas = canvasRef.value
  if (!wrapper || !canvas) return
  const maxW = wrapper.clientWidth
  const maxH = wrapper.clientHeight
  const scale = Math.min(maxW / canvas.width, maxH / canvas.height)
  canvas.style.width = canvas.width * scale + 'px'
  canvas.style.height = canvas.height * scale + 'px'
}

watch(
  () => props.running,
  (running) => {
    if (running) startLoop()
    else stopLoop()
  },
)

onMounted(() => {
  initContext(props.mapWidth, props.mapHeight, props.gridSize)
  fitToWindow()
  window.addEventListener('resize', fitToWindow)
})

onUnmounted(() => {
  stopLoop()
  destroy()
  window.removeEventListener('resize', fitToWindow)
})

defineExpose({ canvasRef, fitToWindow })
</script>

<style scoped>
.game-canvas-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: #e8f5e9;
}
.game-canvas {
  display: block;
  image-rendering: pixelated;
  border-radius: 4px;
}
</style>
