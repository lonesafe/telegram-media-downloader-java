<template>
  <div class="saved-messages-page">
    <!-- 顶部操作栏 -->
    <el-card class="toolbar-card">
      <div class="toolbar">
        <div class="left">
          <el-button type="primary" @click="handleScan" :loading="scanning">
            <el-icon><Search /></el-icon>
            扫描收藏夹
          </el-button>
          <el-button @click="refreshList" :loading="loading">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
          <el-button @click="pauseAll" type="warning" plain>
            <el-icon><VideoPause /></el-icon>
            全部暂停
          </el-button>
          <el-button @click="resumeAll" type="success" plain>
            <el-icon><VideoPlay /></el-icon>
            全部继续
          </el-button>
          <el-button @click="clearCompleted" type="danger" plain>
            <el-icon><Delete /></el-icon>
            清除已完成
          </el-button>
        </div>
        
        <div class="right">
          <el-tag type="info" v-if="stats.chatId">
            收藏夹 Chat ID: {{ stats.chatId }}
          </el-tag>
          <el-tag :type="monitoring ? 'success' : 'warning'" style="margin-left: 8px">
            {{ monitoring ? '监听中' : '未监听' }}
          </el-tag>
        </div>
      </div>
      
      <!-- 统计信息 -->
      <div class="stats-info">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-statistic title="总任务数" :value="stats.total" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="已下载" :value="stats.downloaded">
              <template #prefix>
                <el-icon class="stat-success"><CircleCheck /></el-icon>
              </template>
            </el-statistic>
          </el-col>
          <el-col :span="6">
            <el-statistic title="待下载" :value="stats.pending">
              <template #prefix>
                <el-icon class="stat-warning"><Clock /></el-icon>
              </template>
            </el-statistic>
          </el-col>
          <el-col :span="6">
            <el-statistic title="当前下载速度" :value="formatSpeed(globalSpeed)">
              <template #suffix>MB/s</template>
            </el-statistic>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <!-- Tab 切换 -->
    <el-card>
      <el-tabs v-model="activeTab" @tab-change="refreshList">
        <el-tab-pane label="待下载" name="pending">
          <el-table :data="pendingList" v-loading="loading" stripe>
            <el-table-column type="index" width="50" />
            
            <el-table-column label="消息ID" width="100" prop="messageId" />
            
            <el-table-column label="文件名" min-width="200" show-overflow-tooltip>
              <template #default="{ row }">
                <span>{{ row.fileName || '未命名' }}</span>
              </template>
            </el-table-column>
            
            <el-table-column label="大小" width="100">
              <template #default="{ row }">
                {{ formatFileSize(row.fileSize) }}
              </template>
            </el-table-column>
            
            <el-table-column label="进度" width="180">
              <template #default="{ row }">
                <el-progress
                  :percentage="row.downloadProgress || 0"
                  :status="getProgressStatus(row.status)"
                  :stroke-width="8"
                />
              </template>
            </el-table-column>
            
            <el-table-column label="速度" width="100">
              <template #default="{ row }">
                <span :class="{ 'speed-active': row.isRunning }">
                  {{ row.downloadSpeed || '0 B/s' }}
                </span>
              </template>
            </el-table-column>
            
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" size="small">
                  {{ getStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button-group>
                  <el-button
                    v-if="canPause(row)"
                    type="warning"
                    size="small"
                    @click="pauseTask(row.id)"
                  >
                    暂停
                  </el-button>
                  <el-button
                    v-if="canResume(row)"
                    type="success"
                    size="small"
                    @click="resumeTask(row.id)"
                  >
                    继续
                  </el-button>
                  <el-button
                    type="danger"
                    size="small"
                    @click="deleteTask(row.id)"
                  >
                    删除
                  </el-button>
                </el-button-group>
              </template>
            </el-table-column>
          </el-table>
          
          <el-empty v-if="pendingList.length === 0 && !loading" description="暂无待下载任务" />
        </el-tab-pane>
        
        <el-tab-pane label="已下载" name="downloaded">
          <el-table :data="downloadedList" v-loading="loading" stripe>
            <el-table-column type="index" width="50" />
            
            <el-table-column label="消息ID" width="100" prop="messageId" />
            
            <el-table-column label="文件名" min-width="250" show-overflow-tooltip>
              <template #default="{ row }">
                <span>{{ row.fileName || '未命名' }}</span>
              </template>
            </el-table-column>
            
            <el-table-column label="文件大小" width="120">
              <template #default="{ row }">
                {{ formatFileSize(row.fileSize) }}
              </template>
            </el-table-column>
            
            <el-table-column label="保存路径" min-width="250" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="save-path">{{ row.localPath || '-' }}</span>
              </template>
            </el-table-column>
            
            <el-table-column label="完成时间" width="160">
              <template #default="{ row }">
                {{ formatTime(row.finishedAt) }}
              </template>
            </el-table-column>
            
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button type="danger" size="small" @click="deleteTask(row.id)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          
          <el-empty v-if="downloadedList.length === 0 && !loading" description="暂无已下载文件" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { savedMessagesApi } from '@/api/savedMessages'
import { downloadApi } from '@/api/download'

const loading = ref(false)
const scanning = ref(false)
const monitoring = ref(false)
const activeTab = ref('pending')
const pendingList = ref([])
const downloadedList = ref([])
const globalSpeed = ref(0)

const stats = reactive({
  total: 0,
  downloaded: 0,
  pending: 0,
  chatId: null
})

let refreshTimer = null
let speedTimer = null

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (!bytes || bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let size = bytes
  let unitIndex = 0
  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024
    unitIndex++
  }
  return size.toFixed(2) + ' ' + units[unitIndex]
}

// 格式化速度
const formatSpeed = (speed) => {
  if (!speed) return '0'
  return (speed / 1024 / 1024).toFixed(2)
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return '-'
  const date = new Date(time)
  return date.toLocaleString('zh-CN')
}

// 获取状态类型
const getStatusType = (status) => {
  const map = {
    'SUCCESS_DOWNLOAD': 'success',
    'FAILED_DOWNLOAD': 'danger',
    'PAUSED': 'warning',
    'DOWNLOADING': 'primary',
    'PENDING': 'info'
  }
  return map[status] || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
  const map = {
    'SUCCESS_DOWNLOAD': '已完成',
    'FAILED_DOWNLOAD': '失败',
    'PAUSED': '已暂停',
    'DOWNLOADING': '下载中',
    'PENDING': '等待中'
  }
  return map[status] || status
}

// 获取进度条状态
const getProgressStatus = (status) => {
  if (status === 'SUCCESS_DOWNLOAD') return 'success'
  if (status === 'FAILED_DOWNLOAD') return 'exception'
  if (status === 'PAUSED') return 'warning'
  return ''
}

// 判断是否可以暂停
const canPause = (row) => {
  return row.isRunning && row.status === 'DOWNLOADING'
}

// 判断是否可以继续
const canResume = (row) => {
  return row.status === 'PAUSED' || 
         (!row.isRunning && row.status !== 'SUCCESS_DOWNLOAD' && row.status !== 'SKIP_DOWNLOAD')
}

// 刷新统计
const refreshStats = async () => {
  try {
    const data = await savedMessagesApi.getStats()
    stats.total = data.total || 0
    stats.downloaded = data.downloaded || 0
    stats.pending = data.pending || 0
    stats.chatId = data.chatId
    monitoring.value = data.monitoring || false
  } catch (error) {
    console.error('获取统计失败:', error)
  }
}

// 刷新速度
const refreshSpeed = async () => {
  try {
    const speed = await downloadApi.getSpeed()
    const speedNum = parseFloat(speed.download_speed) || 0
    globalSpeed.value = speedNum
  } catch (error) {
    console.error('获取速度失败:', error)
  }
}

// 刷新列表
const refreshList = async () => {
  loading.value = true
  try {
    if (activeTab.value === 'pending') {
      // 待下载：包含 PENDING, DOWNLOADING, PAUSED, FAILED
      const result = await savedMessagesApi.getTasks(false)
      pendingList.value = result.tasks || []
    } else {
      // 已下载
      const result = await savedMessagesApi.getTasks(true)
      downloadedList.value = (result.tasks || []).filter(t => t.status === 'SUCCESS_DOWNLOAD')
    }
  } catch (error) {
    ElMessage.error('刷新失败')
  } finally {
    loading.value = false
  }
}

// 扫描收藏夹
const handleScan = async () => {
  scanning.value = true
  try {
    const result = await savedMessagesApi.scan()
    ElMessage.success(result.message || '扫描完成')
    refreshList()
    refreshStats()
  } catch (error) {
    ElMessage.error('扫描失败')
  } finally {
    scanning.value = false
  }
}

// 暂停任务
const pauseTask = async (taskId) => {
  try {
    await savedMessagesApi.pauseTask(taskId)
    ElMessage.success('已暂停')
    refreshList()
  } catch (error) {
    ElMessage.error('暂停失败')
  }
}

// 继续任务
const resumeTask = async (taskId) => {
  try {
    await savedMessagesApi.resumeTask(taskId)
    ElMessage.success('已继续')
    refreshList()
  } catch (error) {
    ElMessage.error('继续失败')
  }
}

// 删除任务
const deleteTask = async (taskId) => {
  try {
    await ElMessageBox.confirm('确定要删除该任务吗？', '提示', { type: 'warning' })
    await savedMessagesApi.deleteTask(taskId)
    ElMessage.success('已删除')
    refreshList()
    refreshStats()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

// 全部暂停
const pauseAll = async () => {
  try {
    for (const task of pendingList.value) {
      if (task.isRunning) {
        await savedMessagesApi.pauseTask(task.id)
      }
    }
    ElMessage.success('已暂停所有任务')
    refreshList()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 全部继续
const resumeAll = async () => {
  try {
    for (const task of pendingList.value) {
      if (!task.isRunning && task.status !== 'SUCCESS_DOWNLOAD') {
        await savedMessagesApi.resumeTask(task.id)
      }
    }
    ElMessage.success('已继续所有任务')
    refreshList()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 清除已完成
const clearCompleted = async () => {
  try {
    await ElMessageBox.confirm('确定要清除已下载的文件记录吗？', '提示', { type: 'warning' })
    await savedMessagesApi.clearCompleted()
    ElMessage.success('已清除')
    refreshList()
    refreshStats()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('清除失败')
  }
}

onMounted(() => {
  refreshList()
  refreshStats()
  refreshSpeed()
  
  refreshTimer = setInterval(() => {
    refreshList()
    refreshStats()
  }, 5000)
  
  speedTimer = setInterval(refreshSpeed, 3000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (speedTimer) clearInterval(speedTimer)
})
</script>

<style scoped>
.saved-messages-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar .left {
  display: flex;
  gap: 8px;
}

.stats-info {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e6e6e6;
}

.stat-success {
  color: #67c23a;
  margin-right: 4px;
}

.stat-warning {
  color: #e6a23c;
  margin-right: 4px;
}

.speed-active {
  color: #67c23a;
  font-weight: bold;
}

.save-path {
  color: #606266;
  font-family: monospace;
  font-size: 12px;
}
</style>
