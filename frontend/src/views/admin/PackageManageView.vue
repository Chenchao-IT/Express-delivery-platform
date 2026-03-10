<template>
  <div>
    <h1 class="text-2xl font-bold text-text-primary mb-6">包裹管理</h1>

    <div class="bg-white rounded-card shadow-card p-6 mb-6">
      <h3 class="font-semibold text-text-primary mb-4">入库登记</h3>
      <div class="flex flex-wrap gap-4 items-end">
        <div>
          <label class="block text-sm text-text-secondary mb-1">学生用户名</label>
          <input v-model="createForm.studentUsername" class="input-base" placeholder="学生用户名" />
        </div>
        <div>
          <label class="block text-sm text-text-secondary mb-1">包裹尺寸</label>
          <select v-model="createForm.size" class="input-base">
            <option value="SMALL">小件</option>
            <option value="MEDIUM">中件</option>
            <option value="LARGE">大件</option>
          </select>
        </div>
        <button @click="createPackage" :disabled="createLoading" class="btn-primary">
          {{ createLoading ? '入库中...' : '入库登记' }}
        </button>
      </div>
    </div>

    <div class="bg-white rounded-card shadow-card overflow-hidden">
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead class="bg-fill-disabled">
            <tr>
              <th class="px-4 py-3 text-left text-sm font-medium text-text-primary">运单号</th>
              <th class="px-4 py-3 text-left text-sm font-medium text-text-primary">货架</th>
              <th class="px-4 py-3 text-left text-sm font-medium text-text-primary">尺寸</th>
              <th class="px-4 py-3 text-left text-sm font-medium text-text-primary">状态</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="pkg in packages"
              :key="pkg.id"
              class="border-t border-fill-border hover:bg-fill-disabled/50"
            >
              <td class="px-4 py-3 text-sm">{{ pkg.trackingNumber }}</td>
              <td class="px-4 py-3 text-sm">{{ pkg.shelfCode || '-' }}</td>
              <td class="px-4 py-3 text-sm">{{ sizeLabel(pkg.size) }}</td>
              <td class="px-4 py-3">
                <span :class="statusClass(pkg.status)">{{ statusLabel(pkg.status) }}</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listPackages, createPackage as createPackageApi } from '@/api/package'

const packages = ref([])
const createForm = reactive({ studentUsername: '', size: 'MEDIUM' })
const createLoading = ref(false)

const sizeLabel = (s) => ({ SMALL: '小件', MEDIUM: '中件', LARGE: '大件' }[s] || s)
const statusLabel = (s) => ({
  IN_STORAGE: '待取件',
  OUT_FOR_DELIVERY: '配送中',
  DELIVERED: '已送达',
  PICKED_UP: '已取件',
}[s] || s)
const statusClass = (s) => {
  const base = 'px-2 py-1 rounded text-xs'
  if (s === 'IN_STORAGE') return base + ' bg-brand/10 text-brand'
  if (s === 'PICKED_UP' || s === 'DELIVERED') return base + ' bg-success/20 text-success'
  return base + ' bg-fill-hover'
}

const load = async () => {
  packages.value = await listPackages()
}

const createPackage = async () => {
  if (!createForm.studentUsername.trim()) return
  createLoading.value = true
  try {
    const pkg = await createPackageApi(createForm)
    createForm.studentUsername = ''
    load()
    alert(`入库成功，运单号: ${pkg.trackingNumber}`)
  } catch (e) {
    alert(e?.message || '入库失败')
  } finally {
    createLoading.value = false
  }
}

onMounted(load)
</script>
