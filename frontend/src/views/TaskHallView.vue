<template>
  <div>
    <h1 class="text-2xl font-bold text-text-primary mb-6">任务大厅</h1>

    <div class="bg-white rounded-card shadow-card p-6 mb-6">
      <div class="flex items-center justify-between gap-4 flex-wrap">
        <div>
          <p class="text-text-secondary text-sm">我的钱包</p>
          <p class="text-text-tertiary text-xs mt-1">发布悬赏会冻结金额，任务完成后结算</p>
        </div>
        <div class="flex items-center gap-6">
          <div class="text-right">
            <p class="text-text-tertiary text-xs">余额</p>
            <p class="font-mono text-lg font-semibold text-text-primary">
              {{ wallet ? moneyYuan(wallet.balance) : '-' }}
            </p>
          </div>
          <div class="text-right">
            <p class="text-text-tertiary text-xs">冻结</p>
            <p class="font-mono text-lg font-semibold text-warning-text">
              {{ wallet ? moneyYuan(wallet.frozen) : '-' }}
            </p>
          </div>
          <button class="btn-secondary text-sm" @click="loadWallet" :disabled="walletLoading">
            {{ walletLoading ? '刷新中...' : '刷新余额' }}
          </button>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
      <div class="bg-white rounded-card shadow-card p-6">
        <div class="flex items-center justify-between mb-4">
          <h2 class="font-semibold text-text-primary">可接悬赏</h2>
          <button class="btn-secondary text-sm" @click="load" :disabled="loading">
            {{ loading ? '刷新中...' : '刷新' }}
          </button>
        </div>
        <div v-if="pending.length === 0" class="text-center py-10 text-text-tertiary text-sm">
          暂无可接悬赏任务
        </div>
        <div v-else class="space-y-3">
          <div
            v-for="t in pending"
            :key="t.id"
            class="flex items-center justify-between p-4 rounded-input border border-fill-border hover:border-brand/50"
          >
            <div class="min-w-0">
              <p class="font-medium text-text-primary truncate">
                包裹 #{{ t.packageId }} → {{ destinationLabel(t.destination) }}
              </p>
              <p class="text-text-secondary text-sm mt-1">
                悬赏 <span class="font-mono text-brand">{{ money(t.rewardAmount) }}</span> · 状态 {{ t.status }}
              </p>
            </div>
            <button class="btn-primary text-sm shrink-0" @click="accept(t.id)" :disabled="actionLoading === t.id">
              {{ actionLoading === t.id ? '接单中...' : '接单' }}
            </button>
          </div>
        </div>
      </div>

      <div class="bg-white rounded-card shadow-card p-6">
        <h2 class="font-semibold text-text-primary mb-4">我接到的悬赏</h2>
        <div v-if="my.length === 0" class="text-center py-10 text-text-tertiary text-sm">
          暂无已接任务
        </div>
        <div v-else class="space-y-3">
          <div
            v-for="t in my"
            :key="t.id"
            class="flex items-center justify-between p-4 rounded-input border border-fill-border"
          >
            <div class="min-w-0">
              <p class="font-medium text-text-primary truncate">
                #{{ t.id }} 包裹 #{{ t.packageId }} → {{ destinationLabel(t.destination) }}
              </p>
              <p class="text-text-secondary text-sm mt-1">
                悬赏 <span class="font-mono text-brand">{{ money(t.rewardAmount) }}</span> · 状态 {{ t.status }}
              </p>
            </div>
            <button
              v-if="t.status !== 'COMPLETED'"
              class="btn-primary text-sm shrink-0"
              @click="complete(t.id)"
              :disabled="actionLoading === t.id"
            >
              {{ actionLoading === t.id ? '提交中...' : '完成并结算' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="authStore.isStudent" class="bg-white rounded-card shadow-card p-6">
      <h2 class="font-semibold text-text-primary mb-4">我发布的悬赏</h2>
      <div v-if="published.length === 0" class="text-center py-10 text-text-tertiary text-sm">
        暂无已发布悬赏
      </div>
      <div v-else class="space-y-3">
        <div
          v-for="t in published"
          :key="t.id"
          class="flex items-center justify-between p-4 rounded-input border border-fill-border"
        >
          <div class="min-w-0">
            <p class="font-medium text-text-primary truncate">
              #{{ t.id }} 包裹 #{{ t.packageId }} → {{ destinationLabel(t.destination) }}
            </p>
            <p class="text-text-secondary text-sm mt-1">
              悬赏 <span class="font-mono text-brand">{{ money(t.rewardAmount) }}</span> · 状态 {{ t.status }}
            </p>
          </div>
          <button
            v-if="t.status === 'PENDING'"
            class="btn-secondary text-sm shrink-0"
            @click="cancel(t.id)"
            :disabled="actionLoading === t.id"
          >
            {{ actionLoading === t.id ? '取消中...' : '取消并退回冻结' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getMyWallet } from '@/api/wallet'
import {
  listPendingRewardTasks,
  listMyRewardTasks,
  listPublishedRewardTasks,
  acceptRewardTask,
  cancelRewardTask,
  completeRewardTask,
} from '@/api/delivery'

const authStore = useAuthStore()
const loading = ref(false)
const actionLoading = ref(null)
const pending = ref([])
const my = ref([])
const published = ref([])
const wallet = ref(null)
const walletLoading = ref(false)

const DESTINATION_LABELS = {
  STATION_1: '驿站1',
  DORM_1: '宿舍1号楼',
  DORM_2: '宿舍2号楼',
  DORM_3: '宿舍3号楼',
  CAFETERIA: '食堂',
}

const destinationLabel = (d) => DESTINATION_LABELS[d] || d
const money = (v) => (v == null ? '0.00' : Number(v).toFixed(2)) + ' 元'
const moneyYuan = (v) => (v == null ? '0.00' : Number(v).toFixed(2)) + ' 元'

const loadWallet = async () => {
  walletLoading.value = true
  try {
    wallet.value = await getMyWallet()
  } catch (e) {
    // 钱包不是核心链路，失败不阻断页面
    wallet.value = null
  } finally {
    walletLoading.value = false
  }
}

const load = async () => {
  loading.value = true
  try {
    const [p, m] = await Promise.all([listPendingRewardTasks(), listMyRewardTasks()])
    pending.value = Array.isArray(p) ? p : []
    my.value = Array.isArray(m) ? m : []
    if (authStore.isStudent) {
      const pub = await listPublishedRewardTasks()
      published.value = Array.isArray(pub) ? pub : []
    } else {
      published.value = []
    }
  } catch (e) {
    alert(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const accept = async (id) => {
  actionLoading.value = id
  try {
    await acceptRewardTask(id)
    await load()
    await loadWallet()
    alert('接单成功')
  } catch (e) {
    alert(e?.message || '接单失败')
  } finally {
    actionLoading.value = null
  }
}

const cancel = async (id) => {
  actionLoading.value = id
  try {
    await cancelRewardTask(id)
    await load()
    await loadWallet()
    alert('已取消，冻结金额已退回余额')
  } catch (e) {
    alert(e?.message || '取消失败')
  } finally {
    actionLoading.value = null
  }
}

const complete = async (id) => {
  actionLoading.value = id
  try {
    await completeRewardTask(id)
    await load()
    await loadWallet()
    alert('任务已完成，悬赏已结算')
  } catch (e) {
    alert(e?.message || '操作失败')
  } finally {
    actionLoading.value = null
  }
}

onMounted(async () => {
  await Promise.all([load(), loadWallet()])
})
</script>

