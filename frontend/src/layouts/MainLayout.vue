<template>
  <div class="min-h-screen bg-fill-disabled" role="application" aria-label="校园快递配送系统">
    <!-- 文档 2.2：关键冲突/非关键冲突提示条 -->
    <div
      v-if="authStore.user?.authMessage"
      class="auth-banner"
      :class="authStore.user?.conflictResolutionRequired ? 'auth-banner-warning' : 'auth-banner-info'"
    >
      {{ authStore.user.authMessage }}
    </div>
    <header class="bg-white shadow-dropdown sticky top-0 z-sticky">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between items-center h-16">
          <router-link to="/" class="flex items-center gap-2">
            <span class="text-xl font-semibold text-brand">校园快递</span>
          </router-link>
          <nav class="flex items-center gap-6">
            <router-link
              v-for="item in navItems"
              :key="item.path"
              :to="item.path"
              class="text-text-secondary hover:text-text-primary transition-colors text-sm"
              active-class="text-brand font-medium"
            >
              {{ item.label }}
            </router-link>
            <select
              v-model="theme"
              @change="applyTheme(theme)"
              class="text-text-secondary text-sm border-0 bg-transparent cursor-pointer focus:outline-none"
              aria-label="选择高对比度主题"
            >
              <option value="default">默认主题</option>
              <option value="high-contrast">高对比度</option>
              <option value="dark-high-contrast">深色高对比度</option>
              <option value="yellow-black">黄黑主题</option>
            </select>
            <button
              @click="handleLogout"
              class="text-text-secondary hover:text-danger text-sm"
            >
              退出
            </button>
          </nav>
        </div>
      </div>
    </header>
    <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const theme = ref('default')

function applyTheme(t) {
  document.body.classList.remove('theme-high-contrast', 'theme-dark-high-contrast', 'theme-yellow-black')
  if (t && t !== 'default') document.body.classList.add(`theme-${t}`)
  try { localStorage.setItem('preferredTheme', t || 'default') } catch (_) {}
}

onMounted(() => {
  const saved = localStorage.getItem('preferredTheme')
  if (saved) {
    theme.value = saved
    applyTheme(saved)
  }
})

const navItems = computed(() => {
  const items = [
    { path: '/', label: '包裹中心' },
    { path: '/packages', label: '我的包裹' },
    { path: '/track', label: '快递查询' },
    { path: '/tasks', label: '任务大厅' },
  ]
  if (authStore.user?.conflictResolutionRequired) {
    items.push({ path: '/appeal', label: '身份申诉' })
  }
  if (authStore.isAdmin || authStore.isCourier) {
    items.push({ path: '/admin/packages', label: '包裹管理' })
    items.push({ path: '/admin/deliveries', label: '配送管理' })
  }
  if (authStore.isAdmin) {
    items.push({ path: '/admin/dashboard', label: '管理大屏' })
    items.push({ path: '/admin/users', label: '用户管理' })
    items.push({ path: '/admin/appeals', label: '申诉审核' })
  }
  return items
})

const handleLogout = () => {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.auth-banner {
  padding: 0.5rem 1rem;
  font-size: 0.875rem;
  text-align: center;
}
.auth-banner-warning {
  background: #fffbeb;
  color: #d97706;
  border-bottom: 1px solid #fcd34d;
}
.auth-banner-info {
  background: #e0efff;
  color: #0756b0;
  border-bottom: 1px solid #99c9ff;
}
</style>
