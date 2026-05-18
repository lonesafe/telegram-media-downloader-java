import request from './request'

// 聊天配置 API（仅聊天配置 CRUD，Telegram 配置统一走 telegramApi）
export const configApi = {
  // 获取聊天配置列表
  getChats() {
    return request.get('/config/chats')
  },
  
  // 添加聊天配置
  addChat(data) {
    return request.post('/config/chats', data)
  },
  
  // 更新聊天配置
  updateChat(id, data) {
    return request.put(`/config/chats/${id}`, data)
  },
  
  // 删除聊天配置
  deleteChat(id) {
    return request.delete(`/config/chats/${id}`)
  }
}

// 下载管理 API
export const downloadApi = {
  // 获取下载列表（支持 tab 过滤和分页）
  // tab: downloading | waiting | completed | all
  getList(tab = 'downloading', page = 1, size = 20) {
    return request.get('/download/list', { params: { tab, page, size } })
  },
  
  // 获取下载速度
  getSpeed() {
    return request.get('/download/speed')
  },
  
  // 设置下载状态 (pause/continue)
  setState(state) {
    return request.post('/download/state', null, { params: { state } })
  },
  
  // 创建下载任务
  createTask(data) {
    return request.post('/download/task', data)
  },
  
  // 停止任务
  stopTask(taskId) {
    return request.post(`/download/stop/${taskId}`)
  },

  // 恢复任务
  resumeTask(taskId) {
    return request.post(`/download/resume/${taskId}`)
  },
  
  // 停止任务 (别名)
  stop(taskId) {
    return this.stopTask(taskId)
  },
  
  // 获取任务状态
  getStatus(taskId) {
    return request.get(`/download/status/${taskId}`)
  },
  
  // 获取所有任务
  getTasks() {
    return request.get('/download/tasks')
  },
  
  // 获取所有任务 (别名)
  getAll() {
    return this.getTasks()
  },
  
  // 删除任务
  deleteTask(taskId) {
    return request.delete(`/download/task/${taskId}`)
  },
  
  // 清除已完成任务
  clearCompleted() {
    return request.delete('/download/completed')
  }
}
