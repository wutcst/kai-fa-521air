<!--
  LoginView.vue - 登录/注册页面（v2 - 小清新绿色主题）
  功能：账号密码登录、注册切换、Mock数据模拟
-->
<template>
  <div class="login-container">
    <!-- 背景叶子装饰 -->
    <div class="bg-leaves">
      <span v-for="i in 8" :key="i" class="leaf" :style="getLeafStyle(i)">🌿</span>
      <span v-for="i in 5" :key="'f'+i" class="leaf small" :style="getLeafStyle(i+10)">🍃</span>
    </div>

    <!-- 登录卡片 -->
    <div class="login-card">
      <div class="card-header">
        <div class="logo-icon">
          <svg width="56" height="56" viewBox="0 0 56 56">
            <circle cx="28" cy="28" r="27" fill="#e8f5e9" stroke="#66bb6a" stroke-width="2"/>
            <path d="M16,26 Q24,16 32,26 Q40,36 42,30" stroke="#43a047" stroke-width="2.5" fill="none" stroke-linecap="round"/>
            <circle cx="41" cy="28" r="2.5" fill="#2e7d32"/>
            <ellipse cx="18" cy="24" rx="5" ry="2.5" fill="#a5d6a7" transform="rotate(-15,18,24)"/>
          </svg>
        </div>
        <h1 class="game-title">多人联机贪吃蛇</h1>
        <p class="game-subtitle">Snake Battle · 小清新版</p>
      </div>

      <!-- Tab -->
      <div class="mode-tabs">
        <span :class="['tab-item', { active: isLoginMode }]" @click="switchMode(true)">登录</span>
        <span class="tab-divider">|</span>
        <span :class="['tab-item', { active: !isLoginMode }]" @click="switchMode(false)">注册</span>
      </div>

      <!-- 表单 -->
      <el-form ref="formRef" :model="formData" :rules="formRules" class="login-form" @keyup.enter="handleSubmit">
        <el-form-item prop="username">
          <el-input v-model="formData.username" placeholder="请输入用户名" :prefix-icon="User" size="large" clearable />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="formData.password" type="password" placeholder="请输入密码" :prefix-icon="Lock" size="large" show-password />
        </el-form-item>

        <template v-if="!isLoginMode">
          <el-form-item prop="nickname">
            <el-input v-model="formData.nickname" placeholder="请输入昵称（选填）" :prefix-icon="UserFilled" size="large" clearable />
          </el-form-item>
          <el-form-item prop="confirmPassword">
            <el-input v-model="formData.confirmPassword" type="password" placeholder="请再次输入密码" :prefix-icon="Lock" size="large" show-password />
          </el-form-item>
        </template>

        <div class="form-options" v-if="isLoginMode">
          <el-checkbox v-model="rememberMe">记住密码</el-checkbox>
          <span class="forgot-link">忘记密码？</span>
        </div>

        <el-button type="primary" size="large" class="submit-btn" :loading="isLoading" @click="handleSubmit">
          {{ isLoginMode ? '登 录' : '注 册' }}
        </el-button>
      </el-form>

      <div class="card-footer">
        <span v-if="isLoginMode">还没有账号？</span>
        <span v-else>已有账号？</span>
        <a href="javascript:void(0)" @click="switchMode(!isLoginMode)">
          {{ isLoginMode ? '立即注册' : '立即登录' }}
        </a>
      </div>

      <el-alert title="当前为Mock模式，输入任意用户名密码即可登录（如：admin / 123456）" type="success" :closable="false" show-icon style="margin-top:12px" />
    </div>

    <div class="version-info">v2.0 | 小清新绿色主题</div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, UserFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { loginApi, registerApi } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref(null)
const isLoading = ref(false)
const isLoginMode = ref(true)
const rememberMe = ref(false)

const formData = reactive({
  username: 'admin',
  password: '123456',
  nickname: '',
  confirmPassword: ''
})

const formRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度为2-20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 3, max: 20, message: '密码长度为3-20个字符', trigger: 'blur' }
  ],
  confirmPassword: [{
    validator: (rule, value, callback) => {
      if (!value) callback(new Error('请再次输入密码'))
      else if (value !== formData.password) callback(new Error('两次输入密码不一致'))
      else callback()
    }, trigger: 'blur'
  }]
}

function switchMode(isLogin) {
  isLoginMode.value = isLogin
  formRef.value?.resetFields()
  formData.username = ''
  formData.password = ''
  formData.nickname = ''
  formData.confirmPassword = ''
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  isLoading.value = true
  try {
    if (isLoginMode.value) {
      const { user, token } = await loginApi(formData.username, formData.password)
      userStore.login(user, token)
      ElMessage.success(`欢迎回来，${user.nickname || user.username}！🌿`)
      const redirect = route.query.redirect || '/lobby'
      router.push(redirect)
    } else {
      const { user, token } = await registerApi(formData.username, formData.password, formData.nickname)
      userStore.login(user, token)
      ElMessage.success(`注册成功！欢迎你，${user.nickname || user.username}！🌿`)
      router.push('/lobby')
    }
  } catch (error) {
    ElMessage.error(error.message || '操作失败，请重试')
  } finally { isLoading.value = false }
}

function getLeafStyle(i) {
  return {
    left: `${5 + (i % 4) * 25 + Math.random() * 10}%`,
    top: `${5 + Math.floor(i / 4) * 30 + Math.random() * 15}%`,
    animationDelay: `${i * 0.4}s`,
    animationDuration: `${3 + Math.random() * 3}s`,
    fontSize: `${20 + Math.random() * 16}px`
  }
}
</script>

<style scoped>
.login-container {
  width: 100%; height: 100vh;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  background: linear-gradient(135deg, #e8f5e9, #c8e6c9, #f1f8e9, #dcedc8);
  position: relative; overflow: hidden;
}

/* 叶子装饰 */
.bg-leaves { position: absolute; inset: 0; pointer-events: none; }
.leaf {
  position: absolute; opacity: 0.15;
  animation: floatLeaf 4s ease-in-out infinite;
}
.leaf.small { opacity: 0.1; }
@keyframes floatLeaf {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-15px) rotate(10deg); }
}

/* 卡片 */
.login-card {
  width: 420px; max-width: 92vw;
  background: rgba(255,255,255,0.92);
  backdrop-filter: blur(8px);
  border: 1px solid #c8e6c9;
  border-radius: 18px;
  padding: 36px 36px 28px;
  position: relative; z-index: 1;
  box-shadow: 0 12px 40px rgba(46,59,46,0.1);
}

.card-header { text-align: center; margin-bottom: 24px; }
.logo-icon { margin-bottom: 8px; }
.game-title {
  font-size: 24px; font-weight: 700;
  color: #2e7d32; letter-spacing: 2px; margin: 0;
}
.game-subtitle {
  font-size: 13px; color: #81c784; margin-top: 4px; letter-spacing: 3px;
}

.mode-tabs {
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 22px; gap: 14px;
}
.tab-item {
  font-size: 15px; color: #9eae9e; cursor: pointer;
  transition: all 0.3s; user-select: none;
}
.tab-item:hover { color: #66bb6a; }
.tab-item.active { color: #43a047; font-weight: 700; transform: scale(1.05); }
.tab-divider { color: #c8e6c9; }

.login-form { margin-bottom: 6px; }
:deep(.el-input__wrapper) {
  background: #f5f9f0 !important;
  border: 1px solid #dcedc8 !important;
  box-shadow: none !important;
}
:deep(.el-input__wrapper:hover) { border-color: #a5d6a7 !important; }
:deep(.el-input__wrapper.is-focus) { border-color: #66bb6a !important; }
:deep(.el-input__inner) { color: #2e3b2e; }
:deep(.el-input__inner::placeholder) { color: #a5b5a5; }

.form-options {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 18px; font-size: 13px;
}
.forgot-link { color: #81c784; cursor: pointer; }
.forgot-link:hover { color: #43a047; }

.submit-btn {
  width: 100%; height: 46px; font-size: 16px;
  letter-spacing: 6px; border-radius: 10px;
  background: linear-gradient(135deg, #66bb6a, #43a047);
  border: none;
}
.submit-btn:hover {
  background: linear-gradient(135deg, #81c784, #66bb6a);
}

.card-footer { text-align: center; font-size: 13px; color: #6b8a6b; margin-top: 6px; }
.card-footer a { color: #43a047; text-decoration: none; margin-left: 4px; }
.card-footer a:hover { opacity: 0.8; }

.version-info {
  position: absolute; bottom: 20px;
  font-size: 12px; color: #a5d6a7; z-index: 1;
}
</style>
