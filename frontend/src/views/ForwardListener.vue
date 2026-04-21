<template>
  <div class="forward-listener-page">
    <!-- 顶部操作栏 -->
    <el-card class="toolbar-card">
      <div class="toolbar">
        <div class="left">
          <el-button type="primary" @click="showAddDialog = true">
            <el-icon><Plus /></el-icon>
            添加监听规则
          </el-button>
          <el-button @click="refreshList" :loading="loading">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
        
        <div class="right">
          <el-switch
            v-model="listenerEnabled"
            active-text="监听已开启"
            inactive-text="监听已关闭"
            @change="toggleListener"
          />
        </div>
      </div>
      
      <!-- 统计信息 -->
      <div class="stats-info">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-statistic title="监听规则数" :value="listenerList.length" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="今日触发" :value="todayTriggered" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="累计转发" :value="totalForwarded" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="监听状态" :value="listenerEnabled ? '运行中' : '已停止'">
              <template #prefix>
                <el-icon :class="listenerEnabled ? 'status-running' : 'status-stopped'">
                  <CircleCheck v-if="listenerEnabled" />
                  <CircleClose v-else />
                </el-icon>
              </template>
            </el-statistic>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <!-- 监听规则列表 -->
    <el-card>
      <el-table :data="listenerList" v-loading="loading" stripe>
        <el-table-column type="index" width="50" />
        
        <el-table-column prop="name" label="规则名称" min-width="150">
          <template #default="{ row }">
            <el-input
              v-if="row.editing"
              v-model="row.editName"
              size="small"
              @blur="saveEdit(row)"
              @keyup.enter="saveEdit(row)"
            />
            <span v-else class="rule-name" @click="startEdit(row)">
              {{ row.name || '未命名规则' }}
              <el-icon class="edit-icon"><Edit /></el-icon>
            </span>
          </template>
        </el-table-column>
        
        <el-table-column label="源聊天" min-width="200">
          <template #default="{ row }">
            <div class="chat-list">
              <el-tag
                v-for="chat in row.sourceChats"
                :key="chat.id"
                size="small"
                closable
                @close="removeSourceChat(row, chat.id)"
              >
                {{ chat.title || chat.id }}
              </el-tag>
              <el-button
                v-if="!row.addingSource"
                size="small"
                type="primary"
                link
                @click="row.addingSource = true"
              >
                <el-icon><Plus /></el-icon>
              </el-button>
              <el-input-number
                v-else
                v-model="row.newSourceId"
                size="small"
                :controls="false"
                placeholder="输入Chat ID"
                style="width: 120px"
                @blur="row.addingSource = false"
                @keyup.enter="addSourceChat(row)"
              />
            </div>
          </template>
        </el-table-column>
        
        <el-table-column label="目标聊天" width="200">
          <template #default="{ row }">
            <el-tag v-if="row.targetChat" type="success" size="small">
              {{ row.targetChat.title || row.targetChat.id }}
            </el-tag>
            <el-button v-else size="small" type="primary" link @click="setTargetChat(row)">
              设置目标
            </el-button>
          </template>
        </el-table-column>
        
        <el-table-column label="过滤类型" width="200">
          <template #default="{ row }">
            <el-checkbox-group v-model="row.filterTypes" size="small">
              <el-checkbox label="video">视频</el-checkbox>
              <el-checkbox label="photo">图片</el-checkbox>
              <el-checkbox label="audio">音频</el-checkbox>
              <el-checkbox label="text">文本</el-checkbox>
            </el-checkbox-group>
          </template>
        </el-table-column>
        
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" @change="updateRule(row)" />
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button-group>
              <el-button type="primary" size="small" @click="viewLogs(row)">日志</el-button>
              <el-button type="danger" size="small" @click="deleteRule(row)">删除</el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>
      
      <el-empty v-if="listenerList.length === 0 && !loading" description="暂无监听规则" />
    </el-card>

    <!-- 添加规则对话框 -->
    <el-dialog v-model="showAddDialog" title="添加监听规则" width="600px">
      <el-form :model="addForm" label-width="100px" :rules="addRules" ref="addFormRef">
        <el-form-item label="规则名称" prop="name">
          <el-input v-model="addForm.name" placeholder="给规则起个名字" />
        </el-form-item>
        
        <el-form-item label="源聊天" prop="sourceChatIds">
          <div class="chat-inputs">
            <el-tag
              v-for="id in addForm.sourceChatIds"
              :key="id"
              closable
              @close="removeAddSource(id)"
            >
              {{ id }}
            </el-tag>
            <el-input-number
              v-model="addForm.newSourceId"
              :controls="false"
              placeholder="输入Chat ID按回车"
              style="width: 150px"
              @keyup.enter="addSourceToForm"
            />
          </div>
          <div class="form-tip">可以添加多个源聊天，监听到任一聊天的消息都会触发转发</div>
        </el-form-item>
        
        <el-form-item label="目标聊天" prop="targetChatId">
          <el-input-number v-model="addForm.targetChatId" :controls="false" placeholder="转发到哪个聊天" style="width: 200px" />
          <div class="form-tip">消息将被转发到此聊天ID</div>
        </el-form-item>
        
        <el-form-item label="过滤类型">
          <el-checkbox-group v-model="addForm.filterTypes">
            <el-checkbox label="video">视频</el-checkbox>
            <el-checkbox label="photo">图片</el-checkbox>
            <el-checkbox label="audio">音频</el-checkbox>
            <el-checkbox label="text">文本</el-checkbox>
            <el-checkbox label="document">文档</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="submitAdd" :loading="adding">添加</el-button>
      </template>
    </el-dialog>

    <!-- 日志对话框 -->
    <el-dialog v-model="showLogDialog" title="转发日志" width="800px">
      <el-timeline>
        <el-timeline-item
          v-for="log in currentLogs"
          :key="log.id"
          :type="log.type"
          :timestamp="log.time"
        >
          {{ log.message }}
        </el-timeline-item>
      </el-timeline>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { forwardApi } from '@/api/forward'

const loading = ref(false)
const adding = ref(false)
const listenerEnabled = ref(false)
const listenerList = ref([])
const todayTriggered = ref(0)
const totalForwarded = ref(0)
const showAddDialog = ref(false)
const showLogDialog = ref(false)
const currentLogs = ref([])
const addFormRef = ref(null)

const addForm = reactive({
  name: '',
  sourceChatIds: [],
  newSourceId: null,
  targetChatId: null,
  filterTypes: ['video', 'photo', 'audio', 'text']
})

const addRules = {
  name: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  targetChatId: [{ required: true, message: '请输入目标聊天ID', trigger: 'blur' }]
}

// 刷新列表
const refreshList = async () => {
  loading.value = true
  try {
    const config = await forwardApi.getListenerConfig()
    listenerEnabled.value = config.isRunning
    
    // 转换后端数据为前端格式
    listenerList.value = (config.rules || []).map(rule => ({
      id: rule.id,
      name: rule.name,
      sourceChats: rule.sourceChatIds?.map(id => ({ id, title: null })) || [],
      targetChat: rule.targetChatId ? { id: rule.targetChatId, title: null } : null,
      filterTypes: rule.filterTypes || ['video', 'photo', 'audio', 'text'],
      enabled: rule.enabled !== false,
      editing: false,
      editName: rule.name,
      addingSource: false,
      newSourceId: null
    }))
    
    todayTriggered.value = config.todayTriggered || 0
    totalForwarded.value = config.totalForwarded || 0
  } catch (error) {
    ElMessage.error('刷新失败')
  } finally {
    loading.value = false
  }
}

// 切换监听状态
const toggleListener = async () => {
  try {
    if (listenerEnabled.value) {
      await forwardApi.startListener()
      ElMessage.success('监听已启动')
    } else {
      await forwardApi.stopListener()
      ElMessage.success('监听已停止')
    }
  } catch (error) {
    ElMessage.error('操作失败')
    listenerEnabled.value = !listenerEnabled.value
  }
}

// 开始编辑名称
const startEdit = (row) => {
  row.editing = true
  row.editName = row.name
}

// 保存编辑
const saveEdit = async (row) => {
  row.editing = false
  if (row.editName !== row.name) {
    row.name = row.editName
    await updateRule(row)
  }
}

// 添加源聊天到表单
const addSourceToForm = () => {
  if (addForm.newSourceId && !addForm.sourceChatIds.includes(addForm.newSourceId)) {
    addForm.sourceChatIds.push(addForm.newSourceId)
    addForm.newSourceId = null
  }
}

// 从表单移除源
const removeAddSource = (id) => {
  const idx = addForm.sourceChatIds.indexOf(id)
  if (idx > -1) addForm.sourceChatIds.splice(idx, 1)
}

// 添加源聊天
const addSourceChat = async (row) => {
  if (!row.newSourceId) return
  try {
    await forwardApi.addSourceChat(row.newSourceId)
    row.sourceChats.push({ id: row.newSourceId, title: null })
    row.newSourceId = null
    row.addingSource = false
    ElMessage.success('已添加')
  } catch (error) {
    ElMessage.error('添加失败')
  }
}

// 移除源聊天
const removeSourceChat = async (row, chatId) => {
  try {
    await forwardApi.removeSourceChat(chatId)
    row.sourceChats = row.sourceChats.filter(c => c.id !== chatId)
    ElMessage.success('已移除')
  } catch (error) {
    ElMessage.error('移除失败')
  }
}

// 设置目标聊天
const setTargetChat = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入目标聊天ID', '设置目标', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPattern: /^-?\d+$/,
      inputErrorMessage: '请输入有效的数字ID'
    })
    const targetId = parseInt(value)
    await forwardApi.setTargetChat(targetId)
    row.targetChat = { id: targetId, title: null }
    ElMessage.success('已设置')
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('设置失败')
  }
}

// 更新规则
const updateRule = async (row) => {
  try {
    await forwardApi.updateListenerConfig({
      id: row.id,
      name: row.name,
      sourceChatIds: row.sourceChats.map(c => c.id),
      targetChatId: row.targetChat?.id,
      filterTypes: row.filterTypes,
      enabled: row.enabled
    })
    ElMessage.success('已更新')
  } catch (error) {
    ElMessage.error('更新失败')
  }
}

// 提交添加
const submitAdd = async () => {
  if (!addFormRef.value) return
  await addFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    adding.value = true
    try {
      await forwardApi.updateListenerConfig({
        name: addForm.name,
        sourceChatIds: addForm.sourceChatIds,
        targetChatId: addForm.targetChatId,
        filterTypes: addForm.filterTypes,
        enabled: true
      })
      ElMessage.success('规则已添加')
      showAddDialog.value = false
      addForm.name = ''
      addForm.sourceChatIds = []
      addForm.targetChatId = null
      addForm.filterTypes = ['video', 'photo', 'audio', 'text']
      refreshList()
    } catch (error) {
      ElMessage.error('添加失败')
    } finally {
      adding.value = false
    }
  })
}

// 查看日志
const viewLogs = async (row) => {
  try {
    const logs = await forwardApi.getLogs(row.id)
    currentLogs.value = logs.map(l => ({
      id: l.id,
      type: l.success ? 'success' : 'danger',
      time: l.createdAt,
      message: l.message
    }))
    showLogDialog.value = true
  } catch (error) {
    ElMessage.error('获取日志失败')
  }
}

// 删除规则
const deleteRule = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该规则吗？', '提示', { type: 'warning' })
    await forwardApi.deleteRule(row.id)
    ElMessage.success('已删除')
    refreshList()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

onMounted(() => {
  refreshList()
})
</script>

<style scoped>
.forward-listener-page {
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

.status-running {
  color: #67c23a;
}

.status-stopped {
  color: #f56c6c;
}

.rule-name {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
}

.rule-name:hover .edit-icon {
  opacity: 1;
}

.edit-icon {
  opacity: 0;
  font-size: 12px;
  color: #909399;
  transition: opacity 0.2s;
}

.chat-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
}

.chat-inputs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
