<template>
  <div class="min-h-screen bg-gradient-to-br from-slate-100 via-white to-blue-50 px-4 py-10">
    <div class="mx-auto grid max-w-6xl gap-8 lg:grid-cols-[1.1fr_0.9fr]">
      <section class="rounded-[32px] bg-slate-900 p-8 text-white shadow-2xl">
        <p class="inline-flex rounded-full bg-white/10 px-3 py-1 text-xs font-medium tracking-[0.2em]">
          SMART CAMPUS LOGISTICS
        </p>
        <h1 class="mt-6 text-4xl font-bold leading-tight">
          高效、安全、可追踪的
          <span class="block text-blue-300">校园快递配送入口</span>
        </h1>
        <p class="mt-4 max-w-2xl text-sm leading-7 text-slate-300">
          登录后可进入包裹中心、物流追踪、悬赏代取、模拟扫码入库、配送调度与管理大屏等论文核心模块。
        </p>

        <div class="mt-10 grid gap-4 sm:grid-cols-2">
          <div class="rounded-3xl border border-white/10 bg-white/5 p-5">
            <p class="text-xs text-slate-400">角色</p>
            <p class="mt-3 text-lg font-semibold">学生 / 代取员 / 管理员</p>
          </div>
          <div class="rounded-3xl border border-white/10 bg-white/5 p-5">
            <p class="text-xs text-slate-400">场景</p>
            <p class="mt-3 text-lg font-semibold">入库 / 追踪 / 悬赏 / 调度</p>
          </div>
        </div>
      </section>

      <section class="rounded-[32px] border border-slate-200 bg-white p-8 shadow-xl">
        <p class="text-xs font-semibold uppercase tracking-[0.2em] text-brand">Authentication</p>
        <h2 class="mt-4 text-3xl font-bold text-slate-900">欢迎回来</h2>
        <p class="mt-3 text-sm leading-6 text-slate-500">
          请输入账号和密码，进入你的校园快递工作台。
        </p>

        <form class="mt-8 space-y-5" @submit.prevent="handleSubmit">
          <div>
            <label for="login-username" class="mb-2 block text-sm font-medium text-slate-700">用户名</label>
            <input
              id="login-username"
              v-model="loginForm.username"
              type="text"
              class="w-full rounded-2xl border border-slate-200 px-4 py-3 outline-none transition focus:border-brand"
              placeholder="请输入用户名"
              autocomplete="username"
              @blur="validateField('username')"
            >
            <p v-if="errors.username" class="mt-2 text-sm text-rose-500">{{ errors.username }}</p>
          </div>

          <div>
            <label for="login-password" class="mb-2 block text-sm font-medium text-slate-700">密码</label>
            <input
              id="login-password"
              v-model="loginForm.password"
              type="password"
              class="w-full rounded-2xl border border-slate-200 px-4 py-3 outline-none transition focus:border-brand"
              placeholder="请输入登录密码"
              autocomplete="current-password"
              @blur="validateField('password')"
            >
            <p v-if="errors.password" class="mt-2 text-sm text-rose-500">{{ errors.password }}</p>
          </div>

          <div v-if="error" class="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-700">
            {{ error }}
          </div>

          <button
            type="submit"
            class="w-full rounded-2xl bg-brand px-4 py-3 text-sm font-semibold text-white shadow-sm transition hover:bg-brand/90 disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="loading"
          >
            {{ loading ? '登录中...' : '进入系统' }}
          </button>
        </form>

        <div class="mt-5 space-y-4">
          <button
            type="button"
            class="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
            @click="fillAdmin"
          >
            快速填充管理员账号
          </button>

          <div class="flex flex-wrap items-center justify-between gap-3 text-sm text-slate-500">
            <p>
              还没有账号？
              <router-link to="/register" class="font-medium text-brand">立即注册</router-link>
            </p>
            <span>默认管理员：admin / admin123</span>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const loginForm = reactive({ username: '', password: '' })
const errors = reactive({ username: '', password: '' })
const loading = ref(false)
const error = ref('')

function validateField(field) {
  if (field === 'username') {
    errors.username = loginForm.username.trim() ? '' : '请输入用户名'
  }
  if (field === 'password') {
    errors.password = loginForm.password ? '' : '请输入密码'
  }
}

function validateForm() {
  validateField('username')
  validateField('password')
  return !errors.username && !errors.password
}

function getDeviceId() {
  let id = localStorage.getItem('deviceId')
  if (!id) {
    id = `web_${Math.random().toString(36).slice(2)}_${Date.now()}`
    localStorage.setItem('deviceId', id)
  }
  return id
}

function fillAdmin() {
  loginForm.username = 'admin'
  loginForm.password = 'admin123'
  errors.username = ''
  errors.password = ''
  error.value = ''
}

async function handleSubmit() {
  error.value = ''
  if (!validateForm()) return

  loading.value = true
  try {
    const result = await authStore.login({
      username: loginForm.username.trim(),
      password: loginForm.password,
      deviceId: getDeviceId(),
    })
    router.push(resolveRedirect(result?.role))
  } catch (e) {
    error.value = e?.message || e?.msg || '登录失败，请检查账号和密码'
  } finally {
    loading.value = false
  }
}

function resolveRedirect(role) {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
  if (!redirect) {
    return role === 'ADMIN' || role === 'COURIER' ? '/admin/packages' : '/'
  }

  if (redirect.startsWith('/admin/dashboard') || redirect.startsWith('/admin/users')) {
    return role === 'ADMIN' ? redirect : '/'
  }

  if (redirect.startsWith('/admin/packages') || redirect.startsWith('/admin/deliveries')) {
    return role === 'ADMIN' || role === 'COURIER' ? redirect : '/'
  }

  return redirect
}
</script>
