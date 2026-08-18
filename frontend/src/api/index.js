import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'
import router from '../router'

/**
 * 统一请求层：
 * - baseURL 走 Vite 代理，无跨域问题
 * - 请求自动带上 JWT（Authorization: Bearer xxx）
 * - 响应自动解包 Result：code!=200 统一提示；401 自动登出并跳登录页
 */
const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

http.interceptors.request.use((config) => {
  const store = useUserStore()
  if (store.token) {
    config.headers.Authorization = `Bearer ${store.token}`
  }
  return config
})

http.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body.code !== 200) {
      if (body.code === 401) {
        const store = useUserStore()
        store.logout()
        router.push('/login')
      }
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message))
    }
    return body.data
  },
  (err) => {
    ElMessage.error('网络异常，请检查后端是否已启动')
    return Promise.reject(err)
  }
)

export default http
