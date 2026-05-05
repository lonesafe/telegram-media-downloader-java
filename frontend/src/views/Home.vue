<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside width="200px" class="sidebar">
      <div class="logo">
        <h3>📥 TG Downloader</h3>
      </div>
      
      <el-menu
        :default-active="activeMenu"
        router
        class="sidebar-menu"
      >
        <el-menu-item index="/downloads">
          <el-icon><Download /></el-icon>
          <span>下载管理</span>
        </el-menu-item>
        
        <el-menu-item index="/saved-messages">
          <el-icon><Star /></el-icon>
          <span>收藏夹</span>
        </el-menu-item>
        
        <el-menu-item index="/forward-listener">
          <el-icon><Monitor /></el-icon>
          <span>监听转发</span>
        </el-menu-item>
        
        <el-menu-item index="/forward-tasks">
          <el-icon><Right /></el-icon>
          <span>转发任务</span>
        </el-menu-item>
        
        <el-menu-item index="/config">
          <el-icon><Setting /></el-icon>
          <span>基础配置</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 主内容区 -->
    <el-container>
      <!-- 顶部栏 -->
      <el-header class="header">
        <div class="header-left">
          <h4>{{ currentPageTitle }}</h4>
        </div>
        
        <div class="header-right">
          <el-tag :type="telegramStatus.type" v-if="telegramStatus">
            <el-icon><Connection /></el-icon>
            {{ telegramStatus.text }}
          </el-tag>
          <el-button 
            v-if="!telegramStatus?.connected" 
            type="primary" 
            size="small"
            @click="goToConfig"
          >
            去配置
          </el-button>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { telegramApi } from '@/api/telegram'

const route = useRoute()
const router = useRouter()

const telegramStatus = ref(null)
let statusTimer = null

const activeMenu = computed(() => route.path)
const currentPageTitle = computed(() => route.meta.title || '首页')

const loadTelegramStatus = async () => {
  try {
    const status = await telegramApi.getStatus()
    telegramStatus.value = {
      connected: status.connected,
      type: status.connected ? 'success' : 'danger',
      text: status.connected ? '已连接' : '未连接'
    }
  } catch (error) {
    telegramStatus.value = {
      connected: false,
      type: 'danger',
      text: '未连接'
    }
  }
}

const goToConfig = () => {
  router.push('/config')
}

onMounted(() => {
  loadTelegramStatus()
  statusTimer = setInterval(loadTelegramStatus, 10000)
})

onUnmounted(() => {
  if (statusTimer) clearInterval(statusTimer)
})
</script>

<style scoped>
.layout-container {
  min-height: 100vh;
}

.sidebar {
  background-color: #304156;
  overflow: hidden;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  border-bottom: 1px solid #3a4a5c;
}

.logo h3 {
  margin: 0;
  font-size: 16px;
}

.sidebar-menu {
  border: none;
  background-color: #304156;
}

.sidebar-menu .el-menu-item {
  color: #bfcbd9;
}

.sidebar-menu .el-menu-item:hover {
  background-color: #263445;
}

.sidebar-menu .el-menu-item.is-active {
  color: #409eff;
  background-color: #263445;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}

.header-left h4 {
  margin: 0;
  color: #333;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.main {
  padding: 20px;
  background-color: #f5f7fa;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
