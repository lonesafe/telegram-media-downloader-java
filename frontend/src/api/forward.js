import request from './request'

export const forwardApi = {
  // ==================== 转发任务管理 ====================
  
  // 获取所有转发任务
  getTasks(params = {}) {
    return request.get('/forward/tasks', { params })
  },
  
  // 获取手动提交的任务
  getManualTasks(params = {}) {
    return request.get('/forward/tasks/manual', { params })
  },
  
  // 获取自动监听的任务
  getAutoTasks(params = {}) {
    return request.get('/forward/tasks/auto', { params })
  },
  
  // 按状态获取任务
  getTasksByStatus(status, params = {}) {
    return request.get(`/forward/tasks/status/${status}`, { params })
  },
  
  // 获取统计信息
  getStatistics() {
    return request.get('/forward/statistics')
  },
  
  // 创建转发任务
  createTask(sourceChatId, messageId, targetChatId) {
    return request.post('/forward/tasks', null, {
      params: { sourceChatId, messageId, targetChatId }
    })
  },
  
  // 批量创建转发任务
  createBatchTasks(sourceChatId, messageIds, targetChatId) {
    return request.post('/forward/tasks/batch', {
      sourceChatId,
      messageIds,
      targetChatId
    })
  },
  
  // 重试失败的任务
  retryTask(taskId) {
    return request.post(`/forward/tasks/${taskId}/retry`)
  },
  
  // 删除转发任务
  deleteTask(taskId) {
    return request.delete(`/forward/tasks/${taskId}`)
  },
  
  // 暂停任务
  pauseTask(taskId) {
    return request.post(`/forward/tasks/${taskId}/pause`)
  },
  
  // 继续任务
  resumeTask(taskId) {
    return request.post(`/forward/tasks/${taskId}/resume`)
  },
  
  // 全部暂停
  pauseAll() {
    return request.post('/forward/tasks/pause-all')
  },
  
  // 全部继续
  resumeAll() {
    return request.post('/forward/tasks/resume-all')
  },
  
  // 重试所有失败任务
  retryFailed() {
    return request.post('/forward/tasks/retry-failed')
  },
  
  // 清除已完成任务
  clearCompleted() {
    return request.delete('/forward/tasks/completed')
  },

  // ==================== 转发监听管理 ====================

  // 获取转发监听配置
  getListenerConfig() {
    return request.get('/forward/listener/config')
  },

  // 更新转发监听配置
  updateListenerConfig(config) {
    return request.post('/forward/listener/config', config)
  },
  
  // 删除监听规则
  deleteRule(ruleId) {
    return request.delete(`/forward/listener/rules/${ruleId}`)
  },

  // 启动转发监听
  startListener() {
    return request.post('/forward/listener/start')
  },

  // 停止转发监听
  stopListener() {
    return request.post('/forward/listener/stop')
  },

  // 添加源聊天
  addSourceChat(chatId) {
    return request.post('/forward/listener/sources', { chatId })
  },

  // 移除源聊天
  removeSourceChat(chatId) {
    return request.delete(`/forward/listener/sources/${chatId}`)
  },

  // 设置目标聊天
  setTargetChat(chatId) {
    return request.post('/forward/listener/target', { chatId })
  },
  
  // 获取日志
  getLogs(ruleId) {
    return request.get(`/forward/listener/logs/${ruleId}`)
  }
}
