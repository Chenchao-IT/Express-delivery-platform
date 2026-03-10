import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    // 登录页不处理 401 跳转，避免取消请求或重定向循环
    if (error.response?.status === 401) {
      const path = window.location.pathname
      if (!path.includes('/login') && !path.includes('/register')) {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        window.location.href = '/login'
      }
    }
    // 请求被取消（如页面导航、重复提交）
    if (error.code === 'ERR_CANCELED' || error.message?.includes('cancel')) {
      return Promise.reject(new Error('请求已取消，请勿重复提交'))
    }
    // 网络错误
    if (!error.response) {
      const networkMsg = error.message?.includes('Network Error')
        ? '无法连接服务器，请确认后端已启动 (http://localhost:8080)'
        : (error.message || '网络异常')
      return Promise.reject(new Error(networkMsg))
    }
    const data = error.response?.data
    const msg = (data && typeof data === 'object' && data.message) ? data.message : (data?.error || error.message)
    return Promise.reject(new Error(typeof msg === 'string' ? msg : '请求失败'))
  }
)

export default request
