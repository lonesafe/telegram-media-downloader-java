import request from './request'

// Telegram API
export const telegramApi = {
  // ==================== 配置 ====================
  
  // 获取配置
  getConfig() {
    return request.get('/telegram/config')
  },
  
  // 保存配置
  saveConfig(data) {
    return request.post('/telegram/config', data)
  },
  
  // 测试连接
  testConnection(data) {
    return request.post('/telegram/test-connection', data)
  },
  
  // ==================== 连接管理 ====================
  
  // 获取状态
  getStatus() {
    return request.get('/telegram/status')
  },
  
  // 连接
  connect() {
    return request.post('/telegram/connect')
  },
  
  // 断开
  disconnect() {
    return request.post('/telegram/disconnect')
  },
  
  // ==================== 授权 ====================
  
  // 获取授权状态
  getAuthState() {
    return request.get('/telegram/auth/state')
  },
  
  // 发送手机号
  sendPhone(phone) {
    return request.post('/telegram/auth/phone', { phone })
  },
  
  // 验证验证码
  verifyCode(code) {
    return request.post('/telegram/auth/code', { code })
  },
  
  // 验证密码
  verifyPassword(password) {
    return request.post('/telegram/auth/password', { password })
  },
  
  // ==================== 聊天和消息 ====================
  
  // 获取聊天列表
  getChats() {
    return request.get('/telegram/chats')
  },
  
  // 获取消息
  getMessages(chatId, offset = 0, limit = 50) {
    return request.get('/telegram/messages', { 
      params: { chatId, offset, limit } 
    })
  },
  
  // 下载媒体
  downloadMedia(chatId, messageId, filePath) {
    return request.post('/telegram/download', null, { 
      params: { chatId, messageId, filePath } 
    })
  },
  
  // 转发消息
  forwardMessage(fromChatId, messageId, toChatId) {
    return request.post('/telegram/forward', null, {
      params: { fromChatId, messageId, toChatId }
    })
  },

  // ==================== Bot 管理 ====================

  // 获取 Bot 状态
  getBotStatus() {
    return request.get('/telegram/bot/status')
  },

  // 连接 Bot
  connectBot() {
    return request.post('/telegram/bot/connect')
  },

  // 断开 Bot
  disconnectBot() {
    return request.post('/telegram/bot/disconnect')
  },

  // 获取 Bot 信息
  getBotInfo() {
    return request.get('/telegram/bot/info')
  },

  // 验证 Bot Token 格式
  checkBotToken(token) {
    return request.post('/telegram/bot/check-token', { token })
  }
}
