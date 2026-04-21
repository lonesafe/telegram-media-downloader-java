<template>
  <div class="downloads-page">
    <!-- 顶部操作栏 -->
    <el-card class="toolbar-card">
      <div class="toolbar">
        <div class="left">
          <el-button type="primary" @click="showAddDialog = true">
            <el-icon><Plus /></el-icon>
            添加链接
          </el-button>
          <el-button @click="toggleDownloadState" :type="isPaused ? 'success' : 'warning'">
            <el-icon v-if="isPaused"><VideoPlay /></el-icon>
            <el-icon v-else><VideoPause /></el-icon>
            {{ isPaused ? '继续全部' : '暂停全部' }}
          </el-button>
        </div>
        
        <div class="right">
          <el-button @click="clearCompleted" type="danger" plain :disabled="tabCounts.completedCount === 0">
            <el-icon><Delete /></el-icon>
            清除已完成
          </el-button>
        </div>
      </div>
      
      <!-- 下载速度统计 -->
      <div class="speed-info">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-statistic title="下载速度" :value="formatSpeedNum(globalSpeed.downloadSpeed)">
              <template #suffix>MB/s</template>
            </el-statistic>
          </el-col>
          <el-col :span="6">
            <el-statistic title="正在下载" :value="tabCounts.downloadingCount" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="等待中" :value="tabCounts.waitingCount" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="已完成" :value="tabCounts.completedCount" />
          </el-col>
        </el-row>
      </div>
    </el-card>

    <!-- 下载列表 - 三选项卡 -->
    <el-card class="list-card">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <!-- 正在下载 -->
        <el-tab-pane name="downloading">
          <template #label>
            <span class="tab-label">
              正在下载
              <el-badge :value="tabCounts.downloadingCount" :max="99" :hidden="tabCounts.downloadingCount === 0" />
            </span>
          </template>
        </el-tab-pane>
        
        <!-- 等待下载 -->
        <el-tab-pane name="waiting">
          <template #label>
            <span class="tab-label">
              等待下载
              <el-badge :value="tabCounts.waitingCount" :max="99" :hidden="tabCounts.waitingCount === 0" />
            </span>
          </template>
        </el-tab-pane>
        
        <!-- 下载完成 -->
        <el-tab-pane name="completed">
          <template #label>
            <span class="tab-label">
              下载完成
              <el-badge :value="tabCounts.completedCount" :max="99" :hidden="tabCounts.completedCount === 0" />
            </span>
          </template>
        </el-tab-pane>
      </el-tabs>
      
      <!-- 表格 -->
      <el-table
        :data="downloadList"
        style="width: 100%"
        v-loading="loading"
        stripe
      >
        <el-table-column type="index" width="50" />
        
        <el-table-column prop="fileName" label="文件名" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="file-info">
              <el-icon class="file-icon"><Document /></el-icon>
              <span class="file-name">{{ row.fileName || '未命名' }}</span>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column label="文件大小" width="120">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        
        <el-table-column label="已下载" width="120">
          <template #default="{ row }">
            {{ formatFileSize(row.downloadedSize) }}
          </template>
        </el-table-column>
        
        <el-table-column label="下载进度" width="180">
          <template #default="{ row }">
            <el-progress
              :percentage="row.downloadProgress || 0"
              :status="getProgressStatus(row)"
              :stroke-width="8"
            />
            <div class="progress-text">{{ row.downloadProgress || 0 }}%</div>
          </template>
        </el-table-column>
        
        <el-table-column label="速度" width="100">
          <template #default="{ row }">
            <span :class="{ 'speed-active': row.isRunning }">
              {{ getSpeedDisplay(row) }}
            </span>
          </template>
        </el-table-column>
        
        <el-table-column prop="chatTitle" label="来源" width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.chatTitle || row.chatId }}</el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="保存路径" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="save-path">{{ row.savePath || row.filePath || '-' }}</span>
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
      
      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="totalCount"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
      
      <!-- 空状态 -->
      <el-empty v-if="downloadList.length === 0 && !loading" :description="getEmptyText()" />
    </el-card>

    <!-- 添加链接对话框 -->
    <el-dialog v-model="showAddDialog" title="添加下载链接" width="600px">
      <el-form :model="addForm" label-width="100px" :rules="addRules" ref="addFormRef">
        <el-form-item label="链接" prop="link">
          <el-input
            v-model="addForm.link"
            type="textarea"
            :rows="4"
            placeholder="粘贴 Telegram 链接，支持：&#10;1. t.me/c/xxx/yyy 格式&#10;2. https://t.me/channel/123&#10;3. 多个链接用换行分隔"
          />
        </el-form-item>
        <el-form-item label="保存路径" prop="savePath">
          <el-input v-model="addForm.savePath" placeholder="留空使用默认路径">
            <template #append>
              <el-button @click="browsePath">浏览</el-button>
            </template>
          </el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="submitAdd" :loading="adding">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { downloadApi } from '@/api/download'

const loading = ref(false)
const adding = ref(false)
const downloadList = ref([])
const activeTab = ref('downloading') // downloading | waiting | completed
const isPaused = ref(false)
const showAddDialog = ref(false)
const addFormRef = ref(null)

// 分页
const currentPage = ref(1)
const pageSize = ref(20)
const totalCount = ref(0)

// 选项卡计数
const tabCounts = reactive({
  downloadingCount: 0,
  waitingCount: 0,
  completedCount: 0
})

// 实时速度映射（从 API 获取）
const taskSpeeds = ref({})

const globalSpeed = reactive({
  downloadSpeed: 0,
  uploadSpeed: 0
})

const addForm = reactive({
  link: '',
  savePath: ''
})

const addRules = {
  link: [{ required: true, message: '请输入链接', trigger: 'blur' }]
}

let refreshTimer = null
let listTimer = null

// 获取速度显示
const getSpeedDisplay = (row) => {
  if (taskSpeeds.value[row.id] !== undefined) {
    return formatSpeed(taskSpeeds.value[row.id])
  }
  return row.downloadSpeed || '0 B/s'
}

// 空状态文本
const getEmptyText = () => {
  const map = {
    downloading: '暂无正在下载的任务',
    waiting: '暂无等待下载的任务',
    completed: '暂无已完成的下载'
  }
  return map[activeTab.value] || '暂无任务'
}

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

// 格式化速度数值（用于 el-statistic）
const formatSpeedNum = (bytesPerSec) => {
  if (!bytesPerSec || bytesPerSec === 0) return 0
  if (bytesPerSec < 1024 * 1024) return parseFloat((bytesPerSec / 1024).toFixed(1))
  return parseFloat((bytesPerSec / 1024 / 1024).toFixed(2))
}

// 格式化速度字符串
const formatSpeed = (bytesPerSec) => {
  if (!bytesPerSec || bytesPerSec === 0) return '0 B/s'
  if (bytesPerSec < 1024) return bytesPerSec + ' B/s'
  if (bytesPerSec < 1024 * 1024) return (bytesPerSec / 1024).toFixed(1) + ' KB/s'
  if (bytesPerSec < 1024 * 1024 * 1024) return (bytesPerSec / 1024 / 1024).toFixed(2) + ' MB/s'
  return (bytesPerSec / 1024 / 1024 / 1024).toFixed(2) + ' GB/s'
}

// 获取进度条状态
const getProgressStatus = (row) => {
  if (row.status === 'SUCCESS_DOWNLOAD') return 'success'
  if (row.status === 'FAILED_DOWNLOAD') return 'exception'
  if (row.status === 'PAUSED') return 'warning'
  return ''
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

// 判断是否可以暂停
const canPause = (row) => {
  return row.isRunning && row.status === 'DOWNLOADING'
}

// 判断是否可以继续
const canResume = (row) => {
  return row.status === 'PAUSED' ||
         (!row.isRunning && row.status !== 'SUCCESS_DOWNLOAD' && row.status !== 'SKIP_DOWNLOAD')
}

// 刷新列表
const refreshList = async () => {
  loading.value = true
  try {
    const res = await downloadApi.getList(activeTab.value, currentPage.value, pageSize.value)
    downloadList.value = res.list || []
    totalCount.value = res.total || 0
    // 更新选项卡计数
    tabCounts.downloadingCount = res.downloadingCount || 0
    tabCounts.waitingCount = res.waitingCount || 0
    tabCounts.completedCount = res.completedCount || 0
  } catch (error) {
    ElMessage.error('刷新失败')
  } finally {
    loading.value = false
  }
}

// 刷新速度
const refreshSpeed = async () => {
  try {
    const speed = await downloadApi.getSpeed()
    globalSpeed.downloadSpeed = parseFloat(speed.download_speed) || 0
    globalSpeed.uploadSpeed = parseFloat(speed.upload_speed) || 0
    if (speed.task_speeds) {
      taskSpeeds.value = speed.task_speeds
    }
  } catch (error) {
    console.error('获取速度失败:', error)
  }
}

// 选项卡切换
const onTabChange = (tab) => {
  currentPage.value = 1
  refreshList()
}

// 分页处理
const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
  refreshList()
}

const handlePageChange = (val) => {
  currentPage.value = val
  refreshList()
}

// 切换下载状态
const toggleDownloadState = async () => {
  try {
    const newState = isPaused.value ? 'continue' : 'pause'
    await downloadApi.setState(newState)
    isPaused.value = !isPaused.value
    ElMessage.success(isPaused.value ? '已暂停全部' : '已继续全部')
    // 切换后刷新列表
    setTimeout(refreshList, 500)
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 清除已完成
const clearCompleted = async () => {
  try {
    await ElMessageBox.confirm('确定要清除已完成的任务吗？', '提示', { type: 'warning' })
    await downloadApi.clearCompleted()
    ElMessage.success('已清除')
    refreshList()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('清除失败')
  }
}

// 暂停任务
const pauseTask = async (taskId) => {
  try {
    await downloadApi.stopTask(taskId)
    ElMessage.success('已暂停')
    refreshList()
  } catch (error) {
    ElMessage.error('暂停失败')
  }
}

// 继续任务
const resumeTask = async (taskId) => {
  try {
    await downloadApi.resumeTask(taskId)
    ElMessage.success('已恢复下载')
    refreshList()
  } catch (error) {
    ElMessage.error('恢复失败')
  }
}

// 删除任务
const deleteTask = async (taskId) => {
  try {
    await ElMessageBox.confirm('确定要删除该任务吗？', '提示', { type: 'warning' })
    await downloadApi.deleteTask(taskId)
    ElMessage.success('已删除')
    refreshList()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

// 浏览路径
const browsePath = () => {
  ElMessage.info('文件选择功能需要后端支持')
}

// 提交添加
const submitAdd = async () => {
  if (!addFormRef.value) return

  await addFormRef.value.validate(async (valid) => {
    if (!valid) return

    adding.value = true
    try {
      const links = addForm.link.split('\n').filter(l => l.trim())
      for (const link of links) {
        await downloadApi.createTask({
          chatLink: link.trim(),
          savePath: addForm.savePath
        })
      }
      ElMessage.success(`成功添加 ${links.length} 个任务`)
      showAddDialog.value = false
      addForm.link = ''
      addForm.savePath = ''
      refreshList()
    } catch (error) {
      ElMessage.error('添加失败: ' + (error.message || '未知错误'))
    } finally {
      adding.value = false
    }
  })
}

onMounted(() => {
  refreshList()
  refreshSpeed()
  // 每秒刷新速度
  refreshTimer = setInterval(refreshSpeed, 1000)
  // 每3秒刷新列表
  listTimer = setInterval(refreshList, 3000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (listTimer) clearInterval(listTimer)
})
</script>

<style scoped>
.downloads-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar-card {
  margin-bottom: 0;
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

.speed-info {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e6e6e6;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-icon {
  color: #909399;
  font-size: 18px;
}

.file-name {
  font-weight: 500;
}

.progress-text {
  font-size: 12px;
  color: #909399;
  text-align: center;
  margin-top: 4px;
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

.list-card {
  flex: 1;
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 6px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}
</style>
