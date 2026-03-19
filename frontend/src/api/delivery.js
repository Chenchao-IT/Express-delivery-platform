import request from './request'

export const listDeliveries = (params) => request.get('/deliveries', { params })
export const listPendingDeliveries = () => request.get('/deliveries/pending')
export const listMyDeliveries = () => request.get('/deliveries/my')
export const createDelivery = (data) => request.post('/deliveries', data)
export const grabDelivery = (id) => request.put(`/deliveries/${id}/grab`)
export const startDelivery = (id) => request.put(`/deliveries/${id}/start`)
/** 学生端预约配送 */
export const scheduleDelivery = (packageId, destination) =>
  request.post('/deliveries/schedule', { packageId, destination })
export const assignCourier = (id, courierId) => request.put(`/deliveries/${id}/assign`, { courierId })
export const completeDelivery = (id) => request.put(`/deliveries/${id}/complete`)
export const getDestinations = () => request.get('/deliveries/destinations')

// =========================
// 悬赏代取（学生发布/学生或快递员接单）
// =========================
export const listPendingRewardTasks = () => request.get('/deliveries/reward/pending')
export const listMyRewardTasks = () => request.get('/deliveries/reward/my')
export const listPublishedRewardTasks = () => request.get('/deliveries/reward/published')
export const publishRewardTask = (packageId, destination, rewardAmount) =>
  request.post('/deliveries/reward/publish', { packageId, destination, rewardAmount })
export const acceptRewardTask = (id) => request.put(`/deliveries/reward/${id}/accept`)
export const cancelRewardTask = (id) => request.put(`/deliveries/reward/${id}/cancel`)
export const completeRewardTask = (id) => request.put(`/deliveries/reward/${id}/complete`)

