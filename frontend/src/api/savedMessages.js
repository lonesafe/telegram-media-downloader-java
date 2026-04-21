import request from './request'

export const savedMessagesApi = {
  // 获取统计信息
  getStats() {
    return request.get('/saved-messages/stats')
  },
  
  // 扫描收藏夹
  scan() {
    return request.post('/saved-messages/scan')
  },
  
  // 获取任务列表
  getTasks(includeDownloaded = false) {
    return request.get('/saved-messages/tasks', { params: { includeDownloaded } })
  },
  
  // 暂停任务
  pauseTask(taskId) {
    return request.post(`/saved-messages/tasks/${taskId}/pause`)
  },
  
  // 继续任务
  resumeTask(taskId) {
    return request.post(`/saved-messages/tasks/${taskId}/resume`)
  },
  
  // 删除任务
  deleteTask(taskId) {
    return request.delete(`/saved-messages/tasks/${taskId}`)
  },
  
  // 清除已完成任务
  clearCompleted() {
    return request.delete('/saved-messages/tasks/completed')
  }
}
