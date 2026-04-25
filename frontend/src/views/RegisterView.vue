<template>
  <div class="min-h-screen bg-gradient-to-br from-slate-100 via-white to-emerald-50 px-4 py-10">
    <div class="mx-auto grid max-w-6xl gap-8 lg:grid-cols-[1.05fr_0.95fr]">
      <section class="rounded-[32px] bg-emerald-900 p-8 text-white shadow-2xl">
        <p class="inline-flex rounded-full bg-white/10 px-3 py-1 text-xs font-medium tracking-[0.2em]">
          STUDENT ONBOARDING
        </p>
        <h1 class="mt-6 text-4xl font-bold leading-tight">
          创建你的
          <span class="block text-emerald-300">校园快递账号</span>
        </h1>
        <p class="mt-4 max-w-2xl text-sm leading-7 text-emerald-100/80">
          注册后即可查看包裹状态、货架位置、提货码，并参与悬赏代取与预约配送等核心流程。
        </p>

        <div class="mt-10 rounded-3xl border border-white/10 bg-white/5 p-6">
          <p class="text-sm font-medium">建议完善基础信息</p>
          <p class="mt-2 text-sm leading-7 text-emerald-100/80">
            完整填写手机号、学院和收货地址，有助于提升身份识别、隐私脱敏展示与配送调度效果。
          </p>
        </div>
      </section>

      <section class="rounded-[32px] border border-slate-200 bg-white p-8 shadow-xl">
        <p class="text-xs font-semibold uppercase tracking-[0.2em] text-emerald-600">Registration</p>
        <h2 class="mt-4 text-3xl font-bold text-slate-900">创建学生账号</h2>
        <p class="mt-3 text-sm leading-6 text-slate-500">完成注册后将自动登录并进入包裹中心。</p>

        <form class="mt-8 space-y-5" @submit.prevent="handleRegister">
          <div class="grid gap-5 md:grid-cols-2">
            <div>
              <label for="reg-username" class="mb-2 block text-sm font-medium text-slate-700">用户名 *</label>
              <input id="reg-username" v-model="form.username" class="w-full rounded-2xl border border-slate-200 px-4 py-3 outline-none focus:border-emerald-500">
              <p v-if="errors.username" class="mt-2 text-sm text-rose-500">{{ errors.username }}</p>
            </div>
            <div>
              <label for="reg-password" class="mb-2 block text-sm font-medium text-slate-700">密码 *</label>
              <input id="reg-password" v-model="form.password" type="password" class="w-full rounded-2xl border border-slate-200 px-4 py-3 outline-none focus:border-emerald-500">
              <p v-if="errors.password" class="mt-2 text-sm text-rose-500">{{ errors.password }}</p>
            </div>
          </div>

          <div class="grid gap-5 md:grid-cols-2">
            <div>
              <label for="reg-realName" class="mb-2 block text-sm font-medium text-slate-700">姓名</label>
              <input id="reg-realName" v-model="form.realName" class="w-full rounded-2xl border border-slate-200 px-4 py-3 outline-none focus:border-emerald-500">
            </div>
            <div>
              <label for="reg-phone" class="mb-2 block text-sm font-medium text-slate-700">手机号</label>
              <input id="reg-phone" v-model="form.phone" class="w-full rounded-2xl border border-slate-200 px-4 py-3 outline-none focus:border-emerald-500">
            </div>
          </div>

          <div class="grid gap-5 md:grid-cols-2">
            <div>
              <label for="reg-email" class="mb-2 block text-sm font-medium text-slate-700">邮箱</label>
              <input id="reg-email" v-model="form.email" class="w-full rounded-2xl border border-slate-200 px-4 py-3 outline-none focus:border-emerald-500" @blur="validateField('email')">
              <p v-if="errors.email" class="mt-2 text-sm text-rose-500">{{ errors.email }}</p>
            </div>
            <div>
              <label for="reg-college" class="mb-2 block text-sm font-medium text-slate-700">学院</label>
              <input id="reg-college" v-model="form.college" class="w-full rounded-2xl border border-slate-200 px-4 py-3 outline-none focus:border-emerald-500">
            </div>
          </div>

          <div>
            <label for="reg-address" class="mb-2 block text-sm font-medium text-slate-700">收货地址</label>
            <input id="reg-address" v-model="form.address" class="w-full rounded-2xl border border-slate-200 px-4 py-3 outline-none focus:border-emerald-500">
          </div>

          <div v-if="error" class="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-700">
            {{ error }}
          </div>

          <button
            type="submit"
            class="w-full rounded-2xl bg-emerald-600 px-4 py-3 text-sm font-semibold text-white transition hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="loading"
          >
            {{ loading ? '注册中...' : '创建账号' }}
          </button>
        </form>

        <div class="mt-5 text-sm text-slate-500">
          已有账号？
          <router-link to="/login" class="font-medium text-emerald-600">立即登录</router-link>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const form = reactive({
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: '',
  college: '',
  address: '',
})

const errors = reactive({
  username: '',
  password: '',
  email: '',
})

const loading = ref(false)
const error = ref('')
const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function validateField(field) {
  if (field === 'username') {
    const value = form.username.trim()
    if (!value) errors.username = '请输入用户名'
    else if (value.length < 3) errors.username = '用户名至少 3 个字符'
    else if (value.length > 50) errors.username = '用户名最多 50 个字符'
    else errors.username = ''
  }

  if (field === 'password') {
    if (!form.password) errors.password = '请输入密码'
    else if (form.password.length < 6) errors.password = '密码至少 6 位'
    else errors.password = ''
  }

  if (field === 'email') {
    if (!form.email.trim()) errors.email = ''
    else if (!emailRegex.test(form.email)) errors.email = '请输入有效邮箱地址'
    else errors.email = ''
  }
}

function validateForm() {
  validateField('username')
  validateField('password')
  validateField('email')
  return !errors.username && !errors.password && !errors.email
}

async function handleRegister() {
  error.value = ''
  if (!validateForm()) return

  loading.value = true
  try {
    const result = await authStore.register(form)
    router.push(result?.role === 'ADMIN' || result?.role === 'COURIER' ? '/admin/packages' : '/')
  } catch (e) {
    error.value = e?.message || '注册失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>
