<template>
  <div>
    <h1 class="text-2xl font-bold text-text-primary mb-6">快递查询</h1>

    <div class="bg-white rounded-card shadow-card p-6 mb-6">
      <div class="flex gap-3">
        <input
          v-model="trackingNumber"
          type="text"
          class="input-base flex-1"
          placeholder="请输入运单号"
          @keyup.enter="search"
        />
        <button @click="search" :disabled="loading" class="btn-primary px-6">
          {{ loading ? '查询中...' : '查询' }}
        </button>
      </div>
    </div>

    <div v-if="result" class="bg-white rounded-card shadow-card p-6">
      <h3 class="font-semibold text-text-primary mb-4">查询结果</h3>
      <dl class="grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm">
        <div>
          <dt class="text-text-tertiary">运单号</dt>
          <dd class="font-medium text-text-primary">{{ result.trackingNumber }}</dd>
        </div>
        <div>
          <dt class="text-text-tertiary">状态</dt>
          <dd>{{ statusLabel(result.status) }}</dd>
        </div>
        <div>
          <dt class="text-text-tertiary">货架位置</dt>
          <dd>{{ result.shelfCode || '-' }}</dd>
        </div>
        <div>
          <dt class="text-text-tertiary">包裹尺寸</dt>
          <dd>{{ sizeLabel(result.size) }}</dd>
        </div>
        <div>
          <dt class="text-text-tertiary">入库时间</dt>
          <dd>{{ formatTime(result.storageTime) }}</dd>
        </div>
      </dl>
    </div>

    <div v-else-if="searched && !loading" class="bg-white rounded-card shadow-card p-12 text-center">
      <div class="text-6xl mb-4 opacity-50">🔍</div>
      <h3 class="font-semibold text-text-primary mb-2">未找到包裹</h3>
      <p class="text-text-secondary text-sm">请核对运单号是否正确</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { trackPackage } from '@/api/package'

const trackingNumber = ref('')
const result = ref(null)
const loading = ref(false)
const searched = ref(false)

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

const formatTime = (t) => (t ? new Date(t).toLocaleString('zh-CN') : '-')

const search = async () => {
  if (!trackingNumber.value.trim()) return
  loading.value = true
  searched.value = true
  result.value = null
  try {
    result.value = await trackPackage(trackingNumber.value.trim())
  } catch {
    result.value = null
  } finally {
    loading.value = false
  }
}
</script>
