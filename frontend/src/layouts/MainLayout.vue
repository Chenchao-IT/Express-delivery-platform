<template>
  <div class="min-h-screen bg-slate-50">
    <header class="sticky top-0 z-40 border-b border-slate-200 bg-white/90 backdrop-blur">
      <div class="mx-auto flex max-w-7xl items-center justify-between gap-4 px-4 py-4 sm:px-6 lg:px-8">
        <router-link to="/" class="flex items-center gap-3">
          <div class="flex h-11 w-11 items-center justify-center rounded-2xl bg-brand text-lg font-bold text-white shadow-sm">
            校
          </div>
          <div>
            <p class="text-base font-semibold text-slate-900">校园快递代取配送系统</p>
            <p class="text-xs text-slate-500">Campus Express Delivery</p>
          </div>
        </router-link>

        <nav class="hidden flex-wrap items-center gap-2 lg:flex">
          <router-link
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="rounded-full px-4 py-2 text-sm font-medium text-slate-600 transition hover:bg-slate-100 hover:text-slate-900"
            active-class="bg-brand text-white hover:bg-brand hover:text-white"
          >
            {{ item.label }}
          </router-link>
        </nav>

        <div class="hidden items-center gap-3 lg:flex">
          <div class="rounded-2xl border border-slate-200 bg-white px-4 py-2 text-right shadow-sm">
            <p class="text-sm font-medium text-slate-900">
              {{ authStore.user?.realName || authStore.user?.username || '当前用户' }}
            </p>
            <p class="text-xs text-slate-500">{{ roleLabel(authStore.user?.role) }}</p>
          </div>
          <button
            type="button"
            class="rounded-2xl border border-slate-200 px-4 py-2 text-sm font-medium text-slate-600 transition hover:border-rose-200 hover:bg-rose-50 hover:text-rose-600"
            @click="handleLogout"
          >
            退出登录
          </button>
        </div>

        <button
          type="button"
          class="inline-flex h-11 w-11 items-center justify-center rounded-2xl border border-slate-200 bg-white text-slate-700 lg:hidden"
          @click="mobileMenuOpen = !mobileMenuOpen"
        >
          {{ mobileMenuOpen ? '×' : '≡' }}
        </button>
      </div>

      <div v-if="mobileMenuOpen" class="border-t border-slate-200 bg-white lg:hidden">
        <div class="mx-auto max-w-7xl space-y-3 px-4 py-4 sm:px-6 lg:px-8">
          <router-link
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="block rounded-2xl border border-slate-200 px-4 py-3 text-sm font-medium text-slate-700"
            active-class="border-brand/30 bg-brand/5 text-brand"
            @click="mobileMenuOpen = false"
          >
            {{ item.label }}
          </router-link>

          <div class="rounded-2xl border border-slate-200 bg-slate-50 p-4">
            <p class="text-sm font-medium text-slate-900">
              {{ authStore.user?.realName || authStore.user?.username || '当前用户' }}
            </p>
            <p class="mt-1 text-xs text-slate-500">{{ roleLabel(authStore.user?.role) }}</p>
            <button
              type="button"
              class="mt-4 w-full rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-medium text-rose-600"
              @click="handleLogout"
            >
              退出登录
            </button>
          </div>
        </div>
      </div>
    </header>

    <main class="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const mobileMenuOpen = ref(false)

const navItems = computed(() => {
  const items = [
    { path: '/', label: '包裹中心' },
    { path: '/track', label: '物流追踪' },
    { path: '/tasks', label: '任务大厅' },
    { path: '/notifications', label: '消息通知' },
  ]

  if (authStore.isAdmin || authStore.isCourier) {
    items.push({ path: '/admin/packages', label: '扫码入库' })
    items.push({ path: '/admin/deliveries', label: '配送调度' })
  }

  if (authStore.isAdmin) {
    items.push({ path: '/admin/dashboard', label: '数据大屏' })
    items.push({ path: '/admin/users', label: '用户权限' })
  }

  return items
})

function roleLabel(role) {
  return {
    ADMIN: '管理员',
    COURIER: '代取员',
    STUDENT: '学生',
  }[role] || '用户'
}

function handleLogout() {
  authStore.logout()
  mobileMenuOpen.value = false
  router.push('/login')
}

watch(
  () => route.fullPath,
  () => {
    mobileMenuOpen.value = false
  },
)
</script>
