import request from './request'

export const login = (data) => request.post('/auth/login', data)
export const register = (data) => request.post('/auth/register', data)
export const getMe = () => request.get('/auth/me')
/** 清空登录限流（开发/调试用） */
export const clearRateLimit = () => request.post('/auth/rate-limit/clear')