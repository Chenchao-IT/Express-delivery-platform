import request from './request'

export const openNotificationStream = () => {
  const baseURL = request.defaults?.baseURL || ''
  const url = `${baseURL}/notifications/stream`
  // EventSource 不支持自定义 header；本项目 token 存 localStorage 且 request.js 可能走 header
  // 这里用最小落地：依赖后端同源 cookie/代理；若不通，可后续改为 SSE + token query。
  return new EventSource(url, { withCredentials: true })
}

