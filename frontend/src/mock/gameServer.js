/**
 * Mock 游戏服务端
 * 模拟服务端游戏逻辑：AI蛇移动、食物生成、碰撞检测、积分计算、事件广播
 * 通过 VITE_USE_MOCK 环境变量控制开关
 *
 * 职责：
 * 1. 维护游戏状态（蛇、食物、道具、时间）
 * 2. 按固定 Tick 更新所有蛇的位置
 * 3. 模拟 AI 蛇的AI行为（简单随机移动 + 趋食倾向）
 * 4. 碰撞检测与淘汰判定
 * 5. 生成事件消息（击杀、淘汰、道具刷新）
 * 6. 回调通知前端更新渲染
 */

// ==================== 常量配置 ====================
const MAP_WIDTH = 1200        // 地图虚拟宽度
const MAP_HEIGHT = 1200       // 地图虚拟高度
const GRID_SIZE = 20          // 每格像素
const TICK_RATE = 10          // 每秒 Tick 数（服务端更新频率）
const TICK_INTERVAL = 1000 / TICK_RATE

// 颜色池（给不同蛇分配不同颜色）
const SNAKE_COLORS = [
  '#00e676', '#448aff', '#ff6e40', '#e040fb',
  '#ffd740', '#69f0ae', '#ff5252', '#40c4ff'
]

// 默认方向向量
const DIRECTIONS = {
  up:    { x: 0, y: -1 },
  down:  { x: 0, y: 1 },
  left:  { x: -1, y: 0 },
  right: { x: 1, y: 0 }
}

// ==================== Mock 游戏引擎 ====================

export class MockGameServer {
  /**
   * @param {object} options 配置项
   * @param {number} options.playerCount 玩家数量（含自己）
   * @param {number} options.gameDuration 游戏时长（秒）
   * @param {function} onStateUpdate 状态更新回调 (gameState) => {}
   * @param {function} onEvent 事件回调 (eventType, data) => {}
   * @param {string} mySnakeId 我的蛇ID
   */
  constructor({ playerCount = 4, gameDuration = 300, onStateUpdate, onEvent, mySnakeId, gameMode = 'multi' }) {
    this.playerCount = gameMode === 'single' ? 1 : playerCount
    this.gameDuration = gameMode === 'single' ? Infinity : gameDuration
    this.gameMode = gameMode       // 'single' | 'multi'
    this.onStateUpdate = onStateUpdate
    this.onEvent = onEvent
    this.mySnakeId = mySnakeId

    // 游戏状态
    this.snakes = {}           // { id: { body, direction, color, score, kills, isAlive, speedBoost, shield, magnet } }
    this.foods = []            // [{ x, y, type: 'normal'|'high' }]
    this.items = []            // [{ x, y, type: 'speed'|'shield'|'magnet' }]
    this.obstacles = []        // [{ x, y }] 障碍物
    this.gameTime = 0          // 已运行时间（秒）
    this.status = 'idle'       // idle | countdown | playing | finished
    this.tickTimer = null
    this.foodTimer = null
    this.itemTimer = null
    this.survivalTimer = null
    this.lastTick = 0

    // 蛇名列表
    this.snakeNames = ['闪电蛇', '贪吃大王', '急速先锋', '无敌蛇王', '小菜蛇', '蛇中豪杰', '末日之蛇', '疾风']
  }

  // ---- 初始化游戏 ----
  initGame() {
    // 生成障碍物（少量，约 5 个）
    this.obstacles = this._generateObstacles(5)

    // 生成蛇
    const positions = this._getSpawnPositions(this.playerCount)
    for (let i = 0; i < this.playerCount; i++) {
      const isMe = i === 0 // 第一条蛇是自己
      const id = isMe ? this.mySnakeId : 'ai_' + i
      const pos = positions[i]
      const initialDir = this._getInitialDirection(pos)
      // 初始身体：头 + 3 节身体
      const body = []
      for (let j = 0; j < 4; j++) {
        body.push({
          x: pos.x - DIRECTIONS[initialDir].x * j,
          y: pos.y - DIRECTIONS[initialDir].y * j
        })
      }

      this.snakes[id] = {
        id,
        body,
        direction: initialDir,
        nextDirection: initialDir,
        color: SNAKE_COLORS[i % SNAKE_COLORS.length],
        score: 0,
        kills: 0,
        length: body.length,
        isAlive: true,
        deathTime: 0,
        // 道具状态
        speedBoost: 0,          // 剩余加速时间（秒）
        shield: false,          // 是否有护盾
        magnet: 0,              // 磁铁剩余时间
        // 速度系统：初始 0.5，每吃一个食物 +0.02，上限 2.0
        _speed: 0.5,
        _moveAccum: 0,
        // 基础信息
        nickname: isMe ? '我' : (this.snakeNames[i] || ('AI_' + i)),
        isMe
      }
    }

    // 生成初始食物
    this.foods = this._generateFoods(20)

    // 生成道具
    this.items = this._generateItems(3)

    this.gameTime = 0
    this.status = 'playing'
    this.lastTick = performance.now()

    // 启动定时器
    this._startTimers()

    // 发送首帧
    this._broadcastState()
    this.onEvent?.('game_start', { duration: this.gameDuration })
  }

  // ---- 游戏 Tick ----
  _tick() {
    if (this.status !== 'playing') return

    const now = performance.now()
    const deltaTime = (now - this.lastTick) / 1000
    this.lastTick = now
    this.gameTime += deltaTime

    // 检查时间是否结束（仅多人模式）
    if (this.gameMode === 'multi' && this.gameTime >= this.gameDuration) {
      this._endGame()
      return
    }

    // 检查是否只剩一个活蛇（多人模式）
    if (this.gameMode === 'multi') {
      const alive = Object.values(this.snakes).filter(s => s.isAlive)
      if (alive.length <= 1) {
        if (alive.length === 1) alive[0].score += 200
        this._endGame()
        return
      }
    }

    // 更新所有活蛇
    for (const [id, snake] of Object.entries(this.snakes)) {
      if (!snake.isAlive) continue

      // AI 蛇更新方向
      if (id !== this.mySnakeId) {
        this._updateAIDirection(snake)
      }

      // 应用下一步方向
      snake.direction = snake.nextDirection

      // 计算移动速度（基础速度 0.5，吃食物加速，道具加速 1.5x）
      // 使用步长累积器，保证低速时也能平滑移动
      const baseSpeed = (snake._speed || 0.5) * (snake.speedBoost > 0 ? 1.5 : 1.0)
      snake._moveAccum = (snake._moveAccum || 0) + baseSpeed
      const steps = Math.floor(snake._moveAccum)
      snake._moveAccum -= steps

      // 移动
      for (let s = 0; s < steps; s++) {
        if (!snake.isAlive) break
        this._moveSnake(snake)
      }

      // 更新道具计时
      if (snake.speedBoost > 0) snake.speedBoost = Math.max(0, snake.speedBoost - deltaTime)
      if (snake.magnet > 0) snake.magnet = Math.max(0, snake.magnet - deltaTime)

      // 磁铁效果：吸引附近食物
      if (snake.magnet > 0) {
        this._applyMagnetEffect(snake)
      }
    }

    // 补充食物
    if (this.foods.length < 20) {
      this.foods.push(...this._generateFoods(3))
    }

    // 生存积分（每30秒）
    this.survivalTimer = (this.survivalTimer || 0) + deltaTime
    if (this.survivalTimer >= 30) {
      this.survivalTimer -= 30
      for (const snake of Object.values(this.snakes)) {
        if (snake.isAlive) snake.score += 20
      }
    }

    // 广播状态
    this._broadcastState()
  }

  // ---- 蛇移动 ----
  _moveSnake(snake) {
    const dir = DIRECTIONS[snake.direction]
    const head = snake.body[0]
    const newHead = { x: head.x + dir.x, y: head.y + dir.y }

    // 边界检测：所有模式统一穿墙循环（撞墙不死亡）
    const gridW = MAP_WIDTH / GRID_SIZE
    const gridH = MAP_HEIGHT / GRID_SIZE
    if (newHead.x < 0 || newHead.x >= gridW || newHead.y < 0 || newHead.y >= gridH) {
      newHead.x = ((newHead.x % gridW) + gridW) % gridW
      newHead.y = ((newHead.y % gridH) + gridH) % gridH
    }

    // 撞自己检测：所有模式统一，穿过自己身体不阻挡
    // 【规则】只有撞障碍物/撞其他蛇才会淘汰，自己身体可以穿过

    // 障碍物检测
    const hitObstacle = this.obstacles.some(o => o.x === newHead.x && o.y === newHead.y)
    if (hitObstacle) {
      this._eliminateSnake(snake, '撞到障碍物')
      return
    }

    // 蛇头碰撞检测
    for (const [otherId, other] of Object.entries(this.snakes)) {
      if (otherId === snake.id || !other.isAlive) continue

      // 头碰头
      if (newHead.x === other.body[0].x && newHead.y === other.body[0].y) {
        if (snake.body.length >= other.body.length) {
          // 自己更长（或相等），淘汰对方
          this._eliminateSnake(other, '头碰头落败')
          snake.score += 100
          snake.kills++
          this.onEvent?.('player_kill', { killerId: snake.id, victimId: otherId, method: '头碰头' })
          // 掉落食物
          this._spawnDeathFoods(other)
        } else {
          this._eliminateSnake(snake, '头碰头落败')
          other.score += 100
          other.kills++
          this.onEvent?.('player_kill', { killerId: otherId, victimId: snake.id, method: '头碰头' })
          this._spawnDeathFoods(snake)
          return
        }
        continue
      }

      // 头撞对方蛇身
      const hitOtherBody = other.body.slice(0).some(s => s.x === newHead.x && s.y === newHead.y)
      if (hitOtherBody) {
        if (snake.shield) {
          // 护盾抵消
          snake.shield = false
          this.onEvent?.('shield_used', { snakeId: snake.id })
          // 另一条蛇不受影响
        } else {
          this._eliminateSnake(snake, `撞到${other.nickname}身体`)
          other.score += 100
          other.kills++
          this.onEvent?.('player_kill', { killerId: otherId, victimId: snake.id, method: '撞击身体' })
          this._spawnDeathFoods(snake)
          return
        }
      }
    }

    // 吃食物检测
    for (let i = this.foods.length - 1; i >= 0; i--) {
      const food = this.foods[i]
      if (newHead.x === food.x && newHead.y === food.y) {
        // 蛇身增长（在头部前方加一节）
        snake.body.unshift(newHead)
        snake.body.unshift({ ...newHead })

        snake.score += food.type === 'high' ? 50 : 10
        this.foods.splice(i, 1)
        snake.length = snake.body.length

        // 吃食物加速（上限 2.0）
        snake._speed = Math.min(2.0, (snake._speed || 0.5) + 0.02)
        break
      }
    }

    // 吃道具检测
    for (let i = this.items.length - 1; i >= 0; i--) {
      const item = this.items[i]
      if (newHead.x === item.x && newHead.y === item.y) {
        if (item.type === 'speed') snake.speedBoost = 5
        if (item.type === 'shield') snake.shield = true
        if (item.type === 'magnet') snake.magnet = 8
        this.items.splice(i, 1)
        this.onEvent?.('item_picked', { snakeId: snake.id, itemType: item.type })
        break
      }
    }

    // 移动身体
    snake.body.unshift(newHead)
    snake.body.pop()
    snake.length = snake.body.length
  }

  // ---- AI 方向更新 ----
  _updateAIDirection(snake) {
    const dirs = ['up', 'down', 'left', 'right']
    // 排除180度反向
    const opposites = { up: 'down', down: 'up', left: 'right', right: 'left' }
    const valid = dirs.filter(d => d !== opposites[snake.direction])

    // 偶尔改变方向 (15% 概率)
    if (Math.random() < 0.15) {
      // 检查是否有附近食物（20格以内）
      let targetFood = null
      const head = snake.body[0]
      for (const food of this.foods) {
        const dist = Math.abs(food.x - head.x) + Math.abs(food.y - head.y)
        if (dist < 20 && (!targetFood || dist <
          Math.abs(targetFood.x - head.x) + Math.abs(targetFood.y - head.y))) {
          targetFood = food
        }
      }

      if (targetFood && Math.random() < 0.7) {
        // 向食物方向移动
        const dx = targetFood.x - head.x
        const dy = targetFood.y - head.y
        let preferred = ''
        if (Math.abs(dx) > Math.abs(dy)) {
          preferred = dx > 0 ? 'right' : 'left'
        } else {
          preferred = dy > 0 ? 'down' : 'up'
        }
        if (valid.includes(preferred)) {
          snake.nextDirection = preferred
          return
        }
      }

      // 随机方向
      snake.nextDirection = valid[Math.floor(Math.random() * valid.length)]
    }
  }

  // ---- 淘汰蛇 ----
  _eliminateSnake(snake, reason) {
    snake.isAlive = false
    snake.deathTime = this.gameTime
    this.onEvent?.('player_eliminated', {
      snakeId: snake.id,
      nickname: snake.nickname,
      reason,
      time: this.gameTime
    })

    // 单人模式：自己死了立即结束
    if (this.gameMode === 'single') {
      this._endGame()
      return
    }

    // 多人模式：检查是否只剩一人
    const alive = Object.values(this.snakes).filter(s => s.isAlive)
    if (alive.length <= 1) {
      if (alive.length === 1) alive[0].score += 200
      this._endGame()
    }
  }

  // ---- 死亡掉落食物 ----
  _spawnDeathFoods(snake) {
    for (const seg of snake.body) {
      if (Math.random() < 0.3) { // 30% 身体掉落食物
        this.foods.push({
          x: seg.x,
          y: seg.y,
          type: Math.random() < 0.2 ? 'high' : 'normal'
        })
      }
    }
  }

  // ==================== 地图生成 ====================
  _generateObstacles(count) {
    const obstacles = []
    const gridW = MAP_WIDTH / GRID_SIZE
    const gridH = MAP_HEIGHT / GRID_SIZE
    for (let i = 0; i < count; i++) {
      obstacles.push({
        x: Math.floor(Math.random() * (gridW - 10)) + 5,
        y: Math.floor(Math.random() * (gridH - 10)) + 5
      })
    }
    return obstacles
  }

  _generateFoods(count) {
    const foods = []
    for (let i = 0; i < count; i++) {
      foods.push({
        x: Math.floor(Math.random() * (MAP_WIDTH / GRID_SIZE - 2)) + 1,
        y: Math.floor(Math.random() * (MAP_HEIGHT / GRID_SIZE - 2)) + 1,
        type: Math.random() < 0.1 ? 'high' : 'normal' // 10% 高分食物
      })
    }
    return foods
  }

  _generateItems(count) {
    const items = []
    const types = ['speed', 'shield', 'magnet']
    for (let i = 0; i < count; i++) {
      items.push({
        x: Math.floor(Math.random() * (MAP_WIDTH / GRID_SIZE - 2)) + 1,
        y: Math.floor(Math.random() * (MAP_HEIGHT / GRID_SIZE - 2)) + 1,
        type: types[Math.floor(Math.random() * types.length)]
      })
    }
    return items
  }

  _getSpawnPositions(count) {
    const positions = []
    const margin = 8
    const gridW = MAP_WIDTH / GRID_SIZE
    const gridH = MAP_HEIGHT / GRID_SIZE
    // 四个角落 + 随机
    const corners = [
      { x: margin, y: margin },
      { x: gridW - margin, y: margin },
      { x: margin, y: gridH - margin },
      { x: gridW - margin, y: gridH - margin }
    ]
    for (let i = 0; i < Math.min(count, corners.length); i++) {
      positions.push(corners[i])
    }
    for (let i = positions.length; i < count; i++) {
      positions.push({
        x: Math.floor(Math.random() * (gridW - 2 * margin)) + margin,
        y: Math.floor(Math.random() * (gridH - 2 * margin)) + margin
      })
    }
    return positions
  }

  _getInitialDirection(pos) {
    const gridW = MAP_WIDTH / GRID_SIZE
    const gridH = MAP_HEIGHT / GRID_SIZE
    if (pos.x < gridW / 2 && pos.y < gridH / 2) return 'right'
    if (pos.x >= gridW / 2 && pos.y < gridH / 2) return 'down'
    if (pos.x < gridW / 2 && pos.y >= gridH / 2) return 'up'
    return 'left'
  }

  // ---- 磁铁效果 ----
  _applyMagnetEffect(snake) {
    const head = snake.body[0]
    const range = 8
    for (let i = this.foods.length - 1; i >= 0; i--) {
      const food = this.foods[i]
      const dist = Math.abs(food.x - head.x) + Math.abs(food.y - head.y)
      if (dist <= range && dist > 0) {
        // 食物向蛇靠近
        if (Math.abs(food.x - head.x) > Math.abs(food.y - head.y)) {
          food.x += food.x > head.x ? -1 : 1
        } else {
          food.y += food.y > head.y ? -1 : 1
        }
        // 检查是否已被吸到蛇头位置，立即吸收
        if (food.x === head.x && food.y === head.y) {
          snake.score += food.type === 'high' ? 50 : 10
          snake.body.unshift({ ...head })
          snake._speed = Math.min(2.0, (snake._speed || 0.5) + 0.02)
          snake.length = snake.body.length
          this.foods.splice(i, 1)
        }
      }
    }
  }

  // ---- 定时器 ----
  _startTimers() {
    this.tickTimer = setInterval(() => this._tick(), TICK_INTERVAL)

    // 每5秒补充道具
    this.itemTimer = setInterval(() => {
      if (this.status === 'playing' && this.items.length < 5) {
        this.items.push(...this._generateItems(1))
      }
    }, 5000)

    // 每2秒补充食物
    this.foodTimer = setInterval(() => {
      if (this.status === 'playing' && this.foods.length < 25) {
        this.foods.push(...this._generateFoods(2))
      }
    }, 2000)
  }

  _stopTimers() {
    if (this.tickTimer) { clearInterval(this.tickTimer); this.tickTimer = null }
    if (this.itemTimer) { clearInterval(this.itemTimer); this.itemTimer = null }
    if (this.foodTimer) { clearInterval(this.foodTimer); this.foodTimer = null }
  }

  // ==================== 广播与结束 ====================
  _broadcastState() {
    const snakeStates = {}
    for (const [id, snake] of Object.entries(this.snakes)) {
      snakeStates[id] = {
        id: snake.id,
        body: snake.body,
        direction: snake.direction,
        color: snake.color,
        score: snake.score,
        kills: snake.kills,
        length: snake.length,
        isAlive: snake.isAlive,
        nickname: snake.nickname,
        speedBoost: snake.speedBoost,
        shield: snake.shield,
        magnet: snake.magnet,
        isMe: snake.isMe
      }
    }

    const scoreBoard = Object.values(snakeStates)
      .map(s => ({
        id: s.id,
        nickname: s.nickname,
        score: s.score,
        kills: s.kills,
        length: s.length,
        isAlive: s.isAlive,
        isMe: s.isMe
      }))
      .sort((a, b) => b.score - a.score)

    this.onStateUpdate?.({
      snakes: snakeStates,
      foods: this.foods,
      items: this.items,
      obstacles: this.obstacles,
      scoreBoard,
      gameTime: this.gameTime,
      totalTime: this.gameDuration,
      gameStatus: this.status,
      mapWidth: MAP_WIDTH / GRID_SIZE,
      mapHeight: MAP_HEIGHT / GRID_SIZE,
      gridSize: GRID_SIZE
    })
  }

  _endGame() {
    // 防止重复调用（_eliminateSnake 和 _tick 可能同时触发结束）
    if (this.status === 'finished') return
    this.status = 'finished'
    this._stopTimers()

    // 广播最终状态
    this._broadcastState()

    // 生成结算数据
    const rankings = Object.values(this.snakes)
      .map(s => ({
        id: s.id,
        nickname: s.nickname,
        score: s.score,
        kills: s.kills,
        length: s.length,
        isAlive: s.isAlive,
        survivalTime: s.isAlive ? this.gameTime : s.deathTime,
        isMe: s.isMe,
        color: s.color
      }))
      .sort((a, b) => b.score - a.score)

    this.onEvent?.('game_over', {
      gameId: 'game_' + Date.now(),
      duration: this.gameDuration,
      rankings
    })
  }

  // ==================== 玩家输入处理 ====================
  /** 设置我的蛇方向 */
  setDirection(direction) {
    const snake = this.snakes[this.mySnakeId]
    if (!snake?.isAlive) return

    // 180度反向拦截
    const opposites = { up: 'down', down: 'up', left: 'right', right: 'left' }
    if (direction !== opposites[snake.direction]) {
      snake.nextDirection = direction
    }
  }

  /** 使用道具 */
  useItem(itemType) {
    const snake = this.snakes[this.mySnakeId]
    if (!snake?.isAlive) return

    if (itemType === 'speed') snake.speedBoost = 5
    if (itemType === 'shield') snake.shield = true
    if (itemType === 'magnet') snake.magnet = 8
  }

  /** 销毁游戏 */
  destroy() {
    this._stopTimers()
    this.snakes = {}
    this.foods = []
    this.items = []
    this.obstacles = []
  }
}

/**
 * 创建 Mock 游戏服务实例
 * 工厂函数，方便在组件中使用
 */
export function createMockGame(options) {
  return new MockGameServer(options)
}
