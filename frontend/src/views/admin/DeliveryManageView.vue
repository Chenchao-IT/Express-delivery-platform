<template>
  <div class="space-y-6">
    <section class="flex flex-wrap items-start justify-between gap-4">
      <div>
        <p class="text-xs font-semibold uppercase tracking-[0.2em] text-brand">Delivery Dispatch</p>
        <h1 class="mt-2 text-3xl font-bold text-slate-900">配送调度</h1>
        <p class="mt-3 max-w-3xl text-sm leading-7 text-slate-500">
          对应论文中的预约配送调度与同区域任务执行场景，支持查看、抢单、开始配送和完成配送。
        </p>
      </div>

      <button type="button" class="primary-cta h-11 px-4" :disabled="loading" @click="load">
        {{ loading ? '刷新中...' : '刷新调度' }}
      </button>
    </section>

    <section
      v-if="feedback"
      class="rounded-[22px] border border-brand/20 bg-brand/10 px-5 py-4 text-sm text-brand shadow-sm"
    >
      {{ feedback }}
    </section>

    <template v-if="authStore.isCourier && !authStore.isAdmin">
      <section class="grid gap-6 xl:grid-cols-2">
        <article class="panel-card">
          <div class="flex items-start justify-between gap-3">
            <div>
              <h2 class="panel-title">待抢配送任务</h2>
              <p class="panel-subtitle">面向预约配送场景，优先展示可抢的订单。</p>
            </div>
            <span class="rounded-full bg-brand/10 px-3 py-1 text-sm font-medium text-brand">{{ pendingList.length }} 条</span>
          </div>

          <div v-if="pendingList.length === 0" class="empty-state mt-5">
            <div class="text-5xl">📭</div>
            <h3 class="mt-5 text-2xl font-semibold text-slate-900">暂无待抢任务</h3>
            <p class="mt-3 text-sm leading-7 text-slate-500">当学生发起预约配送后，可在这里快速接管任务。</p>
          </div>

          <div v-else class="mt-5 space-y-4">
            <article v-for="task in pendingList" :key="task.id" class="rounded-[24px] border border-slate-200 bg-white p-5 shadow-sm">
              <div class="flex flex-wrap items-center gap-2">
                <span class="rounded-full bg-slate-100 px-3 py-1 text-xs text-slate-600">任务 #{{ task.id }}</span>
                <span class="rounded-full bg-brand/10 px-3 py-1 text-xs text-brand">{{ destinationLabel(task.destination) }}</span>
              </div>
              <p class="mt-4 text-lg font-semibold text-slate-900">包裹 #{{ task.packageId }}</p>
              <p class="mt-2 text-sm text-slate-500">
                预计距离 {{ formatNumber(task.estimatedDistance) }} m · 预计耗时 {{ formatNumber(task.estimatedTime) }} 分钟
              </p>
              <button
                type="button"
                class="primary-cta mt-5 h-11 px-4"
                :disabled="grabLoading === task.id"
                @click="grabTask(task.id)"
              >
                {{ grabLoading === task.id ? '抢单中...' : '立即抢单' }}
              </button>
            </article>
          </div>
        </article>

        <article class="panel-card">
          <div class="flex items-start justify-between gap-3">
            <div>
              <h2 class="panel-title">我的配送任务</h2>
              <p class="panel-subtitle">对应代取员工作流：接单 → 配送中 → 完成。</p>
            </div>
            <span class="rounded-full bg-amber-100 px-3 py-1 text-sm font-medium text-amber-700">{{ myList.length }} 条</span>
          </div>

          <div v-if="myList.length === 0" class="empty-state mt-5">
            <div class="text-5xl">🧾</div>
            <h3 class="mt-5 text-2xl font-semibold text-slate-900">暂无进行中的任务</h3>
            <p class="mt-3 text-sm leading-7 text-slate-500">接单成功后，可在这里推进配送状态。</p>
          </div>

          <div v-else class="mt-5 space-y-4">
            <article v-for="task in myList" :key="task.id" class="rounded-[24px] border border-slate-200 bg-white p-5 shadow-sm">
              <div class="flex flex-wrap items-center gap-2">
                <span class="rounded-full bg-slate-100 px-3 py-1 text-xs text-slate-600">任务 #{{ task.id }}</span>
                <span class="rounded-full px-3 py-1 text-xs font-medium" :class="statusClass(task.status)">{{ statusLabel(task.status) }}</span>
              </div>
              <p class="mt-4 text-lg font-semibold text-slate-900">
                包裹 #{{ task.packageId }} → {{ destinationLabel(task.destination) }}
              </p>
              <p class="mt-2 text-sm text-slate-500">
                预计耗时 {{ formatNumber(task.estimatedTime) }} 分钟
              </p>

              <div class="mt-5 flex flex-wrap gap-3">
                <button
                  v-if="task.status === 'ASSIGNED'"
                  type="button"
                  class="secondary-cta h-11 px-4"
                  :disabled="startLoading === task.id"
                  @click="startTask(task.id)"
                >
                  {{ startLoading === task.id ? '处理中...' : '开始配送' }}
                </button>
                <button
                  v-if="task.status === 'ASSIGNED' || task.status === 'IN_PROGRESS'"
                  type="button"
                  class="primary-cta h-11 px-4"
                  :disabled="completeLoading === task.id"
                  @click="completeTask(task.id)"
                >
                  {{ completeLoading === task.id ? '处理中...' : '完成配送' }}
                </button>
              </div>
            </article>
          </div>
        </article>
      </section>
    </template>

    <template v-else>
      <section class="panel-card">
        <div class="flex items-start justify-between gap-4">
          <div>
            <h2 class="panel-title">配送任务总览</h2>
            <p class="panel-subtitle">管理员可查看系统内全部预约配送任务，并跟踪执行状态。</p>
          </div>
          <span class="rounded-full bg-brand/10 px-3 py-1 text-sm font-medium text-brand">{{ deliveries.length }} 条</span>
        </div>

        <div v-if="deliveries.length === 0" class="empty-state mt-6">
          <div class="text-5xl">🚚</div>
          <h3 class="mt-5 text-2xl font-semibold text-slate-900">暂无配送任务</h3>
          <p class="mt-3 text-sm leading-7 text-slate-500">当学生发起预约配送后，任务会显示在这里。</p>
        </div>

        <div v-else class="mt-6 space-y-4">
          <article
            v-for="task in deliveries"
            :key="task.id"
            class="flex flex-col gap-4 rounded-[24px] border border-slate-200 bg-white p-5 shadow-sm xl:flex-row xl:items-center xl:justify-between"
          >
            <div class="min-w-0 flex-1">
              <div class="flex flex-wrap items-center gap-2">
                <span class="rounded-full bg-slate-100 px-3 py-1 text-xs text-slate-600">任务 #{{ task.id }}</span>
                <span class="rounded-full px-3 py-1 text-xs font-medium" :class="statusClass(task.status)">{{ statusLabel(task.status) }}</span>
              </div>
              <p class="mt-4 text-lg font-semibold text-slate-900">
                包裹 #{{ task.packageId }} → {{ destinationLabel(task.destination) }}
              </p>
              <p class="mt-2 text-sm text-slate-500">
                预计距离 {{ formatNumber(task.estimatedDistance) }} m · 预计耗时 {{ formatNumber(task.estimatedTime) }} 分钟
              </p>
            </div>

            <button
              v-if="task.status !== 'COMPLETED'"
              type="button"
              class="primary-cta h-11 px-4"
              :disabled="completeLoading === task.id"
              @click="completeTask(task.id)"
            >
              {{ completeLoading === task.id ? '处理中...' : '标记完成' }}
            </button>
          </article>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import {
  completeDelivery,
  grabDelivery,
  listDeliveries,
  listMyDeliveries,
  listPendingDeliveries,
  startDelivery,
} from '@/api/delivery'

const authStore = useAuthStore()
const loading = ref(false)
const grabLoading = ref(null)
const startLoading = ref(null)
const completeLoading = ref(null)
const pendingList = ref([])
const myList = ref([])
const deliveries = ref([])
const feedback = ref('')

function destinationLabel(value) {
  return {
    STATION_1: '驿站',
    DORM_1: '宿舍 1 号楼',
    DORM_2: '宿舍 2 号楼',
    DORM_3: '宿舍 3 号楼',
    CAFETERIA: '食堂',
  }[value] || value
}

function statusLabel(value) {
  return {
    PENDING: '待处理',
    ASSIGNED: '已抢单',
    IN_PROGRESS: '配送中',
    COMPLETED: '已完成',
    FAILED: '失败',
  }[value] || value
}

function statusClass(value) {
  if (value === 'COMPLETED') return 'bg-emerald-100 text-emerald-700'
  if (value === 'ASSIGNED' || value === 'IN_PROGRESS') return 'bg-brand/10 text-brand'
  if (value === 'PENDING') return 'bg-amber-100 text-amber-700'
  return 'bg-slate-100 text-slate-500'
}

function formatNumber(value) {
  return Number(value ?? 0).toFixed(0)
}

async function load() {
  loading.value = true
  try {
    if (authStore.isCourier && !authStore.isAdmin) {
      const [pending, my] = await Promise.all([listPendingDeliveries(), listMyDeliveries()])
      pendingList.value = Array.isArray(pending) ? pending.filter((item) => item.type === 'SCHEDULED') : []
      myList.value = Array.isArray(my) ? my.filter((item) => item.type === 'SCHEDULED') : []
      deliveries.value = []
    } else {
      const list = await listDeliveries()
      deliveries.value = Array.isArray(list) ? list.filter((item) => item.type === 'SCHEDULED') : []
      pendingList.value = []
      myList.value = []
    }
  } catch (error) {
    feedback.value = error?.message || '配送任务加载失败'
  } finally {
    loading.value = false
  }
}

async function grabTask(id) {
  grabLoading.value = id
  try {
    await grabDelivery(id)
    await load()
    feedback.value = '抢单成功'
  } catch (error) {
    feedback.value = error?.message || '抢单失败'
  } finally {
    grabLoading.value = null
  }
}

async function startTask(id) {
  startLoading.value = id
  try {
    await startDelivery(id)
    await load()
    feedback.value = '配送任务已开始'
  } catch (error) {
    feedback.value = error?.message || '开始配送失败'
  } finally {
    startLoading.value = null
  }
}

async function completeTask(id) {
  completeLoading.value = id
  try {
    await completeDelivery(id)
    await load()
    feedback.value = '配送任务已完成'
  } catch (error) {
    feedback.value = error?.message || '完成配送失败'
  } finally {
    completeLoading.value = null
  }
}

onMounted(load)
</script>
