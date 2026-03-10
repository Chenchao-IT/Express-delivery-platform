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

