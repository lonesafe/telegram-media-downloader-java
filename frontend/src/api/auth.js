import request from './request'

// 认证相关 API
export const authApi = {
  login(password) {
    return request.post('/auth/login', { password })
  },
  logout() {
    return request.post('/auth/logout')
  },
  checkAuth() {
    return request.get('/auth/check')
  }
}

// 配置相关 API
export const configApi = {
  getConfig() {
    return request.get('/config')
  },
  updateConfig(data) {
    return request.put('/config', data)
  },
  getChats() {
    return request.get('/config/chats')
  },
  addChat(data) {
    return request.post('/config/chats', data)
  },
  updateChat(id, data) {
    return request.put(`/config/chats/${id}`, data)
  },
  deleteChat(id) {
    return request.delete(`/config/chats/${id}`)
  },
  getProxy() {
    return request.get('/config/proxy')
  },
  updateProxy(data) {
    return request.put('/config/proxy', data)
  },
  getCloudConfig() {
    return request.get('/config/cloud')
  },
  updateCloudConfig(data) {
    return request.put('/config/cloud', data)
  }
}

// 下载相关 API
export const downloadApi = {
  getList(alreadyDown = false) {
    return request.get('/download/list', { params: { alreadyDown } })
  },
  getSpeed() {
    return request.get('/download/speed')
  },
  setState(state) {
    return request.post('/download/state', null, { params: { state } })
  },
  createTask(data) {
    return request.post('/download/task', data)
  },
  stopTask(taskId) {
    return request.post(`/download/stop/${taskId}`)
  },
  getTaskStatus(taskId) {
    return request.get(`/download/status/${taskId}`)
  },
  getAllTasks() {
    return request.get('/download/tasks')
  },
  clearCompleted() {
    return request.delete('/download/completed')
  }
}

// 任务相关 API
export const taskApi = {
  getAll() {
    return request.get('/download/tasks')
  },
  stop(taskId) {
    return request.post(`/download/stop/${taskId}`)
  },
  getStatus(taskId) {
    return request.get(`/download/status/${taskId}`)
  }
}
