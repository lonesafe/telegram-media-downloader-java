import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器 - 兼容多种响应格式
request.interceptors.response.use(
  response => {
    const res = response.data
    // 兼容多种响应格式:
    // 1. { success: true, ... }
    // 2. { code: 1, ... }
    // 3. { code: 200, ... }
    const isSuccess = res.success === true || res.code === 1 || res.code === 200
    if (!isSuccess) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res.data
  },
  error => {
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
