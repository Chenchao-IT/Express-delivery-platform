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
  (error) => Promise.reject(error),
)

request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const status = error.response?.status
    const disable403Redirect = import.meta.env.DEV && localStorage.getItem('debug:disable403Redirect') === '1'

    if (status === 401) {
      const path = window.location.pathname
      if (!path.includes('/login') && !path.includes('/register')) {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        window.location.href = '/login'
      }
    }

    if (status === 403) {
      if (disable403Redirect) {
        console.warn('[debug] 403 redirect disabled:', {
          url: error.config?.url,
          method: error.config?.method,
          data: error.config?.data,
        })
        return Promise.reject(error)
      }
      const path = window.location.pathname
      if (!path.includes('/403')) {
        window.location.href = '/403'
      }
    }

    if (error.code === 'ERR_CANCELED' || error.message?.includes('cancel')) {
      return Promise.reject(new Error('请求已取消，请勿重复提交'))
    }

    if (!error.response) {
      const networkMsg = error.message?.includes('Network Error')
        ? '无法连接后端服务，请确认 http://localhost:8080 已启动'
        : (error.message || '网络异常')
      return Promise.reject(new Error(networkMsg))
    }

    const data = error.response?.data
    const msg = (data && typeof data === 'object' && data.message)
      ? data.message
      : (data?.error || error.message)

    return Promise.reject(new Error(typeof msg === 'string' ? msg : '请求失败'))
  },
)

export default request
