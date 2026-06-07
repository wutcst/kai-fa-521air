<!--
  GuideView.vue - 新手引导页面
  卡片+箭头引导式布局，展示游戏玩法、模式、操作流程
-->
<template>
  <div class="guide-container">
    <div class="top-decor"></div>

    <!-- 顶部 -->
    <header class="guide-header">
      <div class="header-left">
        <el-button text @click="goBack" class="back-btn">
          <el-icon><ArrowLeft /></el-icon> 返回大厅
        </el-button>
        <span class="page-title">📖 新手引导</span>
      </div>
    </header>

    <div class="guide-body">
      <!-- 欢迎横幅 -->
      <div class="welcome-banner">
        <div class="banner-content">
          <h1>🎮 欢迎来到贪吃蛇大作战！</h1>
          <p>这是一款多人联机贪吃蛇对战游戏，你可以与全球玩家实时对抗，看看谁才是蛇中之王！</p>
        </div>
        <div class="banner-snake">🐍</div>
      </div>

      <!-- ====== 第一步：选择模式 ====== -->
      <div class="step-section">
        <div class="step-number">1</div>
        <h2 class="step-title">选择游戏模式</h2>
        <p class="step-desc">两种模式任你选择，每种都有不同的乐趣</p>

        <div class="mode-cards">
          <div class="mode-card multi">
            <div class="mode-card-icon">👥</div>
            <h3>多人对战</h3>
            <ul class="mode-features">
              <li>⚔️ 与其他玩家实时对战</li>
              <li>🛡️ 撞到其他蛇淘汰，撞墙不淘汰</li>
              <li>🏆 按得分排名，争夺冠军</li>
              <li>⏱ 固定时长，3/5/8 分钟可选</li>
              <li>🤖 支持添加 Bot 陪练</li>
            </ul>
            <el-tag type="primary" size="large" effect="dark">推荐</el-tag>
          </div>

          <div class="mode-card single">
            <div class="mode-card-icon">🧘</div>
            <h3>单人无尽</h3>
            <ul class="mode-features">
              <li>🧠 独自挑战，超越自我</li>
              <li>💥 撞到墙壁即淘汰</li>
              <li>📈 挑战最高分纪录</li>
              <li>♾ 无限时长，直到淘汰</li>
              <li>🎯 专注操作，磨练技巧</li>
            </ul>
            <el-tag type="success" size="large" effect="dark">练习</el-tag>
          </div>
        </div>
      </div>

      <!-- 箭头分隔 -->
      <div class="arrow-divider">
        <span class="arrow-icon">⬇</span>
        <span class="arrow-text">选择模式后，通过以下方式开始游戏</span>
        <span class="arrow-icon">⬇</span>
      </div>

      <!-- ====== 第二步：开始对战 ====== -->
      <div class="step-section">
        <div class="step-number">2</div>
        <h2 class="step-title">三种方式开始对战</h2>
        <p class="step-desc">挑选你喜欢的方式，快速投入战斗</p>

        <div class="flow-cards">
          <!-- 快速匹配 -->
          <div class="flow-card highlight">
            <div class="flow-card-badge">⚡ 最快</div>
            <div class="flow-card-icon-wrap">
              <span class="flow-card-icon">⚡</span>
            </div>
            <h4>快速匹配</h4>
            <p>自动匹配旗鼓相当的对手，1v1 对战</p>
            <div class="flow-steps">
              <span class="flow-step">点击匹配</span>
              <span class="flow-arrow">→</span>
              <span class="flow-step">自动配对</span>
              <span class="flow-arrow">→</span>
              <span class="flow-step">进入房间</span>
              <span class="flow-arrow">→</span>
              <span class="flow-step">开始对战</span>
            </div>
          </div>

          <!-- 创建房间 -->
          <div class="flow-card">
            <div class="flow-card-icon-wrap">
              <span class="flow-card-icon">🏠</span>
            </div>
            <h4>创建房间</h4>
            <p>自定义规则，邀请好友或等待路人加入</p>
            <div class="flow-steps">
              <span class="flow-step">设置参数</span>
              <span class="flow-arrow">→</span>
              <span class="flow-step">创建房间</span>
              <span class="flow-arrow">→</span>
              <span class="flow-step">等待玩家</span>
              <span class="flow-arrow">→</span>
              <span class="flow-step">开始游戏</span>
            </div>
          </div>

          <!-- 加入房间 -->
          <div class="flow-card">
            <div class="flow-card-icon-wrap">
              <span class="flow-card-icon">🚪</span>
            </div>
            <h4>加入房间</h4>
            <p>浏览大厅房间列表，选择心仪的房间加入</p>
            <div class="flow-steps">
              <span class="flow-step">浏览列表</span>
              <span class="flow-arrow">→</span>
              <span class="flow-step">选择房间</span>
              <span class="flow-arrow">→</span>
              <span class="flow-step">加入房间</span>
              <span class="flow-arrow">→</span>
              <span class="flow-step">准备开战</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 箭头分隔 -->
      <div class="arrow-divider">
        <span class="arrow-icon">⬇</span>
        <span class="arrow-text">对战后，查看你的战绩和排名</span>
        <span class="arrow-icon">⬇</span>
      </div>

      <!-- ====== 第三步：查看数据 ====== -->
      <div class="step-section">
        <div class="step-number">3</div>
        <h2 class="step-title">查看你的数据</h2>
        <p class="step-desc">全面追踪你的游戏表现</p>

        <div class="feature-cards">
          <div class="feature-card">
            <div class="feature-icon">📋</div>
            <h4>对战记录</h4>
            <p>查看每一场对战的详细数据：排名、得分、击杀、存活时间</p>
            <el-tag size="small" type="primary" effect="plain">大厅 → 对战记录</el-tag>
          </div>

          <div class="feature-card">
            <div class="feature-icon">🏆</div>
            <h4>排行榜</h4>
            <p>总榜 / 多人榜 / 单人榜，看看你在全服排第几</p>
            <el-tag size="small" type="warning" effect="plain">大厅 → 排行榜</el-tag>
          </div>

          <div class="feature-card">
            <div class="feature-icon">📊</div>
            <h4>个人统计</h4>
            <p>总场次、胜率、最高分、场均排名一目了然</p>
            <el-tag size="small" type="success" effect="plain">对战记录页顶部</el-tag>
          </div>
        </div>
      </div>

      <!-- ====== 第四步：操作技巧 ====== -->
      <div class="step-section tips-section">
        <div class="step-number">💡</div>
        <h2 class="step-title">操作与技巧</h2>

        <div class="tips-grid">
          <div class="tip-card">
            <span class="tip-icon">⌨️</span>
            <div class="tip-content">
              <h5>键盘控制</h5>
              <p>使用 <kbd>↑</kbd> <kbd>↓</kbd> <kbd>←</kbd> <kbd>→</kbd> 或 <kbd>W</kbd><kbd>A</kbd><kbd>S</kbd><kbd>D</kbd> 控制蛇的移动方向</p>
            </div>
          </div>
          <div class="tip-card">
            <span class="tip-icon">🎯</span>
            <div class="tip-content">
              <h5>包围战术</h5>
              <p>利用身体围住对手，限制其移动空间，逼迫对手撞到你</p>
            </div>
          </div>
          <div class="tip-card">
            <span class="tip-icon">🍎</span>
            <div class="tip-content">
              <h5>吃食物成长</h5>
              <p>地图上会随机刷新食物，吃掉它们让你的蛇变长，得分更高</p>
            </div>
          </div>
          <div class="tip-card">
            <span class="tip-icon">⚡</span>
            <div class="tip-content">
              <h5>加速冲刺</h5>
              <p>按住 <kbd>空格键</kbd> 可以加速移动，但会消耗身体长度，谨慎使用</p>
            </div>
          </div>
          <div class="tip-card">
            <span class="tip-icon">💬</span>
            <div class="tip-content">
              <h5>房间聊天</h5>
              <p>在房间等待时，可以和对手聊天互动，交流战术</p>
            </div>
          </div>
          <div class="tip-card">
            <span class="tip-icon">🤖</span>
            <div class="tip-content">
              <h5>Bot 陪练</h5>
              <p>创建房间时可以开启 Bot，用 AI 对手练习技术</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部按钮 -->
      <div class="guide-footer">
        <el-button size="large" @click="goBack" :icon="ArrowLeft">返回大厅</el-button>
        <el-button type="primary" size="large" @click="startGame">⚡ 开始对战</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'

const router = useRouter()

function goBack() {
  router.push('/lobby')
}

function startGame() {
  router.push('/lobby')
}
</script>

<style scoped>
.guide-container {
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

.guide-header {
  display: flex;
  align-items: center;
  padding: 12px 24px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-color);
  flex-shrink: 0;
}
.header-left { display: flex; align-items: center; gap: 16px; }
.back-btn { font-size: 14px; color: var(--primary-dark); }
.page-title { font-size: 20px; font-weight: 700; color: var(--text-primary); }

.guide-body {
  flex: 1;
  overflow-y: auto;
  padding: 0 24px 40px;
}

/* 欢迎横幅 */
.welcome-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #e8f5e9, #c8e6c9, #a5d6a7);
  border-radius: 16px;
  padding: 28px 36px;
  margin: 20px 0;
  border: 1px solid #81c784;
}
.banner-content h1 {
  font-size: 22px;
  color: #2e7d32;
  margin: 0 0 8px;
}
.banner-content p {
  font-size: 14px;
  color: #33691e;
  margin: 0;
  line-height: 1.6;
  max-width: 520px;
}
.banner-snake {
  font-size: 64px;
  animation: snake-wiggle 1.5s ease-in-out infinite;
}
@keyframes snake-wiggle {
  0%, 100% { transform: rotate(-3deg); }
  50% { transform: rotate(3deg); }
}

/* 步骤区域 */
.step-section {
  margin: 28px 0;
}
.step-number {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--primary-color);
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 8px;
}
.step-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 4px;
}
.step-desc {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0 0 16px;
}

/* 模式卡片 */
.mode-cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.mode-card {
  background: var(--bg-card);
  border: 2px solid var(--border-color);
  border-radius: 14px;
  padding: 24px;
  position: relative;
  transition: transform 0.2s, box-shadow 0.2s;
}
.mode-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}
.mode-card.multi { border-color: #90caf9; }
.mode-card.single { border-color: #a5d6a7; }
.mode-card-icon { font-size: 40px; margin-bottom: 8px; }
.mode-card h3 { font-size: 18px; font-weight: 700; margin: 0 0 12px; color: var(--text-primary); }
.mode-card .el-tag { position: absolute; top: 20px; right: 20px; }
.mode-features {
  list-style: none;
  padding: 0;
  margin: 0;
}
.mode-features li {
  font-size: 13px;
  color: var(--text-secondary);
  padding: 5px 0;
  line-height: 1.5;
}

/* 箭头分隔 */
.arrow-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 16px 0;
}
.arrow-icon {
  font-size: 20px;
  color: var(--primary-color);
  animation: arrow-bounce 1.5s ease-in-out infinite;
}
@keyframes arrow-bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(6px); }
}
.arrow-text {
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 500;
}

/* 流程卡片 */
.flow-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.flow-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 14px;
  padding: 22px;
  text-align: center;
  position: relative;
  transition: transform 0.2s, box-shadow 0.2s;
}
.flow-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-md);
}
.flow-card.highlight {
  border-color: #ffd54f;
  background: linear-gradient(180deg, #fffde7, #fff);
}
.flow-card-badge {
  position: absolute;
  top: -10px;
  left: 50%;
  transform: translateX(-50%);
  background: #ff9800;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  padding: 3px 12px;
  border-radius: 10px;
  white-space: nowrap;
}
.flow-card-icon-wrap {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--bg-hover);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 10px;
}
.flow-card-icon { font-size: 28px; }
.flow-card h4 { font-size: 16px; font-weight: 700; margin: 0 0 6px; color: var(--text-primary); }
.flow-card p { font-size: 12px; color: var(--text-muted); margin: 0 0 14px; }
.flow-steps {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 4px;
}
.flow-step {
  font-size: 12px;
  font-weight: 600;
  color: var(--primary-dark);
  background: var(--bg-hover);
  padding: 4px 10px;
  border-radius: 6px;
}
.flow-arrow {
  font-size: 14px;
  color: var(--primary-color);
  font-weight: 700;
}

/* 功能卡片 */
.feature-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.feature-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 20px;
  text-align: center;
}
.feature-icon { font-size: 36px; margin-bottom: 8px; }
.feature-card h4 { font-size: 15px; font-weight: 700; margin: 0 0 6px; color: var(--text-primary); }
.feature-card p { font-size: 12px; color: var(--text-secondary); margin: 0 0 12px; line-height: 1.5; }

/* 技巧区域 */
.tips-section {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 14px;
  padding: 24px;
  margin: 28px 0;
}
.tips-section .step-number {
  background: #ffa726;
}
.tips-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-top: 16px;
}
.tip-card {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: var(--bg-main);
  border-radius: 10px;
  border: 1px solid var(--border-light);
}
.tip-icon { font-size: 24px; flex-shrink: 0; width: 40px; text-align: center; }
.tip-content h5 {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 4px;
}
.tip-content p {
  font-size: 12px;
  color: var(--text-secondary);
  margin: 0;
  line-height: 1.5;
}
kbd {
  display: inline-block;
  padding: 1px 6px;
  font-size: 11px;
  font-family: monospace;
  background: #eceff1;
  border: 1px solid #b0bec5;
  border-radius: 4px;
  margin: 0 1px;
}

/* 底部 */
.guide-footer {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 32px 0 20px;
}
</style>
