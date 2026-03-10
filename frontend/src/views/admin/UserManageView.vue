<template>
  <div>
    <h1 class="text-2xl font-bold text-text-primary mb-6">用户管理</h1>

    <div class="bg-white rounded-card shadow-card overflow-hidden">
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead class="bg-fill-disabled">
            <tr>
              <th class="px-4 py-3 text-left text-sm font-medium text-text-primary">ID</th>
              <th class="px-4 py-3 text-left text-sm font-medium text-text-primary">用户名</th>
              <th class="px-4 py-3 text-left text-sm font-medium text-text-primary">姓名</th>
              <th class="px-4 py-3 text-left text-sm font-medium text-text-primary">角色</th>
              <th class="px-4 py-3 text-left text-sm font-medium text-text-primary">学院</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="u in users"
              :key="u.id"
              class="border-t border-fill-border hover:bg-fill-disabled/50"
            >
              <td class="px-4 py-3 text-sm">{{ u.id }}</td>
              <td class="px-4 py-3 text-sm">{{ u.username }}</td>
              <td class="px-4 py-3 text-sm">{{ u.realName || '-' }}</td>
              <td class="px-4 py-3">
                <span :class="roleClass(u.role)">{{ roleLabel(u.role) }}</span>
              </td>
              <td class="px-4 py-3 text-sm">{{ u.college || '-' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listUsers } from '@/api/user'

const users = ref([])

const roleLabel = (r) => ({ ADMIN: '管理员', COURIER: '快递员', STUDENT: '学生' }[r] || r)
const roleClass = (r) => {
  const base = 'px-2 py-1 rounded text-xs'
  if (r === 'ADMIN') return base + ' bg-danger/20 text-danger'
  if (r === 'COURIER') return base + ' bg-brand/10 text-brand'
  return base + ' bg-fill-hover'
}

const load = async () => {
  users.value = await listUsers()
}

onMounted(load)
</script>
