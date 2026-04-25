import request from './request'

export const listNotifications = () => request.get('/notifications/history')
export const readAllNotifications = () => request.put('/notifications/read-all')

export const openNotificationStream = () => {
  const baseURL = request.defaults?.baseURL || '/api'
  const token = localStorage.getItem('token')
  const url = `${baseURL}/notifications/stream${token ? `?token=${encodeURIComponent(token)}` : ''}`
  return new EventSource(url, { withCredentials: false })
}
