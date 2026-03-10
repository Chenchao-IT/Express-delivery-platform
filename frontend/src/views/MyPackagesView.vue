<template>
  <div>
    <h1 class="text-2xl font-bold text-text-primary mb-6">我的包裹</h1>

    <div v-if="loading" class="text-center py-12 text-text-secondary">加载中...</div>

    <div v-else-if="packages.length === 0" class="bg-white rounded-card shadow-card p-12 text-center">
      <div class="text-6xl mb-4 opacity-50">📦</div>
      <h3 class="font-semibold text-text-primary mb-2">暂无包裹</h3>
      <p class="text-text-secondary text-sm">您目前没有待取或进行中的快递包裹</p>
    </div>

    <div v-else class="space-y-4">
      <div
        v-for="pkg in packages"
        :key="pkg.id"
        class="bg-white rounded-card shadow-card p-5 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4"
      >
        <div class="flex-1">
          <p class="font-medium text-text-primary">运单号: {{ pkg.trackingNumber }}</p>
          <p class="text-text-secondary text-sm mt-1">
            货架: {{ pkg.shelfCode || '-' }} · 
            尺寸: {{ sizeLabel(pkg.size) }} · 
            状态: {{ statusLabel(pkg.status) }}
          </p>
        </div>
        <button
          v-if="pkg.status === 'IN_STORAGE'"
          @click="pickup(pkg.id)"
          :disabled="pickupLoading === pkg.id"
          class="btn-primary shrink-0"
        >
          {{ pickupLoading === pkg.id ? '取件中...' : '确认取件' }}
        </button>
        <span
          v-else
          class="text-text-tertiary text-sm shrink-0"
        >
          {{ pkg.status === 'PICKED_UP' ? '已取件' : statusLabel(pkg.status) }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMyPackages, pickupPackage } from '@/api/package'

const packages = ref([])
const loading = ref(true)
const pickupLoading = ref(null)

const sizeLabel = (size) => {
  const map = { SMALL: '小件', MEDIUM: '中件', LARGE: '大件' }
  return map[size] || size
}

const statusLabel = (status) => {
  const map = {
    IN_STORAGE: '待取件',
    OUT_FOR_DELIVERY: '配送中',
    DELIVERED: '已送达',
    PICKED_UP: '已取件',
  }
  return map[status] || status
}

const load = async () => {
  loading.value = true
  try {
    packages.value = await getMyPackages()
  } finally {
    loading.value = false
  }
}

const pickup = async (id) => {
  pickupLoading.value = id
  try {
    await pickupPackage(id)
    load()
  } finally {
    pickupLoading.value = null
  }
}

onMounted(load)
</script>
