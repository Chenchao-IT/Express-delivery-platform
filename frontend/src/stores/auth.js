import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, register as registerApi } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const isCourier = computed(() => user.value?.role === 'COURIER')
  const isStudent = computed(() => user.value?.role === 'STUDENT')

  const login = async (credentials) => {
    const res = await loginApi(credentials)
    token.value = res.token
    // 文档 2.2：支持正常登录、非关键冲突(hasConflicts)、关键冲突需申诉(conflictResolutionRequired)
    user.value = {
      username: res.username,
      role: res.role,
      realName: res.realName,
      hasConflicts: res.hasConflicts ?? false,
      conflictResolutionRequired: res.conflictResolutionRequired ?? false,
      shadowId: res.shadowId,
      minimalPermissions: res.minimalPermissions,
      authMessage: res.message,
    }
    localStorage.setItem('token', res.token)
    localStorage.setItem('user', JSON.stringify(user.value))
    return res
  }

  const register = async (data) => {
    const res = await registerApi(data)
    token.value = res.token
    user.value = {
      username: res.username,
      role: res.role,
      realName: res.realName,
    }
    localStorage.setItem('token', res.token)
    localStorage.setItem('user', JSON.stringify(user.value))
    return res
  }

  const logout = () => {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return {
    token,
    user,
    isLoggedIn,
    isAdmin,
    isCourier,
    isStudent,
    login,
    register,
    logout,
  }
})
