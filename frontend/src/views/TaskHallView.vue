<template>
  <div class="space-y-6">
    <section class="flex flex-wrap items-start justify-between gap-4">
      <div>
        <p class="text-xs font-semibold uppercase tracking-[0.2em] text-brand">Task Center</p>
        <h1 class="mt-2 text-3xl font-bold text-slate-900">任务大厅</h1>
        <p class="mt-3 max-w-3xl text-sm leading-7 text-slate-500">
          面向论文中的“悬赏代取”场景，支持任务发布、隐私脱敏展示、接单与结算闭环。
        </p>
      </div>

      <div class="flex flex-wrap gap-3">
        <button type="button" class="secondary-cta h-11 px-4" :disabled="walletLoading" @click="loadWallet">
          {{ walletLoading ? '刷新中...' : '刷新钱包' }}
        </button>
        <button type="button" class="primary-cta h-11 px-4" :disabled="loading" @click="load">
          {{ loading ? '同步中...' : '刷新任务' }}
        </button>
      </div>
    </section>

    <section
      v-if="feedback"
      class="rounded-[22px] border border-brand/20 bg-brand/10 px-5 py-4 text-sm text-brand shadow-sm"
    >
      {{ feedback }}
    </section>

    <section class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
      <article class="rounded-[28px] border border-slate-200 bg-white p-6 shadow-sm">
        <p class="text-sm text-slate-500">可接任务</p>
        <p class="mt-4 text-4xl font-bold text-slate-900">{{ pending.length }}</p>
        <p class="mt-2 text-sm text-slate-500">支持学生与代取员共同接单</p>
      </article>
      <article class="rounded-[28px] border border-slate-200 bg-white p-6 shadow-sm">
        <p class="text-sm text-slate-500">我的任务</p>
        <p class="mt-4 text-4xl font-bold text-slate-900">{{ my.length }}</p>
        <p class="mt-2 text-sm text-slate-500">查看当前负责的代取任务</p>
      </article>
      <article v-if="authStore.isStudent" class="rounded-[28px] border border-slate-200 bg-white p-6 shadow-sm">
        <p class="text-sm text-slate-500">我发布的悬赏</p>
        <p class="mt-4 text-4xl font-bold text-slate-900">{{ published.length }}</p>
        <p class="mt-2 text-sm text-slate-500">支持取消未接单任务并自动退款</p>
      </article>
      <article class="rounded-[28px] border border-slate-200 bg-white p-6 shadow-sm">
        <p class="text-sm text-slate-500">冻结金额</p>
        <p class="mt-4 text-4xl font-bold text-slate-900">¥{{ money(wallet?.frozen) }}</p>
        <p class="mt-2 text-sm text-slate-500">用于验证冻结与结算流程</p>
      </article>
    </section>

    <section class="grid gap-6 xl:grid-cols-2">
      <article class="panel-card">
        <div class="flex items-start justify-between gap-3">
          <div>
            <h2 class="panel-title">可接悬赏</h2>
            <p class="panel-subtitle">发布者身份信息已做脱敏处理，符合论文中的隐私保护要求。</p>
          </div>
          <span class="rounded-full bg-brand/10 px-3 py-1 text-sm font-medium text-brand">{{ pending.length }} 条</span>
        </div>

        <div v-if="pending.length === 0" class="empty-state mt-5">
          <div class="text-5xl">📭</div>
          <h3 class="mt-5 text-2xl font-semibold text-slate-900">暂无可接悬赏</h3>
          <p class="mt-3 text-sm leading-7 text-slate-500">稍后刷新看看，新任务发布后会出现在这里。</p>
        </div>

        <div v-else class="mt-5 space-y-4">
          <article v-for="task in pending" :key="task.id" class="rounded-[24px] border border-slate-200 bg-white p-5 shadow-sm">
            <div class="flex flex-wrap items-center gap-2">
              <span class="rounded-full bg-slate-100 px-3 py-1 text-xs text-slate-600">任务 #{{ task.id }}</span>
              <span class="rounded-full bg-brand/10 px-3 py-1 text-xs text-brand">{{ destinationLabel(task.destination) }}</span>
            </div>
            <p class="mt-4 text-lg font-semibold text-slate-900">包裹 #{{ task.packageId }}</p>
            <p class="mt-2 text-sm text-slate-500">
              发布者：{{ task.publisherMaskedName || '匿名用户' }} · {{ task.publisherMaskedPhone || '未绑定手机号' }}
            </p>
            <p class="mt-2 text-sm text-slate-500">
              悬赏金额：<span class="font-semibold text-brand">¥{{ money(task.rewardAmount) }}</span>
            </p>
            <button
              type="button"
              class="primary-cta mt-5 h-11 px-4"
              :disabled="actionLoading === task.id"
              @click="accept(task.id)"
            >
              {{ actionLoading === task.id ? '接单中...' : '立即接单' }}
            </button>
          </article>
        </div>
      </article>

      <article class="panel-card">
        <div class="flex items-start justify-between gap-3">
          <div>
            <h2 class="panel-title">我的代取任务</h2>
            <p class="panel-subtitle">跟踪我已接单的任务，并进行完成结算。</p>
          </div>
          <span class="rounded-full bg-amber-100 px-3 py-1 text-sm font-medium text-amber-700">{{ my.length }} 条</span>
        </div>

        <div v-if="my.length === 0" class="empty-state mt-5">
          <div class="text-5xl">🧾</div>
          <h3 class="mt-5 text-2xl font-semibold text-slate-900">暂无已接任务</h3>
          <p class="mt-3 text-sm leading-7 text-slate-500">接单成功后，这里会展示你当前负责的代取任务。</p>
        </div>

        <div v-else class="mt-5 space-y-4">
          <article v-for="task in my" :key="task.id" class="rounded-[24px] border border-slate-200 bg-white p-5 shadow-sm">
            <div class="flex flex-wrap items-center gap-2">
              <span class="rounded-full bg-slate-100 px-3 py-1 text-xs text-slate-600">任务 #{{ task.id }}</span>
              <span class="rounded-full px-3 py-1 text-xs font-medium" :class="task.status === 'COMPLETED' ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'">
                {{ rewardStatusLabel(task.status) }}
              </span>
            </div>
            <p class="mt-4 text-lg font-semibold text-slate-900">
              包裹 #{{ task.packageId }} → {{ destinationLabel(task.destination) }}
            </p>
            <p class="mt-2 text-sm text-slate-500">
              奖励金额：<span class="font-semibold text-brand">¥{{ money(task.rewardAmount) }}</span>
            </p>
            <button
              v-if="task.status !== 'COMPLETED'"
              type="button"
              class="primary-cta mt-5 h-11 px-4"
              :disabled="actionLoading === task.id"
              @click="complete(task.id)"
            >
              {{ actionLoading === task.id ? '提交中...' : '完成并结算' }}
            </button>
          </article>
        </div>
      </article>
    </section>

    <section v-if="authStore.isStudent" class="panel-card">
      <div class="flex items-start justify-between gap-3">
        <div>
          <h2 class="panel-title">我发布的悬赏</h2>
          <p class="panel-subtitle">查看已发布任务的状态，并支持取消未被接单的任务。</p>
        </div>
        <span class="rounded-full bg-slate-100 px-3 py-1 text-sm font-medium text-slate-600">{{ published.length }} 条</span>
      </div>

      <div v-if="published.length === 0" class="empty-state mt-5">
        <div class="text-5xl">🎯</div>
        <h3 class="mt-5 text-2xl font-semibold text-slate-900">还没有发布悬赏</h3>
        <p class="mt-3 text-sm leading-7 text-slate-500">可返回包裹中心，对待取件包裹发布悬赏代取任务。</p>
      </div>

      <div v-else class="mt-5 grid gap-4 xl:grid-cols-2">
        <article v-for="task in published" :key="task.id" class="rounded-[24px] border border-slate-200 bg-white p-5 shadow-sm">
          <div class="flex flex-wrap items-center gap-2">
            <span class="rounded-full bg-slate-100 px-3 py-1 text-xs text-slate-600">任务 #{{ task.id }}</span>
            <span class="rounded-full px-3 py-1 text-xs font-medium" :class="task.status === 'COMPLETED' ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'">
              {{ rewardStatusLabel(task.status) }}
            </span>
          </div>
          <p class="mt-4 text-lg font-semibold text-slate-900">
            包裹 #{{ task.packageId }} → {{ destinationLabel(task.destination) }}
          </p>
          <p class="mt-2 text-sm text-slate-500">
            悬赏金额：<span class="font-semibold text-brand">¥{{ money(task.rewardAmount) }}</span>
          </p>
          <button
            v-if="task.status === 'PENDING'"
            type="button"
            class="secondary-cta mt-5 h-11 px-4"
            :disabled="actionLoading === task.id"
            @click="cancel(task.id)"
          >
            {{ actionLoading === task.id ? '取消中...' : '取消并退款' }}
          </button>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getMyWallet } from '@/api/wallet'
import {
  acceptRewardTask,
  cancelRewardTask,
  completeRewardTask,
  listMyRewardTasks,
  listPendingRewardTasks,
  listPublishedRewardTasks,
} from '@/api/delivery'

const authStore = useAuthStore()
const loading = ref(false)
const walletLoading = ref(false)
const actionLoading = ref(null)
const pending = ref([])
const my = ref([])
const published = ref([])
const wallet = ref(null)
const feedback = ref('')

function money(value) {
  return Number(value ?? 0).toFixed(2)
}

function destinationLabel(value) {
  return {
    STATION_1: '驿站',
    DORM_1: '宿舍 1 号楼',
    DORM_2: '宿舍 2 号楼',
    DORM_3: '宿舍 3 号楼',
    CAFETERIA: '食堂',
  }[value] || value
}

function rewardStatusLabel(status) {
  return {
    PENDING: '待接单',
    ASSIGNED: '已接单',
    IN_PROGRESS: '进行中',
    COMPLETED: '已完成',
    FAILED: '已取消',
  }[status] || status
}

async function loadWallet() {
  walletLoading.value = true
  try {
    wallet.value = await getMyWallet()
  } catch {
    wallet.value = null
  } finally {
    walletLoading.value = false
  }
}

async function load() {
  loading.value = true
  try {
    const [pendingList, myList] = await Promise.all([
      listPendingRewardTasks(),
      listMyRewardTasks(),
    ])
    pending.value = Array.isArray(pendingList) ? pendingList : []
    my.value = Array.isArray(myList) ? myList : []

    if (authStore.isStudent) {
      const publishedList = await listPublishedRewardTasks()
      published.value = Array.isArray(publishedList) ? publishedList : []
    } else {
      published.value = []
    }
  } catch (error) {
    feedback.value = error?.message || '任务加载失败'
  } finally {
    loading.value = false
  }
}

async function accept(id) {
  actionLoading.value = id
  try {
    await acceptRewardTask(id)
    await Promise.all([load(), loadWallet()])
    feedback.value = '接单成功'
  } catch (error) {
    feedback.value = error?.message || '接单失败'
  } finally {
    actionLoading.value = null
  }
}

async function cancel(id) {
  actionLoading.value = id
  try {
    await cancelRewardTask(id)
    await Promise.all([load(), loadWallet()])
    feedback.value = '任务已取消，冻结金额已退回'
  } catch (error) {
    feedback.value = error?.message || '取消失败'
  } finally {
    actionLoading.value = null
  }
}

async function complete(id) {
  actionLoading.value = id
  try {
    await completeRewardTask(id)
    await Promise.all([load(), loadWallet()])
    feedback.value = '任务已完成，奖励已结算'
  } catch (error) {
    feedback.value = error?.message || '操作失败'
  } finally {
    actionLoading.value = null
  }
}

onMounted(async () => {
  await Promise.all([load(), loadWallet()])
})
</script>
