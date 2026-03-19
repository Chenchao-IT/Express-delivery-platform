<template>
  <div>
    <h1 class="text-2xl font-bold text-text-primary mb-6">管理数据大屏</h1>

    <!-- 顶部统计卡片 -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
      <div class="bg-white rounded-card shadow-card p-5">
        <p class="text-text-secondary text-xs mb-1">包裹总数</p>
        <p class="text-2xl font-bold text-text-primary">{{ stats.total }}</p>
      </div>
      <div class="bg-white rounded-card shadow-card p-5">
        <p class="text-text-secondary text-xs mb-1">在库包裹</p>
        <p class="text-2xl font-bold text-brand">{{ stats.inStorage }}</p>
      </div>
      <div class="bg-white rounded-card shadow-card p-5">
        <p class="text-text-secondary text-xs mb-1">配送中/待送达</p>
        <p class="text-2xl font-bold text-warning-text">{{ stats.moving }}</p>
      </div>
      <div class="bg-white rounded-card shadow-card p-5">
        <p class="text-text-secondary text-xs mb-1">已完成（签收+退回）</p>
        <p class="text-2xl font-bold text-success">{{ stats.finished }}</p>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">
      <!-- 状态分布“饼图”（环形条） -->
      <div class="bg-white rounded-card shadow-card p-6 lg:col-span-1">
        <h2 class="font-semibold text-text-primary mb-4">包裹状态分布</h2>
        <div v-if="stats.total === 0" class="text-center py-10 text-text-tertiary text-sm">
          暂无包裹数据
        </div>
        <div v-else>
          <div class="flex items-center justify-between text-xs text-text-secondary mb-3">
            <span>在库</span>
            <span>{{ stats.inStorage }} 件</span>
          </div>
          <div class="h-2 rounded-full bg-fill-border mb-4 overflow-hidden">
            <div
              class="h-full bg-brand transition-all duration-500"
              :style="{ width: percent(stats.inStorage) + '%' }"
            />
          </div>

          <div class="flex items-center justify-between text-xs text-text-secondary mb-3">
            <span>配送中/待送达</span>
            <span>{{ stats.moving }} 件</span>
          </div>
          <div class="h-2 rounded-full bg-fill-border mb-4 overflow-hidden">
            <div
              class="h-full bg-warning/60 transition-all duration-500"
              :style="{ width: percent(stats.moving) + '%' }"
            />
          </div>

          <div class="flex items-center justify-between text-xs text-text-secondary mb-3">
            <span>已完成</span>
            <span>{{ stats.finished }} 件</span>
          </div>
          <div class="h-2 rounded-full bg-fill-border overflow-hidden">
            <div
              class="h-full bg-success/70 transition-all duration-500"
              :style="{ width: percent(stats.finished) + '%' }"
            />
          </div>
        </div>
      </div>

      <!-- 近 7 天入库量条形图 -->
      <div class="bg-white rounded-card shadow-card p-6 lg:col-span-2">
        <h2 class="font-semibold text-text-primary mb-4">近 7 天入库趋势</h2>
        <div v-if="dailyTrend.length === 0" class="text-center py-10 text-text-tertiary text-sm">
          暂无近 7 天入库记录
        </div>
        <div v-else class="h-56 flex items-end gap-3">
          <div
            v-for="d in dailyTrend"
            :key="d.date"
            class="flex-1 flex flex-col items-center justify-end"
          >
            <div
              class="w-full rounded-t-md bg-brand/60 hover:bg-brand transition-colors"
              :style="{ height: barHeight(d.count) + '%' }"
            ></div>
            <div class="mt-2 text-[11px] text-text-tertiary text-center leading-tight">
              <div>{{ d.label }}</div>
              <div>{{ d.count }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部：配送任务概览 -->
    <div class="bg-white rounded-card shadow-card p-6">
      <h2 class="font-semibold text-text-primary mb-4">配送任务概览</h2>
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
        <div>
          <p class="text-text-secondary mb-1">任务总数</p>
          <p class="text-lg font-semibold text-text-primary">{{ deliveryStats.total }}</p>
        </div>
        <div>
          <p class="text-text-secondary mb-1">待分配</p>
          <p class="text-lg font-semibold text-warning-text">{{ deliveryStats.pending }}</p>
        </div>
        <div>
          <p class="text-text-secondary mb-1">配送中</p>
          <p class="text-lg font-semibold text-brand">{{ deliveryStats.inProgress }}</p>
        </div>
        <div>
          <p class="text-text-secondary mb-1">已完成</p>
          <p class="text-lg font-semibold text-success">{{ deliveryStats.completed }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { listPackages } from '@/api/package'
import { listDeliveries } from '@/api/delivery'

const rawPackages = ref([])
const rawDeliveries = ref([])

const stats = computed(() => {
  const total = rawPackages.value.length
  let inStorage = 0
  let moving = 0
  let finished = 0

  rawPackages.value.forEach((p) => {
    if (p.status === 'IN_STORAGE') inStorage++
    else if (p.status === 'OUT_FOR_DELIVERY' || p.status === 'DELIVERED') moving++
    else if (p.status === 'PICKED_UP' || p.status === 'RETURNED' || p.status === 'COMPLETED') finished++
  })

  return { total, inStorage, moving, finished }
})

const deliveryStats = computed(() => {
  const total = rawDeliveries.value.length
  let pending = 0
  let inProgress = 0
  let completed = 0

  rawDeliveries.value.forEach((d) => {
    if (d.status === 'PENDING' || d.status === 'ASSIGNED') pending++
    else if (d.status === 'IN_PROGRESS') inProgress++
    else if (d.status === 'COMPLETED') completed++
  })

  return { total, pending, inProgress, completed }
})

const dailyTrend = computed(() => {
  if (!rawPackages.value.length) return []
  const map = new Map()
  rawPackages.value.forEach((p) => {
    if (!p.storageTime) return
    const date = p.storageTime.substring(0, 10)
    map.set(date, (map.get(date) || 0) + 1)
  })

  // 最近 7 天（按日期排序后取最后 7 个）
  const entries = Array.from(map.entries()).sort((a, b) => (a[0] < b[0] ? -1 : 1))
  const last7 = entries.slice(-7)
  return last7.map(([date, count]) => ({
    date,
    count,
    label: date.slice(5), // MM-dd
  }))
})

const percent = (value) => {
  if (!stats.value.total) return 0
  return Math.round((value / stats.value.total) * 100)
}

const barHeight = (value) => {
  if (!dailyTrend.value.length) return 0
  const max = Math.max(...dailyTrend.value.map((d) => d.count))
  if (!max) return 5
  return Math.max(10, Math.round((value / max) * 100))
}

onMounted(async () => {
  try {
    // 已有接口：管理员角色可拉取全部包裹和任务
    rawPackages.value = await listPackages()
    rawDeliveries.value = await listDeliveries()
  } catch (e) {
    console.error(e)
  }
})
</script>

