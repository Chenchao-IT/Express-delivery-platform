import request from './request'

export const getMyWallet = () => request.get('/wallet/me')

