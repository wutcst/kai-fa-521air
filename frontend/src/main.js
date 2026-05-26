/**
 * 应用入口 - 初始化Vue实例、路由、状态管理、UI组件库
 */
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'

// 全局样式
import './assets/global.css'

const app = createApp(App)

// 注册 Element Plus 图标组件
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())       // Pinia 状态管理
app.use(router)              // Vue Router 路由
app.use(ElementPlus)         // Element Plus UI 组件库

app.mount('#app')
