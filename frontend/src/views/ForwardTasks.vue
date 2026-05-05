<template>
  <div class="forward-tasks-page">
    <!-- 顶部统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="4">
        <el-card class="stat-card">
          <div class="stat-value">{{ stats.total }}</div>
          <div class="stat-label">总任务</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="stat-card success">
          <div class="stat-value">{{ stats.success }}</div>
          <div class="stat-label">已完成</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="stat-card warning">
          <div class="stat-value">{{ stats.pending }}</div>
          <div class="stat-label">等待中</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="stat-card primary">
          <div class="stat-value">{{ stats.forwarding }}</div>
          <div class="stat-label">转发中</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="stat-card danger">
          <div class="stat-value">{{ stats.failed }}</div>
          <div class="stat-label">失败</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="stat-card info">
          <div class="stat-value">{{ stats.auto }}</div>
          <div class="stat-label">自动转发</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 操作栏 -->
    <el-card class="toolbar-card">
      <div class="toolbar">
        <div class="left">
          <el-button type="primary" @click="refreshList" :loading="loading">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
          <el-button @click="pauseAll" type="warning">
            <el-icon><VideoPause /></el-icon>
            全部暂停
          </el-button>
          <el-button @click="resumeAll" type="success">
            <el-icon><VideoPlay /></el-icon>
            全部继续
          </el-button>
          <el-button @click="retryFailed" type="primary" plain>
            <el-icon><RefreshRight /></el-icon>
            重试失败
          </el-button>
          <el-button @click="clearCompleted" type="danger" plain>
            <el-icon><Delete /></el-icon>
            清除已完成
          </el-button>
        </div>
        
        <div class="right">
          <el-radio-group v-model="filterType" size="small" @change="refreshList">
            <el-radio-button label="all">全部</el-radio-button>
            <el-radio-button label="manual">手动</el-radio-button>
            <el-radio-button label="auto">自动</el-radio-button>
          </el-radio-group>
          <el-select v-model="filterStatus" placeholder="状态筛选" clearable size="small" @change="refreshList" style="width: 120px; margin-left: 8px;">
            <el-option label="等待中" value="PENDING" />
            <el-option label="转发中" value="FORWARDING" />
            <el-option label="已完成" value="SUCCESS" />
            <el-option label="失败" value="FAILED" />
          </el-select>
        </div>
      </div>
    </el-card>

    <!-- 转发任务列表 -->
    <el-card>
      <el-table :data="taskList" v-loading="loading" stripe :default-sort="{ prop: 'createdAt', order: 'descending' }">
        <el-table-column type="index" width="50" />
        
        <el-table-column label="来源" min-width="200">
          <template #default="{ row }">
            <div class="chat-info">
              <el-icon><ChatDotRound /></el-icon>
              <div class="chat-detail">
                <div class="chat-title">{{ row.sourceChatTitle || '未知聊天' }}</div>
                <div class="chat-id">ID: {{ row.sourceChatId }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column label="目标" min-width="200">
          <template #default="{ row }">
            <div class="chat-info">
              <el-icon><Right /></el-icon>
              <div class="chat-detail">
                <div class="chat-title">{{ row.targetChatTitle || '未知聊天' }}</div>
                <div class="chat-id">ID: {{ row.targetChatId }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="messageId" label="消息ID" width="100" />
        
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isAutoForward ? 'success' : 'primary'" size="small">
              {{ row.isAutoForward ? '自动' : '手动' }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="错误信息" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.errorMessage" class="error-msg">{{ row.errorMessage }}</span>
            <span v-else class="no-error">-</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="200" fixed="right">
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
                v-if="row.status === 'FAILED'"
                type="primary"
                size="small"
                @click="retryTask(row.id)"
              >
                重试
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
      
      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @size-change="refreshList"
          @current-change="refreshList"
        />
      </div>
      
      <el-empty v-if="taskList.length === 0 && !loading" description="暂无转发任务" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { forwardApi } from '@/api/forward'

const loading = ref(false)
const taskList = ref([])
const filterType = ref('all')
const filterStatus = ref('')
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

const stats = reactive({
  total: 0,
  success: 0,
  pending: 0,
  forwarding: 0,
  failed: 0,
  auto: 0
})

let refreshTimer = null

// 获取状态类型
const getStatusType = (status) => {
  const map = {
    'SUCCESS': 'success',
    'FAILED': 'danger',
    'PENDING': 'info',
    'FORWARDING': 'warning',
    'STOPPED': 'info'
  }
  return map[status] || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
  const map = {
    'SUCCESS': '已完成',
    'FAILED': '失败',
    'PENDING': '等待中',
    'FORWARDING': '转发中',
    'STOPPED': '已停止'
  }
  return map[status] || status
}

// 判断是否可以暂停
const canPause = (row) => {
  return row.status === 'FORWARDING'
}

// 判断是否可以继续
const canResume = (row) => {
  return row.status === 'PENDING' || row.status === 'STOPPED' || row.status === 'FAILED'
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return '-'
  const date = new Date(time)
  return date.toLocaleString('zh-CN')
}

// 刷新列表
const refreshList = async () => {
  loading.value = true
  try {
    let data
    if (filterType.value === 'manual') {
      data = await forwardApi.getManualTasks({ page: page.value - 1, size: pageSize.value })
    } else if (filterType.value === 'auto') {
      data = await forwardApi.getAutoTasks({ page: page.value - 1, size: pageSize.value })
    } else {
      data = await forwardApi.getTasks({ page: page.value - 1, size: pageSize.value })
    }
    
    // 如果有状态筛选，前端过滤
    if (filterStatus.value) {
      taskList.value = (data.content || []).filter(t => t.status === filterStatus.value)
    } else {
      taskList.value = data.content || []
    }
    total.value = data.totalElements || 0
    
    // 刷新统计
    await refreshStats()
  } catch (error) {
    ElMessage.error('刷新失败')
  } finally {
    loading.value = false
  }
}

// 刷新统计
const refreshStats = async () => {
  try {
    const statsData = await forwardApi.getStatistics()
    stats.total = statsData.total || 0
    stats.success = statsData.success || 0
    stats.pending = statsData.pending || 0
    stats.forwarding = statsData.forwarding || 0
    stats.failed = statsData.failed || 0
    stats.auto = statsData.auto || 0
  } catch (error) {
    console.error('获取统计失败:', error)
  }
}

// 全部暂停
const pauseAll = async () => {
  try {
    await forwardApi.pauseAll()
    ElMessage.success('已暂停所有任务')
    refreshList()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 全部继续
const resumeAll = async () => {
  try {
    await forwardApi.resumeAll()
    ElMessage.success('已恢复所有任务')
    refreshList()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 重试失败
const retryFailed = async () => {
  try {
    await forwardApi.retryFailed()
    ElMessage.success('已重试失败任务')
    refreshList()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 清除已完成
const clearCompleted = async () => {
  try {
    await ElMessageBox.confirm('确定要清除已完成的任务吗？', '提示', { type: 'warning' })
    await forwardApi.clearCompleted()
    ElMessage.success('已清除')
    refreshList()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('清除失败')
  }
}

// 暂停任务
const pauseTask = async (taskId) => {
  try {
    await forwardApi.pauseTask(taskId)
    ElMessage.success('已暂停')
    refreshList()
  } catch (error) {
    ElMessage.error('暂停失败')
  }
}

// 继续任务
const resumeTask = async (taskId) => {
  try {
    await forwardApi.resumeTask(taskId)
    ElMessage.success('已继续')
    refreshList()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 重试任务
const retryTask = async (taskId) => {
  try {
    await forwardApi.retryTask(taskId)
    ElMessage.success('已重试')
    refreshList()
  } catch (error) {
    ElMessage.error('重试失败')
  }
}

// 删除任务
const deleteTask = async (taskId) => {
  try {
    await ElMessageBox.confirm('确定要删除该任务吗？', '提示', { type: 'warning' })
    await forwardApi.deleteTask(taskId)
    ElMessage.success('已删除')
    refreshList()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

onMounted(() => {
  refreshList()
  refreshTimer = setInterval(refreshList, 5000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})
</script>

<style scoped>
.forward-tasks-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stats-row {
  margin-bottom: 0;
}

.stat-card {
  text-align: center;
}

.stat-card.success .stat-value {
  color: #67c23a;
}

.stat-card.warning .stat-value {
  color: #e6a23c;
}

.stat-card.danger .stat-value {
  color: #f56c6c;
}

.stat-card.primary .stat-value {
  color: #409eff;
}

.stat-card.info .stat-value {
  color: #909399;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #606266;
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

.chat-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chat-detail {
  display: flex;
  flex-direction: column;
}

.chat-title {
  font-weight: 500;
}

.chat-id {
  font-size: 12px;
  color: #909399;
}

.error-msg {
  color: #f56c6c;
}

.no-error {
  color: #909399;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
