import request from './request'

export const listUsers = (params) => request.get('/users', { params })
export const listCouriers = () => request.get('/users/couriers')
