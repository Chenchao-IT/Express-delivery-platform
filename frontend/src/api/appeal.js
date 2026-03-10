import request from './request'

/** 文档 2.2：提交身份申诉 */
export const submitAppeal = (data) => request.post('/auth/appeal', data)

/** 我的申诉列表 */
export const getMyAppeals = () => request.get('/auth/appeals/me')

/** 管理员：待审核申诉列表 */
export const getPendingAppeals = () => request.get('/admin/appeals')

/** 管理员：审核申诉 */
export const reviewAppeal = (id, body) => request.post(`/admin/appeals/${id}/review`, body)
