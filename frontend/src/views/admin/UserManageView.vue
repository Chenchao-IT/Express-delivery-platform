<template>
  <div class="space-y-6">
    <section class="flex flex-wrap items-start justify-between gap-4">
      <div>
        <p class="text-xs font-semibold uppercase tracking-[0.2em] text-brand">User Access</p>
        <h1 class="mt-2 text-3xl font-bold text-slate-900">用户权限</h1>
        <p class="mt-3 max-w-3xl text-sm leading-7 text-slate-500">
          管理员可查看系统用户、角色分布与基础信息，用于论文中的多角色权限验证。
        </p>
      </div>
      <button type="button" class="primary-cta h-11 px-4" :disabled="loading" @click="load">
        {{ loading ? '刷新中...' : '刷新列表' }}
      </button>
    </section>

    <section class="panel-card overflow-hidden">
      <div v-if="users.length === 0 && !loading" class="empty-state py-16">
        <div class="text-5xl">👥</div>
        <h3 class="mt-5 text-2xl font-semibold text-slate-900">暂无用户数据</h3>
        <p class="mt-3 text-sm leading-7 text-slate-500">注册、导入或初始化数据后，用户会显示在这里。</p>
      </div>

      <div v-else class="overflow-x-auto">
        <table class="min-w-full text-left">
          <thead class="bg-slate-50">
            <tr>
              <th class="px-4 py-3 text-sm font-medium text-slate-700">ID</th>
              <th class="px-4 py-3 text-sm font-medium text-slate-700">用户名</th>
              <th class="px-4 py-3 text-sm font-medium text-slate-700">姓名</th>
              <th class="px-4 py-3 text-sm font-medium text-slate-700">角色</th>
              <th class="px-4 py-3 text-sm font-medium text-slate-700">学院</th>
              <th class="px-4 py-3 text-sm font-medium text-slate-700">手机号</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="user in users"
              :key="user.id"
              class="border-t border-slate-100 hover:bg-slate-50/70"
            >
              <td class="px-4 py-3 text-sm text-slate-600">{{ user.id }}</td>
              <td class="px-4 py-3 text-sm text-slate-900">{{ user.username }}</td>
              <td class="px-4 py-3 text-sm text-slate-600">{{ user.realName || '-' }}</td>
              <td class="px-4 py-3">
                <span class="rounded-full px-3 py-1 text-xs font-medium" :class="roleClass(user.role)">
                  {{ roleLabel(user.role) }}
                </span>
              </td>
              <td class="px-4 py-3 text-sm text-slate-600">{{ user.college || '-' }}</td>
              <td class="px-4 py-3 text-sm text-slate-600">{{ user.phone || '-' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { listUsers } from '@/api/user'

const users = ref([])
const loading = ref(false)

function roleLabel(role) {
  return { ADMIN: '管理员', COURIER: '代取员', STUDENT: '学生' }[role] || role
}

function roleClass(role) {
  if (role === 'ADMIN') return 'bg-rose-100 text-rose-700'
  if (role === 'COURIER') return 'bg-brand/10 text-brand'
  return 'bg-slate-100 text-slate-600'
}

async function load() {
  loading.value = true
  try {
    const list = await listUsers()
    users.value = Array.isArray(list) ? list : []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
