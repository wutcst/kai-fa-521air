/**
 * useSnake 组合式函数
 * Canvas 2D 完整渲染：蛇(多条/标签/死亡粒子)、食物、道具、障碍物、小地图
 * 支持插值渲染，减少网络延迟抖动
 */
import { ref } from 'vue'

// ==================== 颜色常量 ====================
const COLORS = {
  bg: '#e8f5e9',
  grid: '#f1f8e9',
  gridLine: '#dcedc8',
  border: '#a5d6a7',
  foodNormal: '#ff5252',
  foodHigh: '#ffd740',
  foodHighGlow: 'rgba(255,215,64,0.3)',
  obstacle: '#9eae9e',
  minimapBg: 'rgba(255,255,255,0.7)',
  minimapMyDot: '#43a047',
  minimapDot: '#66bb6a',
  minimapBorder: '#c8e6c9',
}

// 道具配色
const ITEM_CONFIG = {
  speed: { color: '#448aff', symbol: '⚡', label: '加速' },
  shield: { color: '#e040fb', symbol: '🛡', label: '护盾' },
  magnet: { color: '#ff6e40', symbol: '🧲', label: '磁铁' },
}

/**
 * @param {import('vue').Ref<HTMLCanvasElement|null>} canvasRef Canvas元素引用
 */
export function useSnake(canvasRef) {
  let ctx = null
  let prevState = null // 上一帧状态（用于插值）
  const interpolation = 0.5 // 插值因子

  // 画布尺寸
  const canvasWidth = ref(800)
  const canvasHeight = ref(800)

  // ---- 初始化 ----
  function initContext(mapW, mapH, gs) {
    const canvas = canvasRef?.value || canvasRef
    if (!canvas) return false
    ctx = canvas.getContext('2d')
    canvas.width = mapW * gs
    canvas.height = mapH * gs
    canvasWidth.value = canvas.width
    canvasHeight.value = canvas.height
    return true
  }

  // ==================== 渲染主函数 ====================
  /**
   * 渲染完整游戏画面
   * @param {object} state 游戏状态
   * @param {boolean} smooth 是否启用插值平滑
   */
  function render(state, smooth = true) {
    if (!ctx) return

    // 插值（如果有前一帧）
    let renderState = state
    if (smooth && prevState) {
      renderState = interpolateState(prevState, state, interpolation)
    }
    prevState = { ...state, snakes: { ...state.snakes } }

    const { snakes, foods, items, obstacles, gridSize } = renderState

    // 1. 清屏 + 背景
    ctx.fillStyle = COLORS.bg
    ctx.fillRect(0, 0, ctx.canvas.width, ctx.canvas.height)

    // 2. 网格
    drawGrid(gridSize)

    // 3. 边界
    ctx.strokeStyle = COLORS.border
    ctx.lineWidth = 3
    ctx.strokeRect(0, 0, ctx.canvas.width, ctx.canvas.height)

    // 4. 障碍物
    if (obstacles?.length) drawObstacles(obstacles, gridSize)

    // 5. 食物
    if (foods?.length) drawFoods(foods, gridSize)

    // 6. 道具
    if (items?.length) drawItems(items, gridSize)

    // 7. 蛇（含死亡效果）
    if (snakes) drawAllSnakes(snakes, gridSize)

    // 8. 小地图
    if (snakes) drawMinimap(snakes, foods, renderState)
  }

  // ---- 状态插值 ----
  function interpolateState(prev, next, t) {
    if (!prev?.snakes || !next?.snakes) return next

    const interpSnakes = {}
    for (const id of Object.keys(next.snakes || {})) {
      const ps = prev.snakes?.[id]
      const ns = next.snakes[id]
      if (!ps || !ns) {
        interpSnakes[id] = ns
        continue
      }

      // 对蛇身做线性插值
      const body = ns.body.map((seg, i) => {
        const prevSeg = ps.body?.[i]
        if (!prevSeg) return seg
        return {
          x: prevSeg.x + (seg.x - prevSeg.x) * t,
          y: prevSeg.y + (seg.y - prevSeg.y) * t,
        }
      })

      interpSnakes[id] = { ...ns, body }
    }
    return { ...next, snakes: interpSnakes }
  }

  // ==================== 网格背景 ====================
  function drawGrid(gs = 20) {
    const w = ctx.canvas.width
    const h = ctx.canvas.height

    ctx.fillStyle = COLORS.grid
    ctx.fillRect(0, 0, w, h)

    ctx.strokeStyle = COLORS.gridLine
    ctx.lineWidth = 0.5
    for (let x = 0; x <= w; x += gs) {
      ctx.beginPath()
      ctx.moveTo(x + 0.5, 0)
      ctx.lineTo(x + 0.5, h)
      ctx.stroke()
    }
    for (let y = 0; y <= h; y += gs) {
      ctx.beginPath()
      ctx.moveTo(0, y + 0.5)
      ctx.lineTo(w, y + 0.5)
      ctx.stroke()
    }
  }

  // ==================== 障碍物 ====================
  function drawObstacles(obstacles, gs) {
    for (const obs of obstacles) {
      ctx.fillStyle = COLORS.obstacle
      ctx.fillRect(obs.x * gs + 2, obs.y * gs + 2, gs - 4, gs - 4)

      // 砖纹理
      ctx.strokeStyle = '#444466'
      ctx.lineWidth = 1
      ctx.strokeRect(obs.x * gs + 3, obs.y * gs + 3, gs - 6, gs - 6)
    }
  }

  // ==================== 食物 ====================
  function drawFoods(foods, gs) {
    for (const food of foods) {
      const cx = food.x * gs + gs / 2
      const cy = food.y * gs + gs / 2

      if (food.type === 'high') {
        // 光晕
        ctx.fillStyle = COLORS.foodHighGlow
        ctx.beginPath()
        ctx.arc(cx, cy, gs * 0.45, 0, Math.PI * 2)
        ctx.fill()
        // 主体
        const gradient = ctx.createRadialGradient(cx, cy, 0, cx, cy, gs * 0.35)
        gradient.addColorStop(0, '#ffffff')
        gradient.addColorStop(0.6, COLORS.foodHigh)
        gradient.addColorStop(1, '#b8860b')
        ctx.fillStyle = gradient
        ctx.beginPath()
        ctx.arc(cx, cy, gs * 0.35, 0, Math.PI * 2)
        ctx.fill()
      } else {
        // 普通食物：径向渐变圆
        const gradient = ctx.createRadialGradient(cx - 1, cy - 1, 0, cx, cy, gs * 0.28)
        gradient.addColorStop(0, '#ff8a80')
        gradient.addColorStop(1, COLORS.foodNormal)
        ctx.fillStyle = gradient
        ctx.beginPath()
        ctx.arc(cx, cy, gs * 0.28, 0, Math.PI * 2)
        ctx.fill()
      }
    }
  }

  // ==================== 道具 ====================
  function drawItems(items, gs) {
    for (const item of items) {
      const config = ITEM_CONFIG[item.type] || ITEM_CONFIG.speed
      const cx = item.x * gs + gs / 2
      const cy = item.y * gs + gs / 2
      const half = gs * 0.4

      // 旋转菱形
      const time = Date.now() / 1000
      const rotation = Math.sin(time * 2) * 0.1

      ctx.save()
      ctx.translate(cx, cy)
      ctx.rotate(rotation)

      // 光晕
      ctx.shadowColor = config.color
      ctx.shadowBlur = 6
      ctx.fillStyle = config.color + '99'
      ctx.beginPath()
      ctx.moveTo(0, -half)
      ctx.lineTo(half, 0)
      ctx.lineTo(0, half)
      ctx.lineTo(-half, 0)
      ctx.closePath()
      ctx.fill()
      ctx.shadowBlur = 0

      // 图标
      ctx.fillStyle = '#ffffff'
      ctx.font = `${gs * 0.5}px sans-serif`
      ctx.textAlign = 'center'
      ctx.textBaseline = 'middle'
      ctx.fillText(config.symbol, 0, 1)
      ctx.restore()
    }
  }

  // ==================== 所有蛇绘制 ====================
  function drawAllSnakes(snakes, gs) {
    // 死亡蛇先画，活蛇后画（活蛇在上层）
    const sorted = Object.entries(snakes).sort(([, a], [, b]) => {
      if (a.isAlive && !b.isAlive) return 1
      if (!a.isAlive && b.isAlive) return -1
      return a.isMe ? 1 : -1
    })

    for (const [, snake] of sorted) {
      if (snake.isAlive) {
        drawSnake(snake, gs)
        drawSnakeLabel(snake, gs)
      } else {
        drawDeadSnake(snake, gs)
      }
    }
  }

  // ---- 活蛇渲染 ----
  function drawSnake(snake, gs) {
    const { body, color, direction, shield, speedBoost } = snake
    if (!body?.length) return

    // 护盾光环
    if (shield) {
      const head = body[0]
      ctx.strokeStyle = '#e040fb'
      ctx.lineWidth = 3
      ctx.shadowColor = '#e040fb'
      ctx.shadowBlur = 10
      ctx.beginPath()
      ctx.arc(head.x * gs + gs / 2, head.y * gs + gs / 2, gs * 1.2, 0, Math.PI * 2)
      ctx.stroke()
      ctx.shadowBlur = 0
    }

    // 加速拖影
    if (speedBoost > 0) {
      const last = body[body.length - 1]
      ctx.fillStyle = color + '33'
      ctx.fillRect(last.x * gs + 4, last.y * gs + 4, gs - 8, gs - 8)
      if (body.length > 1) {
        const second = body[body.length - 2]
        ctx.fillRect(second.x * gs + 6, second.y * gs + 6, gs - 12, gs - 12)
      }
    }

    // 蛇身绘制（从尾到头）
    for (let i = body.length - 1; i >= 0; i--) {
      const seg = body[i]
      const x = seg.x * gs
      const y = seg.y * gs
      const isHead = i === 0

      if (isHead) {
        // 蛇头
        // 身体颜色暗化渐变作为基色
        const headGrad = ctx.createLinearGradient(x, y, x + gs, y + gs)
        const headColor = lightenColor(color, 20)
        headGrad.addColorStop(0, headColor)
        headGrad.addColorStop(1, color)
        ctx.fillStyle = headGrad

        // 圆角正方形（蛇头）
        const radius = gs * 0.3
        roundRect(x + 1, y + 1, gs - 2, gs - 2, radius)
        ctx.fill()

        // 蛇头边框
        ctx.strokeStyle = '#ffffff44'
        ctx.lineWidth = 1.5
        roundRect(x + 1, y + 1, gs - 2, gs - 2, radius)
        ctx.stroke()

        // 如果是自己的蛇，额外高亮
        if (snake.isMe) {
          ctx.strokeStyle = '#ffffff'
          ctx.lineWidth = 2
          roundRect(x + 0.5, y + 0.5, gs - 1, gs - 1, radius)
          ctx.stroke()
        }

        // 眼睛
        drawEyes(x, y, gs, direction)
      } else if (i === 1) {
        // 颈部，颜色稍亮
        ctx.fillStyle = lightenColor(color, 10)
        ctx.fillRect(x + 2, y + 2, gs - 4, gs - 4)
      } else {
        // 身体
        const alpha = 1 - (i / body.length) * 0.3
        ctx.globalAlpha = alpha
        const altColor = i % 2 === 0 ? color : lightenColor(color, -10)
        ctx.fillStyle = altColor
        ctx.fillRect(x + 2, y + 2, gs - 4, gs - 4)

        // 鳞片纹理
        if (i % 3 === 0) {
          ctx.strokeStyle = '#ffffff11'
          ctx.lineWidth = 0.5
          ctx.beginPath()
          ctx.moveTo(x + 3, y + gs / 2)
          ctx.lineTo(x + gs - 3, y + gs / 2)
          ctx.stroke()
        }
      }
    }

    ctx.globalAlpha = 1
  }

  // ---- 蛇头眼睛 ----
  function drawEyes(x, y, gs, direction) {
    const eyeSize = gs * 0.22
    const eyeOffset = gs * 0.22
    let positions

    switch (direction) {
      case 'up':
        positions = [
          { ex: x + eyeOffset, ey: y + eyeOffset },
          { ex: x + gs - eyeOffset - eyeSize, ey: y + eyeOffset },
        ]
        break
      case 'down':
        positions = [
          { ex: x + eyeOffset, ey: y + gs - eyeOffset - eyeSize },
          { ex: x + gs - eyeOffset - eyeSize, ey: y + gs - eyeOffset - eyeSize },
        ]
        break
      case 'left':
        positions = [
          { ex: x + eyeOffset, ey: y + eyeOffset },
          { ex: x + eyeOffset, ey: y + gs - eyeOffset - eyeSize },
        ]
        break
      default: // right
        positions = [
          { ex: x + gs - eyeOffset - eyeSize, ey: y + eyeOffset },
          { ex: x + gs - eyeOffset - eyeSize, ey: y + gs - eyeOffset - eyeSize },
        ]
    }

    for (const pos of positions) {
      ctx.fillStyle = '#ffffff'
      ctx.beginPath()
      ctx.arc(pos.ex + eyeSize / 2, pos.ey + eyeSize / 2, eyeSize / 2, 0, Math.PI * 2)
      ctx.fill()

      // 瞳孔
      ctx.fillStyle = '#000000'
      ctx.beginPath()
      ctx.arc(pos.ex + eyeSize / 2, pos.ey + eyeSize / 2, eyeSize / 4, 0, Math.PI * 2)
      ctx.fill()
    }
  }

  // ---- 昵称标签 ----
  function drawSnakeLabel(snake, gs) {
    const head = snake.body[0]
    if (!head) return

    const text = snake.nickname || ''
    const x = head.x * gs + gs / 2
    const y = head.y * gs - 4

    // 背景
    ctx.font = 'bold 11px sans-serif'
    const metrics = ctx.measureText(text)
    const tw = metrics.width
    const th = 16

    ctx.fillStyle = snake.isMe ? 'rgba(0,230,118,0.85)' : 'rgba(0,0,0,0.7)'
    const rx = x - tw / 2 - 4
    const ry = y - th
    ctx.beginPath()
    ctx.moveTo(rx + 4, ry)
    ctx.lineTo(rx + tw + 8, ry)
    ctx.lineTo(rx + tw + 8, ry + th)
    ctx.lineTo(rx + 4, ry + th)
    ctx.arcTo(rx, ry + th, rx, ry, 4)
    ctx.arcTo(rx, ry, rx + 4, ry, 4)
    ctx.closePath()
    ctx.fill()

    // 文字
    ctx.fillStyle = '#ffffff'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(text, x, ry + th / 2)
  }

  // ---- 死亡蛇渲染 ----
  function drawDeadSnake(snake, gs) {
    const { body } = snake
    if (!body?.length) return

    // 死亡粒子效果（基于时间的闪烁）
    const elapsed = (Date.now() / 100) % 2
    const fadeAlpha = Math.max(0, 0.4 - elapsed * 0.15)

    for (const seg of body) {
      ctx.fillStyle =
        '#ff5252' +
        Math.floor(fadeAlpha * 255)
          .toString(16)
          .padStart(2, '0')
      ctx.fillRect(seg.x * gs + 4, seg.y * gs + 4, gs - 8, gs - 8)
    }

    // 粒子散射
    if (fadeAlpha > 0.05) {
      for (let i = 0; i < body.length; i++) {
        const seg = body[i]
        const px = seg.x * gs + gs / 2 + Math.sin(elapsed * 5 + i) * 8
        const py = seg.y * gs + gs / 2 + Math.cos(elapsed * 4 + i) * 8

        ctx.fillStyle = `rgba(255,100,100,${fadeAlpha * 0.6})`
        ctx.beginPath()
        ctx.arc(px, py, 2, 0, Math.PI * 2)
        ctx.fill()
      }
    }
  }

  // ==================== 小地图 ====================
  function drawMinimap(snakes, foods, state) {
    const w = ctx.canvas.width
    const h = ctx.canvas.height
    const mmW = 150
    const mmH = 150
    const mmX = w - mmW - 12
    const mmY = h - mmH - 12
    const scaleX = mmW / (state.mapWidth || 50)
    const scaleY = mmH / (state.mapHeight || 50)

    // 背景
    ctx.fillStyle = COLORS.minimapBg
    ctx.fillRect(mmX, mmY, mmW, mmH)
    ctx.strokeStyle = COLORS.minimapBorder
    ctx.lineWidth = 1
    ctx.strokeRect(mmX, mmY, mmW, mmH)

    // 食物小点
    if (foods) {
      for (const food of foods) {
        ctx.fillStyle = food.type === 'high' ? COLORS.foodHigh : COLORS.foodNormal
        const fx = mmX + food.x * scaleX
        const fy = mmY + food.y * scaleY
        ctx.fillRect(fx, fy, 1.5, 1.5)
      }
    }

    // 蛇小点
    if (snakes) {
      for (const [, snake] of Object.entries(snakes)) {
        if (!snake.body?.length) continue
        const head = snake.body[0]
        ctx.fillStyle = snake.isMe ? COLORS.minimapMyDot : snake.color
        const sx = mmX + head.x * scaleX - 2
        const sy = mmY + head.y * scaleY - 2
        ctx.fillRect(sx, sy, snake.isMe ? 5 : 3, snake.isMe ? 5 : 3)
      }
    }

    // 视口指示框
    // （当前显示完整地图，视口框暂时简化）
  }

  // ==================== 工具函数 ====================
  function roundRect(x, y, w, h, r) {
    ctx.beginPath()
    ctx.moveTo(x + r, y)
    ctx.lineTo(x + w - r, y)
    ctx.arcTo(x + w, y, x + w, y + r, r)
    ctx.lineTo(x + w, y + h - r)
    ctx.arcTo(x + w, y + h, x + w - r, y + h, r)
    ctx.lineTo(x + r, y + h)
    ctx.arcTo(x, y + h, x, y + h - r, r)
    ctx.lineTo(x, y + r)
    ctx.arcTo(x, y, x + r, y, r)
    ctx.closePath()
  }

  function lightenColor(hex, amount) {
    const num = parseInt(hex.replace('#', ''), 16)
    const r = Math.min(255, Math.max(0, (num >> 16) + amount))
    const g = Math.min(255, Math.max(0, ((num >> 8) & 0xff) + amount))
    const b = Math.min(255, Math.max(0, (num & 0xff) + amount))
    return `#${((r << 16) | (g << 8) | b).toString(16).padStart(6, '0')}`
  }

  // ---- 清理 ----
  function destroy() {
    ctx = null
    prevState = null
  }

  return {
    canvasWidth,
    canvasHeight,
    ctx,
    initContext,
    render,
    destroy,
  }
}
