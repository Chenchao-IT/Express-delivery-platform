import request from './request'

export const getMyPackages = () => request.get('/packages/my')
export const trackPackage = (trackingNumber) => request.get(`/packages/track/${trackingNumber}`)
export const pickupPackage = (id) => request.post(`/packages/${id}/pickup`)
export const listPackages = (params) => request.get('/packages', { params })
export const createPackage = (data) => request.post('/packages', data)
