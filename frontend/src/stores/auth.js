import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { login as loginApi, register as registerApi } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const isCourier = computed(() => user.value?.role === 'COURIER')
  const isStudent = computed(() => user.value?.role === 'STUDENT')

  async function login(credentials) {
    const res = await loginApi(credentials)
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

  async function register(data) {
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

  function logout() {
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
