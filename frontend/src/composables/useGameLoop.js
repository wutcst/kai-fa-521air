/**
 * useGameLoop 组合式函数
 * 游戏主循环管理：使用 requestAnimationFrame 实现平滑帧率
 * 负责：帧率控制、时序计算、渲染/逻辑回调调度
 */
import { ref, onUnmounted } from 'vue'

/**
 * @param {function} onUpdate 每帧更新回调 (deltaTime: number) => void
 * @param {number} fps 目标帧率，默认60
 */
export function useGameLoop(onUpdate, fps = 60) {
  const isRunning = ref(false)
  const fpsActual = ref(0)

  let animationFrameId = null
  let lastTime = 0
  let frameCount = 0
  let fpsTimer = 0
  const frameInterval = 1000 / fps // 每帧之间的时间间隔（毫秒）
  let accumulated = 0

  // ---- 启动循环 ----
  function start() {
    if (isRunning.value) return
    isRunning.value = true
    lastTime = performance.now()
    animationFrameId = requestAnimationFrame(loop)
  }

  // ---- 停止循环 ----
  function stop() {
    isRunning.value = false
    if (animationFrameId) {
      cancelAnimationFrame(animationFrameId)
      animationFrameId = null
    }
  }

  // ---- 主循环 ----
  function loop(timestamp) {
    if (!isRunning.value) return

    const deltaTime = timestamp - lastTime
    lastTime = timestamp

    // 累积时间，按固定间隔调用更新
    accumulated += deltaTime

    // 帧率统计
    frameCount++
    fpsTimer += deltaTime
    if (fpsTimer >= 1000) {
      fpsActual.value = Math.round(frameCount * 1000 / fpsTimer)
      frameCount = 0
      fpsTimer = 0
    }

    // 使用固定时间步长，避免高频刷新导致性能问题
    while (accumulated >= frameInterval) {
      onUpdate(frameInterval / 1000) // 传入 deltaTime（秒）
      accumulated -= frameInterval
    }

    animationFrameId = requestAnimationFrame(loop)
  }

  // 组件卸载时停止
  onUnmounted(() => {
    stop()
  })

  return {
    isRunning,
    fpsActual,
    start,
    stop
  }
}
