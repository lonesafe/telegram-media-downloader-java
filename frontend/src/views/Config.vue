<template>
  <div class="config-page">
    <!-- Telegram 状态卡片 -->
    <el-card class="status-card">
      <template #header>
        <div class="card-header">
          <span>Telegram 连接状态</span>
          <el-tag :type="connectionStatus.type">{{ connectionStatus.text }}</el-tag>
        </div>
      </template>
      
      <el-descriptions :column="3" border v-if="telegramStatus">
        <el-descriptions-item label="连接状态">
          <el-tag :type="telegramStatus.connected ? 'success' : 'info'" size="small">
            {{ telegramStatus.connected ? '已连接' : '未连接' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="用户">
          <span>{{ telegramStatus.userName || telegramStatus.userId || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="手机号">
          <span>{{ telegramStatus.phoneNumber || '-' }}</span>
        </el-descriptions-item>
      </el-descriptions>
      
      <div class="status-actions">
        <el-button 
          v-if="!telegramStatus?.connected" 
          type="primary" 
          @click="connectTelegram"
          :loading="connecting"
        >
          登录 Telegram
        </el-button>
        <el-button 
          v-if="telegramStatus?.connected" 
          type="warning" 
          @click="disconnectTelegram"
        >
          断开连接
        </el-button>
        <el-button @click="loadTelegramStatus">
          <el-icon><Refresh /></el-icon>
          刷新状态
        </el-button>
      </div>
    </el-card>

    <!-- Bot 状态卡片 -->
    <el-card class="status-card" v-if="telegramConfig.botToken">
      <template #header>
        <div class="card-header">
          <span>Bot 连接状态</span>
          <el-tag :type="botStatus?.connected ? 'success' : 'info'">
            {{ botStatus?.connected ? '已连接' : '未连接' }}
          </el-tag>
        </div>
      </template>
      
      <el-descriptions :column="3" border v-if="botStatus">
        <el-descriptions-item label="Bot 名称">
          <span>{{ botStatus.botName || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="Bot 用户名">
          <span>{{ botStatus.botUsername || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="botStatus.connected ? 'success' : 'warning'" size="small">
            {{ botStatus.connected ? '在线' : '离线' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
      
      <div class="status-actions">
        <el-button 
          v-if="!botStatus?.connected" 
          type="success" 
          @click="connectBot"
          :loading="botConnecting"
        >
          连接 Bot
        </el-button>
        <el-button 
          v-if="botStatus?.connected" 
          type="danger" 
          @click="disconnectBot"
        >
          断开 Bot
        </el-button>
        <el-button @click="loadBotStatus">
          <el-icon><Refresh /></el-icon>
          刷新状态
        </el-button>
      </div>
      
      <el-alert 
        v-if="botStatus?.connected" 
        type="success" 
        :closable="false"
        class="bot-tip"
      >
        <template #title>
          Bot 已就绪，发送 /help 查看 Bot 命令
        </template>
      </el-alert>
    </el-card>

    <!-- 基础配置卡片 -->
    <el-card>
      <template #header>
        <div class="card-header">
          <span>基础配置</span>
          <el-button type="primary" @click="saveAllConfig" :loading="saving">保存</el-button>
        </div>
      </template>

      <el-tabs>
        <!-- Telegram 配置 -->
        <el-tab-pane label="Telegram">
          <el-form :model="telegramConfig" label-width="120px">
            <el-form-item label="启用服务">
              <el-switch v-model="telegramConfig.enabled" />
            </el-form-item>
            
            <el-form-item label="API ID" required>
              <el-input 
                v-model="telegramConfig.apiId" 
                placeholder="从 my.telegram.org 获取"
                :disabled="telegramStatus?.connected"
              />
            </el-form-item>
            
            <el-form-item label="API Hash" required>
              <el-input 
                v-model="telegramConfig.apiHash" 
                placeholder="从 my.telegram.org 获取"
                show-password
                :disabled="telegramStatus?.connected"
              />
            </el-form-item>
            
            <el-form-item label="Bot Token">
              <el-input 
                v-model="telegramConfig.botToken" 
                placeholder="可选，用于 Bot 模式"
                :disabled="telegramStatus?.connected"
              />
              <span class="form-tip">使用 Bot 发送命令，需要先在 BotFather 创建</span>
            </el-form-item>
            
            <el-divider content-position="left">系统信息</el-divider>
            
            <el-form-item label="自动检测">
              <el-tag type="info">
                <el-icon><Monitor /></el-icon>
                程序自动检测系统类型，从 tdlib 文件夹加载库
              </el-tag>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        
        <!-- 下载配置 -->
        <el-tab-pane label="下载">
          <el-form :model="downloadConfig" label-width="120px">
            <el-form-item label="保存路径">
              <el-input v-model="downloadConfig.savePath" placeholder="./downloads" />
            </el-form-item>
            
            <el-form-item label="临时路径">
              <el-input v-model="downloadConfig.tempPath" placeholder="./temp" />
            </el-form-item>
            
            <el-form-item label="并发任务数">
              <el-input-number v-model="downloadConfig.maxConcurrentTasks" :min="1" :max="20" />
            </el-form-item>
            
            <el-form-item label="下载类型">
              <el-checkbox-group v-model="downloadConfig.downloadTypes">
                <el-checkbox label="video">视频</el-checkbox>
                <el-checkbox label="audio">音频</el-checkbox>
                <el-checkbox label="photo">图片</el-checkbox>
                <el-checkbox label="document">文档</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            
            <el-form-item label="隐藏文件名">
              <el-switch v-model="downloadConfig.hideFileName" />
            </el-form-item>
            
            <el-form-item label="下载TXT">
              <el-switch v-model="downloadConfig.enableDownloadTxt" />
            </el-form-item>
            
            <el-form-item label="日期格式">
              <el-input v-model="downloadConfig.dateFormat" placeholder="yyyy_MM" />
            </el-form-item>
          </el-form>
        </el-tab-pane>
        
        <!-- 代理配置 -->
        <el-tab-pane label="代理">
          <el-form :model="proxyConfig" label-width="120px">
            <el-form-item label="启用代理">
              <el-switch v-model="proxyConfig.enabled" />
            </el-form-item>
            
            <el-form-item label="代理类型">
              <el-select v-model="proxyConfig.scheme" :disabled="!proxyConfig.enabled">
                <el-option label="SOCKS5" value="socks5" />
                <el-option label="SOCKS4" value="socks4" />
                <el-option label="HTTP" value="http" />
              </el-select>
            </el-form-item>
            
            <el-form-item label="代理地址">
              <el-input v-model="proxyConfig.hostname" placeholder="127.0.0.1" :disabled="!proxyConfig.enabled" />
            </el-form-item>
            
            <el-form-item label="代理端口">
              <el-input-number v-model="proxyConfig.port" :min="1" :max="65535" :disabled="!proxyConfig.enabled" />
            </el-form-item>
            
            <el-form-item label="用户名">
              <el-input v-model="proxyConfig.username" placeholder="可选" :disabled="!proxyConfig.enabled" />
            </el-form-item>
            
            <el-form-item label="密码">
              <el-input v-model="proxyConfig.password" type="password" placeholder="可选" :disabled="!proxyConfig.enabled" show-password />
            </el-form-item>
          </el-form>
        </el-tab-pane>
        
        <!-- 云盘配置 -->
        <el-tab-pane label="云盘">
          <el-form :model="cloudConfig" label-width="120px">
            <el-form-item label="启用上传">
              <el-switch v-model="cloudConfig.enableUploadFile" />
            </el-form-item>
            
            <el-form-item label="上传适配器">
              <el-select v-model="cloudConfig.uploadAdapter" :disabled="!cloudConfig.enableUploadFile">
                <el-option label="Rclone" value="rclone" />
                <el-option label="阿里云盘" value="aligo" />
              </el-select>
            </el-form-item>
            
            <el-form-item label="远程目录">
              <el-input v-model="cloudConfig.remoteDir" placeholder="drive:/telegram" :disabled="!cloudConfig.enableUploadFile" />
            </el-form-item>
            
            <el-form-item label="Rclone路径" v-if="cloudConfig.uploadAdapter === 'rclone'">
              <el-input v-model="cloudConfig.rclonePath" placeholder="rclone" />
            </el-form-item>
            
            <el-form-item label="上传前压缩">
              <el-switch v-model="cloudConfig.beforeUploadFileZip" :disabled="!cloudConfig.enableUploadFile" />
            </el-form-item>
            
            <el-form-item label="上传后删除">
              <el-switch v-model="cloudConfig.afterUploadFileDelete" :disabled="!cloudConfig.enableUploadFile" />
            </el-form-item>
          </el-form>
        </el-tab-pane>
        
        <!-- 其他配置 -->
        <el-tab-pane label="其他">
          <el-form :model="otherConfig" label-width="120px">
            <el-form-item label="登录密码">
              <el-input v-model="otherConfig.webLoginSecret" placeholder="Web 登录密码（可选）" show-password />
            </el-form-item>
            
            <el-form-item label="界面语言">
              <el-select v-model="otherConfig.language">
                <el-option label="English" value="EN" />
                <el-option label="中文" value="ZH" />
                <el-option label="Русский" value="RU" />
                <el-option label="Українська" value="UA" />
              </el-select>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 转发监听配置 -->
        <el-tab-pane label="转发监听">
          <el-form :model="forwardConfig" label-width="140px">
            <el-form-item label="启用转发监听">
              <el-switch v-model="forwardConfig.enabled" />
              <span class="form-tip">自动转发指定聊天的新消息到目标聊天</span>
            </el-form-item>

            <el-form-item label="监听状态" v-if="forwardConfig.enabled">
              <el-tag :type="forwardListenerRunning ? 'success' : 'info'">
                {{ forwardListenerRunning ? '运行中' : '已停止' }}
              </el-tag>
              <el-button 
                size="small" 
                :type="forwardListenerRunning ? 'danger' : 'success'"
                @click="toggleForwardListener"
                style="margin-left: 12px;"
              >
                {{ forwardListenerRunning ? '停止' : '启动' }}
              </el-button>
            </el-form-item>

            <el-divider v-if="forwardConfig.enabled" content-position="left">源聊天配置</el-divider>

            <el-form-item label="源聊天ID" v-if="forwardConfig.enabled">
              <div class="chat-list">
                <el-tag
                  v-for="chatId in forwardConfig.sourceChatIds"
                  :key="chatId"
                  closable
                  @close="removeSourceChat(chatId)"
                  style="margin-right: 8px; margin-bottom: 8px;"
                >
                  {{ chatId }}
                </el-tag>
                <el-input-number
                  v-model="newSourceChatId"
                  placeholder="输入聊天ID"
                  style="width: 150px;"
                  :controls="false"
                />
                <el-button type="primary" size="small" @click="addSourceChat" style="margin-left: 8px;">
                  添加
                </el-button>
              </div>
              <span class="form-tip">监听的源聊天ID列表，新消息将自动转发到目标聊天</span>
            </el-form-item>

            <el-form-item label="目标聊天ID" v-if="forwardConfig.enabled">
              <el-input-number
                v-model="forwardConfig.targetChatId"
                placeholder="输入目标聊天ID"
                style="width: 200px;"
                :controls="false"
              />
              <span class="form-tip">转发消息的目标聊天ID</span>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- Saved Messages 配置 -->
        <el-tab-pane label="收藏夹监听">
          <el-form :model="savedMessagesConfig" label-width="140px">
            <el-form-item label="启用收藏夹监听">
              <el-switch v-model="savedMessagesConfig.enabled" />
              <span class="form-tip">开启后监听 Saved Messages 变化，新媒体自动加入下载</span>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- Telegram 授权对话框 -->
    <el-dialog
      v-model="authDialogVisible"
      title="Telegram 授权"
      width="450px"
      :close-on-click-modal="false"
    >
      <div class="auth-content">
        <el-steps :active="authStep" direction="vertical">
          <el-step title="输入手机号" description="注册 Telegram 的手机号" />
          <el-step title="输入验证码" description="Telegram 发送的验证码" />
          <el-step title="输入密码" v-if="needPassword" description="两步验证密码（如果设置了）" />
        </el-steps>
        
        <el-form v-if="authStep === 0" :model="authForm" @submit.prevent="submitPhone">
          <el-form-item label="手机号">
            <el-input 
              v-model="authForm.phone" 
              placeholder="+86 138xxxxxxx"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="submitPhone" :loading="authLoading">
              发送验证码
            </el-button>
          </el-form-item>
        </el-form>
        
        <el-form v-else-if="authStep === 1" :model="authForm" @submit.prevent="submitCode">
          <el-form-item label="验证码">
            <el-input 
              v-model="authForm.code" 
              placeholder="输入验证码"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="submitCode" :loading="authLoading">
              确认
            </el-button>
            <el-button @click="authStep = 0; authForm.code = ''">重新输入</el-button>
          </el-form-item>
        </el-form>
        
        <el-form v-else-if="authStep === 2" :model="authForm" @submit.prevent="submitPassword">
          <el-form-item label="密码">
            <el-input 
              v-model="authForm.password" 
              type="password"
              placeholder="两步验证密码"
              show-password
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="submitPassword" :loading="authLoading">
              确认
            </el-button>
          </el-form-item>
        </el-form>
        
        <el-alert 
          v-if="authMessage" 
          :title="authMessage" 
          :type="authMessageType"
          show-icon
          class="auth-alert"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { telegramApi } from '@/api/telegram'
import { configApi } from '@/api/download'
import { forwardApi } from '@/api/forward'

const saving = ref(false)
const connecting = ref(false)

// Telegram 状态
const telegramStatus = ref(null)

// Bot 状态
const botStatus = ref(null)
const botConnecting = ref(false)

const connectionStatus = computed(() => {
  if (!telegramStatus.value) {
    return { type: 'info', text: '未初始化' }
  }
  if (telegramStatus.value.connected) {
    return { type: 'success', text: '已连接' }
  }
  return { type: 'warning', text: '未连接' }
})

// Telegram 配置
const telegramConfig = reactive({
  enabled: true,
  apiId: '',
  apiHash: '',
  botToken: ''
})

// 下载配置
const downloadConfig = reactive({
  savePath: './downloads',
  tempPath: './temp',
  maxConcurrentTasks: 5,
  downloadTypes: ['video'],
  hideFileName: false,
  enableDownloadTxt: false,
  dateFormat: 'yyyy_MM'
})

// 代理配置
const proxyConfig = reactive({
  enabled: false,
  scheme: 'socks5',
  hostname: '',
  port: 1080,
  username: '',
  password: ''
})

// 云盘配置
const cloudConfig = reactive({
  enableUploadFile: false,
  uploadAdapter: 'rclone',
  remoteDir: '',
  rclonePath: 'rclone',
  beforeUploadFileZip: false,
  afterUploadFileDelete: false
})

// 其他配置
const otherConfig = reactive({
  webLoginSecret: '',
  language: 'ZH'
})

// 转发监听配置
const forwardConfig = reactive({
  enabled: true,
  sourceChatIds: [],
  targetChatId: null
})
const forwardListenerRunning = ref(false)
const newSourceChatId = ref(null)

// Saved Messages 配置
const savedMessagesConfig = reactive({
  enabled: true
})

// 授权对话框
const authDialogVisible = ref(false)
const authStep = ref(0)
const authForm = reactive({
  phone: '',
  code: '',
  password: ''
})
const authMessage = ref('')
const authMessageType = ref('info')
const needPassword = ref(false)
const authLoading = ref(false)

// 加载 Telegram 状态
const loadTelegramStatus = async () => {
  try {
    const status = await telegramApi.getStatus()
    telegramStatus.value = status
  } catch (error) {
    console.error('Failed to load telegram status:', error)
  }
}

// 加载 Bot 状态
const loadBotStatus = async () => {
  try {
    const status = await telegramApi.getBotStatus()
    botStatus.value = status
  } catch (error) {
    console.error('Failed to load bot status:', error)
    botStatus.value = null
  }
}

// 连接 Bot
const connectBot = async () => {
  if (!telegramConfig.botToken) {
    ElMessage.warning('请先配置 Bot Token')
    return
  }
  
  botConnecting.value = true
  try {
    await telegramApi.connectBot()
    await loadBotStatus()
    ElMessage.success('Bot 连接成功')
  } catch (error) {
    ElMessage.error('Bot 连接失败: ' + (error.message || '未知错误'))
  } finally {
    botConnecting.value = false
  }
}

// 断开 Bot
const disconnectBot = async () => {
  try {
    await telegramApi.disconnectBot()
    await loadBotStatus()
    ElMessage.success('Bot 已断开')
  } catch (error) {
    ElMessage.error('断开失败')
  }
}

// 连接 Telegram
const connectTelegram = async () => {
  // 先保存配置
  await saveTelegramConfig()
  
  connecting.value = true
  try {
    // 调用 /connect 启动 TDLib 客户端（会进入等待手机号状态）
    await telegramApi.connect()
    
    // 立即打开授权对话框，让用户输入手机号
    authDialogVisible.value = true
    authStep.value = 0
    authMessage.value = '请输入您的 Telegram 手机号'
    authMessageType.value = 'info'
    
    // 定期检查状态变化
    const timer = setInterval(async () => {
      const state = await telegramApi.getAuthState()
      if (state.needCode) {
        authStep.value = 1
        authMessage.value = '验证码已发送，请输入'
        authMessageType.value = 'info'
        clearInterval(timer)
      } else if (state.needPassword) {
        authStep.value = 2
        authMessage.value = '请输入两步验证密码'
        authMessageType.value = 'info'
        clearInterval(timer)
      } else if (state.isConnected) {
        authStep.value = 3
        authMessage.value = '登录成功！'
        authMessageType.value = 'success'
        clearInterval(timer)
        setTimeout(() => {
          authDialogVisible.value = false
          loadTelegramStatus()
        }, 1000)
      }
    }, 1000)
    
  } catch (error) {
    ElMessage.error('连接失败: ' + (error.message || '未知错误'))
  } finally {
    connecting.value = false
  }
}

// 断开连接
const disconnectTelegram = async () => {
  try {
    await telegramApi.disconnect()
    await loadTelegramStatus()
    ElMessage.success('已断开连接')
  } catch (error) {
    ElMessage.error('断开失败')
  }
}

// 提交手机号
const submitPhone = async () => {
  if (!authForm.phone) {
    ElMessage.warning('请输入手机号')
    return
  }
  
  authLoading.value = true
  authMessage.value = ''
  
  try {
    await telegramApi.sendPhone(authForm.phone)
    authMessage.value = '验证码已发送到: ' + authForm.phone
    authMessageType.value = 'success'
    authStep.value = 1
  } catch (error) {
    authMessage.value = error.message || '发送失败'
    authMessageType.value = 'error'
  } finally {
    authLoading.value = false
  }
}

// 提交验证码
const submitCode = async () => {
  if (!authForm.code) {
    ElMessage.warning('请输入验证码')
    return
  }
  
  authLoading.value = true
  authMessage.value = ''
  
  try {
    await telegramApi.verifyCode(authForm.code)
    authMessage.value = '验证成功'
    authMessageType.value = 'success'
    authStep.value = 2
    needPassword.value = true
  } catch (error) {
    if (error.message?.includes('password')) {
      needPassword.value = true
      authStep.value = 2
    } else {
      authMessage.value = error.message || '验证失败'
      authMessageType.value = 'error'
    }
  } finally {
    authLoading.value = false
  }
}

// 提交密码
const submitPassword = async () => {
  authLoading.value = true
  authMessage.value = ''
  
  try {
    await telegramApi.verifyPassword(authForm.password)
    authMessage.value = '授权成功'
    authMessageType.value = 'success'
    
    // 关闭对话框并刷新状态
    setTimeout(() => {
      authDialogVisible.value = false
      loadTelegramStatus()
    }, 1000)
  } catch (error) {
    authMessage.value = error.message || '授权失败'
    authMessageType.value = 'error'
  } finally {
    authLoading.value = false
  }
}

// ==================== 转发监听管理 ====================

// 启动/停止转发监听
const toggleForwardListener = async () => {
  try {
    if (forwardListenerRunning.value) {
      await forwardApi.stopListener()
      forwardListenerRunning.value = false
      ElMessage.success('转发监听已停止')
    } else {
      await forwardApi.startListener()
      forwardListenerRunning.value = true
      ElMessage.success('转发监听已启动')
    }
  } catch (error) {
    ElMessage.error('操作失败: ' + (error.message || '未知错误'))
  }
}

// 添加源聊天
const addSourceChat = async () => {
  if (!newSourceChatId.value) {
    ElMessage.warning('请输入聊天ID')
    return
  }
  try {
    await forwardApi.addSourceChat(newSourceChatId.value)
    forwardConfig.sourceChatIds.push(newSourceChatId.value)
    newSourceChatId.value = null
    ElMessage.success('已添加')
  } catch (error) {
    ElMessage.error('添加失败: ' + (error.message || '未知错误'))
  }
}

// 移除源聊天
const removeSourceChat = async (chatId) => {
  try {
    await forwardApi.removeSourceChat(chatId)
    const index = forwardConfig.sourceChatIds.indexOf(chatId)
    if (index > -1) {
      forwardConfig.sourceChatIds.splice(index, 1)
    }
    ElMessage.success('已移除')
  } catch (error) {
    ElMessage.error('移除失败: ' + (error.message || '未知错误'))
  }
}

// 保存 Telegram 配置
const saveTelegramConfig = async () => {
  try {
    await telegramApi.saveConfig(telegramConfig)
  } catch (error) {
    console.error('Failed to save telegram config:', error)
  }
}

// 保存所有配置
const saveAllConfig = async () => {
  saving.value = true
  try {
    // 保存各部分配置
    await Promise.all([
      telegramApi.saveConfig({
        ...telegramConfig,
        forwardListenerEnabled: forwardConfig.enabled,
        forwardListenerSourceChatIds: JSON.stringify(forwardConfig.sourceChatIds),
        forwardListenerTargetChatId: forwardConfig.targetChatId,
        savedMessagesEnabled: savedMessagesConfig.enabled
      }),
      configApi.save({
        ...otherConfig,
        savePath: downloadConfig.savePath,
        tempPath: downloadConfig.tempPath,
        maxConcurrentTasks: downloadConfig.maxConcurrentTasks,
        downloadTypes: JSON.stringify(downloadConfig.downloadTypes),
        hideFileName: downloadConfig.hideFileName,
        enableDownloadTxt: downloadConfig.enableDownloadTxt,
        dateFormat: downloadConfig.dateFormat
      }),
      configApi.saveProxy(proxyConfig),
      configApi.saveCloud(cloudConfig)
    ])
    
    ElMessage.success('配置已保存')
  } catch (error) {
    console.error('Failed to save config:', error)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

// 加载配置
const loadConfig = async () => {
  try {
    // 加载 Telegram 配置
    try {
      const tgConfig = await telegramApi.getConfig()
      Object.assign(telegramConfig, tgConfig)
      
      // 加载转发监听配置
      if (tgConfig.forwardListenerEnabled !== undefined) {
        forwardConfig.enabled = tgConfig.forwardListenerEnabled
      }
      if (tgConfig.forwardListenerSourceChatIds) {
        try {
          forwardConfig.sourceChatIds = JSON.parse(tgConfig.forwardListenerSourceChatIds)
        } catch (e) {
          forwardConfig.sourceChatIds = []
        }
      }
      if (tgConfig.forwardListenerTargetChatId) {
        forwardConfig.targetChatId = tgConfig.forwardListenerTargetChatId
      }
      
      // 加载 Saved Messages 配置
      if (tgConfig.savedMessagesEnabled !== undefined) {
        savedMessagesConfig.enabled = tgConfig.savedMessagesEnabled
      }
    } catch (e) {
      console.error('Failed to load telegram config:', e)
    }
    
    // 加载转发监听状态
    try {
      const listenerConfig = await forwardApi.getListenerConfig()
      forwardListenerRunning.value = listenerConfig.isRunning
    } catch (e) {
      console.error('Failed to load forward listener config:', e)
    }
    
    // 加载基础配置
    try {
      const baseConfig = await configApi.get()
      if (baseConfig) {
        // 解析下载类型（JSON数组字符串转为数组）
        let downloadTypes = ['video']
        if (baseConfig.downloadTypes) {
          try {
            downloadTypes = JSON.parse(baseConfig.downloadTypes)
          } catch (e) {
            console.warn('解析下载类型失败，使用默认值:', e)
          }
        }
        Object.assign(downloadConfig, {
          savePath: baseConfig.savePath || './downloads',
          tempPath: baseConfig.tempPath || './temp',
          maxConcurrentTasks: baseConfig.maxConcurrentTasks || 5,
          downloadTypes: downloadTypes,
          hideFileName: baseConfig.hideFileName || false,
          enableDownloadTxt: baseConfig.enableDownloadTxt || false,
          dateFormat: baseConfig.dateFormat || 'yyyy_MM'
        })
        Object.assign(otherConfig, {
          webLoginSecret: baseConfig.webLoginSecret || '',
          language: baseConfig.language || 'ZH'
        })
      }
    } catch (e) {
      console.error('Failed to load base config:', e)
    }
    
    // 加载代理配置
    try {
      const proxy = await configApi.getProxy()
      Object.assign(proxyConfig, proxy)
    } catch (e) {
      console.error('Failed to load proxy config:', e)
    }
    
    // 加载云盘配置
    try {
      const cloud = await configApi.getCloud()
      Object.assign(cloudConfig, cloud)
    } catch (e) {
      console.error('Failed to load cloud config:', e)
    }
  } catch (error) {
    console.error('Failed to load config:', error)
  }
}

onMounted(() => {
  loadConfig()
  loadTelegramStatus()
  loadBotStatus()
})
</script>

<style scoped>
.config-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.form-tip {
  display: block;
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.status-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.status-card :deep(.el-card__header) {
  background: rgba(255, 255, 255, 0.1);
  color: white;
}

.status-card :deep(.el-card__body) {
  color: white;
}

.status-card :deep(.el-descriptions__label) {
  color: rgba(255, 255, 255, 0.8);
}

.status-card :deep(.el-descriptions__content) {
  color: white;
}

.status-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}

.auth-content {
  padding: 20px;
}

.auth-alert {
  margin-top: 16px;
}
</style>
