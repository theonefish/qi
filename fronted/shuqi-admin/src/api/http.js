// axios 封装：自动携带 Sa-Token、Result 解包、401 回登录页
import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

export const BASE_URL = 'http://localhost:8080'

/** 相对文件地址（/files/...）转完整地址 */
export function fullUrl(path) {
  if (!path) return ''
  return path.indexOf('http') === 0 ? path : BASE_URL + path
}

export function getToken() {
  return localStorage.getItem('shuqi_admin_token') || ''
}

export function getAdminName() {
  return localStorage.getItem('shuqi_admin_name') || '管理员'
}

export function clearSession() {
  localStorage.removeItem('shuqi_admin_token')
  localStorage.removeItem('shuqi_admin_name')
}

const http = axios.create({
  baseURL: BASE_URL,
  timeout: 15000
})

http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) config.headers.Authorization = token
  return config
})

http.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body && body.code === 0) return body.data
    const msg = (body && body.msg) || '请求失败'
    if (body && body.code === 401) {
      clearSession()
      router.push('/login')
    }
    ElMessage.error(msg)
    return Promise.reject(new Error(msg))
  },
  () => {
    ElMessage.error('网络连接失败，请确认后端服务已启动')
    return Promise.reject(new Error('network error'))
  }
)

export default http
