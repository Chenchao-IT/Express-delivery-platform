<template>
  <div class="space-y-6">
    <section class="flex flex-wrap items-start justify-between gap-4">
      <div>
        <p class="text-xs font-semibold uppercase tracking-[0.2em] text-brand">Visual Dashboard</p>
        <h1 class="mt-2 text-3xl font-bold text-slate-900">数据大屏</h1>
        <p class="mt-3 max-w-3xl text-sm leading-7 text-slate-500">
          结合论文中的“今日处理量、滞留率、配送任务态势”等要求，对包裹与配送任务进行可视化统计。
        </p>
      </div>
      <button type="button" class="primary-cta h-11 px-4" :disabled="loading" @click="load">
        {{ loading ? '刷新中...' : '刷新大屏' }}
      </button>
    </section>

    <section class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
      <article v-for="item in overviewCards" :key="item.key" class="rounded-[28px] border border-slate-200 bg-white p-6 shadow-sm">
        <p class="text-sm text-slate-500">{{ item.label }}</p>
        <p class="mt-4 text-4xl font-bold text-slate-900">{{ item.value }}</p>
        <p class="mt-2 text-sm text-slate-500">{{ item.hint }}</p>
      </article>
    </section>

    <section class="grid gap-6 xl:grid-cols-2">
      <article class="panel-card">
        <h2 class="panel-title">包裹状态分布</h2>
        <p class="panel-subtitle">用于观察当前驿站积压量与流转效率。</p>

        <div class="mt-6 space-y-4">
          <div v-for="item in packageBars" :key="item.key">
            <div class="mb-2 flex items-center justify-between text-sm text-slate-500">
              <span>{{ item.label }}</span>
              <span>{{ item.value }}</span>
            </div>
            <div class="h-3 overflow-hidden rounded-full bg-slate-100">
              <div class="h-full rounded-full" :class="item.barClass" :style="{ width: `${item.percent}%` }" />
            </div>
          </div>
        </div>
      </article>

      <article class="panel-card">
        <h2 class="panel-title">配送任务概览</h2>
        <p class="panel-subtitle">展示待处理、配送中与已完成任务数量。</p>

        <div class="mt-6 grid grid-cols-2 gap-4">
          <div v-for="item in deliveryCards" :key="item.key" class="rounded-[24px] bg-slate-50 p-4">
            <p class="text-xs text-slate-400">{{ item.label }}</p>
            <p class="mt-3 text-2xl font-semibold text-slate-900">{{ item.value }}</p>
          </div>
        </div>
      </article>
    </section>

    <section class="panel-card">
      <h2 class="panel-title">近 7 天入库趋势</h2>
      <p class="panel-subtitle">辅助观察日均处理量与入库峰值。</p>

      <div v-if="dailyTrend.length === 0" class="empty-state mt-6">
        <div class="text-5xl">📈</div>
        <h3 class="mt-5 text-2xl font-semibold text-slate-900">暂无趋势数据</h3>
        <p class="mt-3 text-sm leading-7 text-slate-500">当系统存在近 7 天入库记录时，这里会展示每日统计。</p>
      </div>

      <div v-else class="mt-6 flex h-64 items-end gap-4">
        <div v-for="item in dailyTrend" :key="item.date" class="flex flex-1 flex-col items-center justify-end">
          <div class="w-full rounded-t-xl bg-brand/70 transition hover:bg-brand" :style="{ height: `${item.height}%` }" />
          <p class="mt-3 text-xs text-slate-400">{{ item.label }}</p>
          <p class="mt-1 text-sm font-medium text-slate-900">{{ item.count }}</p>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { listPackages } from '@/api/package'
import { listDeliveries } from '@/api/delivery'

const loading = ref(false)
const packages = ref([])
const deliveries = ref([])

const packageStats = computed(() => {
  const result = {
    total: packages.value.length,
    inStorage: 0,
    delivering: 0,
    finished: 0,
  }

  packages.value.forEach((pkg) => {
    if (pkg.status === 'IN_STORAGE') result.inStorage += 1
    else if (pkg.status === 'OUT_FOR_DELIVERY') result.delivering += 1
    else if (['DELIVERED', 'PICKED_UP', 'COMPLETED'].includes(pkg.status)) result.finished += 1
  })

  return result
})

const deliveryStats = computed(() => {
  const result = {
    total: deliveries.value.length,
    pending: 0,
    inProgress: 0,
    completed: 0,
  }

  deliveries.value.forEach((item) => {
    if (item.status === 'PENDING' || item.status === 'ASSIGNED') result.pending += 1
    else if (item.status === 'IN_PROGRESS') result.inProgress += 1
    else if (item.status === 'COMPLETED') result.completed += 1
  })

  return result
})

const retentionRate = computed(() => {
  if (!packageStats.value.total) return '0%'
  return `${Math.round((packageStats.value.inStorage / packageStats.value.total) * 100)}%`
})

const overviewCards = computed(() => [
  {
    key: 'total',
    label: '包裹总量',
    value: packageStats.value.total,
    hint: '系统内全部包裹记录',
  },
  {
    key: 'inStorage',
    label: '当前滞留',
    value: packageStats.value.inStorage,
    hint: `驿站滞留率 ${retentionRate.value}`,
  },
  {
    key: 'task',
    label: '配送任务',
    value: deliveryStats.value.total,
    hint: '覆盖预约配送与调度统计',
  },
  {
    key: 'completed',
    label: '已完成流转',
    value: packageStats.value.finished,
    hint: '已送达或已签收包裹',
  },
])

const packageBars = computed(() => {
  const total = packageStats.value.total || 1
  return [
    {
      key: 'inStorage',
      label: '待取件 / 滞留',
      value: packageStats.value.inStorage,
      percent: Math.round((packageStats.value.inStorage / total) * 100),
      barClass: 'bg-amber-400',
    },
    {
      key: 'delivering',
      label: '配送中',
      value: packageStats.value.delivering,
      percent: Math.round((packageStats.value.delivering / total) * 100),
      barClass: 'bg-brand',
    },
    {
      key: 'finished',
      label: '已完成',
      value: packageStats.value.finished,
      percent: Math.round((packageStats.value.finished / total) * 100),
      barClass: 'bg-emerald-500',
    },
  ]
})

const deliveryCards = computed(() => [
  { key: 'pending', label: '待处理任务', value: deliveryStats.value.pending },
  { key: 'progress', label: '配送中任务', value: deliveryStats.value.inProgress },
  { key: 'completed', label: '已完成任务', value: deliveryStats.value.completed },
  { key: 'retention', label: '驿站滞留率', value: retentionRate.value },
])

const dailyTrend = computed(() => {
  const grouped = new Map()
  packages.value.forEach((pkg) => {
    if (!pkg.storageTime) return
    const date = String(pkg.storageTime).slice(0, 10)
    grouped.set(date, (grouped.get(date) || 0) + 1)
  })

  const items = Array.from(grouped.entries())
    .sort((a, b) => a[0].localeCompare(b[0]))
    .slice(-7)
    .map(([date, count]) => ({ date, count, label: date.slice(5) }))

  const max = Math.max(...items.map((item) => item.count), 1)
  return items.map((item) => ({
    ...item,
    height: Math.max(12, Math.round((item.count / max) * 100)),
  }))
})

async function load() {
  loading.value = true
  try {
    const [pkgList, taskList] = await Promise.all([listPackages(), listDeliveries()])
    packages.value = Array.isArray(pkgList) ? pkgList : []
    deliveries.value = Array.isArray(taskList) ? taskList : []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
