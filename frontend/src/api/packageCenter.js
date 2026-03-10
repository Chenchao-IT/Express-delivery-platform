import request from './request'

/** 包裹中心 BFF 聚合数据（文档 2.1） */
export const getHomeData = () => request.get('/package-center/home')

/** 验证合并取件合法性（文档 1.1） */
export const validateMerge = (packageIds) =>
  request.post('/package-center/merge/validate', packageIds)

/** 执行合并取件（文档 1.1） */
export const executeMergedPickup = (packageIds) =>
  request.post('/package-center/merge/pickup', packageIds)

/** 成就进度（文档 1.3） */
export const getAchievements = () => request.get('/package-center/achievements')
