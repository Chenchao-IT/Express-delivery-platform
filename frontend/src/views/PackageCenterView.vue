<template>
  <div>
    <h1 class="text-2xl font-bold text-text-primary mb-6" id="page-title">
      包裹中心
    </h1>

    <!-- 加载中 -->
    <div v-if="loading" class="space-y-6">
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div v-for="i in 3" :key="i" class="bg-white rounded-card shadow-card p-6 animate-pulse">
          <div class="h-8 bg-fill-border rounded mb-4 w-1/2"></div>
          <div class="h-4 bg-fill-border rounded w-3/4"></div>
        </div>
      </div>
      <div class="bg-white rounded-card shadow-card p-6 animate-pulse">
        <div class="h-6 bg-fill-border rounded mb-4 w-1/3"></div>
        <div class="space-y-3">
          <div v-for="i in 4" :key="i" class="h-16 bg-fill-border rounded"></div>
        </div>
      </div>
    </div>

    <!-- 降级提示（仅作横幅，不隐藏主内容） -->
    <div
      v-if="!loading && homeData?.metadata?.degraded"
      class="mb-4 p-4 rounded-card bg-warning/10 border border-warning/30 text-warning-text"
      role="alert"
    >
      系统繁忙，已启用简化模式，部分功能可能受限
    </div>

    <!-- 主内容：统计、包裹列表、成就等 -->
    <template v-if="!loading">
      <!-- 统计卡片 -->
      <div class="grid grid-cols-1 md:grid-cols-5 gap-4 mb-6">
        <div
          class="bg-white rounded-card shadow-card p-5 flex items-center gap-4"
          role="region"
          aria-label="待取件统计"
        >
          <div class="w-12 h-12 rounded-full bg-brand/10 flex items-center justify-center">
            <span class="text-xl">📦</span>
          </div>
          <div>
            <p class="text-2xl font-bold text-text-primary">{{ stats.incomingCount }}</p>
            <p class="text-text-secondary text-sm">待取件</p>
          </div>
        </div>
        <div
          class="bg-white rounded-card shadow-card p-5 flex items-center gap-4"
          role="region"
          aria-label="配送中统计"
        >
          <div class="w-12 h-12 rounded-full bg-brand/10 flex items-center justify-center">
            <span class="text-xl">🚚</span>
          </div>
          <div>
            <p class="text-2xl font-bold text-text-primary">{{ stats.deliveringCount }}</p>
            <p class="text-text-secondary text-sm">配送中</p>
          </div>
        </div>
        <div
          class="bg-white rounded-card shadow-card p-5 flex items-center gap-4"
          role="region"
          aria-label="已送达统计"
        >
          <div class="w-12 h-12 rounded-full bg-success/10 flex items-center justify-center">
            <span class="text-xl">📬</span>
          </div>
          <div>
            <p class="text-2xl font-bold text-text-primary">{{ stats.deliveredCount }}</p>
            <p class="text-text-secondary text-sm">已送达</p>
          </div>
        </div>

        <div
          class="bg-white rounded-card shadow-card p-5 flex items-center gap-4"
          role="region"
          aria-label="钱包余额"
        >
          <div class="w-12 h-12 rounded-full bg-brand/10 flex items-center justify-center">
            <span class="text-xl">💰</span>
          </div>
          <div>
            <p class="text-2xl font-bold text-text-primary font-mono">
              {{ wallet ? Number(wallet.balance || 0).toFixed(2) : '-' }}
            </p>
            <p class="text-text-secondary text-sm">余额（元）</p>
          </div>
        </div>

        <div
          class="bg-white rounded-card shadow-card p-5 flex items-center gap-4"
          role="region"
          aria-label="钱包冻结"
        >
          <div class="w-12 h-12 rounded-full bg-warning/10 flex items-center justify-center">
            <span class="text-xl">🧊</span>
          </div>
          <div>
            <p class="text-2xl font-bold text-warning-text font-mono">
              {{ wallet ? Number(wallet.frozen || 0).toFixed(2) : '-' }}
            </p>
            <p class="text-text-secondary text-sm">冻结（元）</p>
          </div>
        </div>
      </div>

      <!-- 包裹列表 -->
      <div class="bg-white rounded-card shadow-card p-6 mb-6">
        <div class="flex justify-between items-center mb-4">
          <h2 class="font-semibold text-text-primary">我的包裹</h2>
          <button
            v-if="selectedIncoming.length >= 2"
            @click="showMergeConfirm = true"
            class="btn-primary text-sm"
            :disabled="mergeLoading"
          >
            {{ mergeLoading ? '处理中...' : `合并取件 (${selectedIncoming.length})` }}
          </button>
        </div>

        <div v-if="packages.length === 0" class="text-center py-12 text-text-secondary">
          <div class="text-6xl mb-4 opacity-50">📦</div>
          <p>暂无包裹</p>
        </div>

        <div v-else class="space-y-4">
          <div
            v-for="pkg in packages"
            :key="pkg.id"
            class="flex items-center justify-between p-4 rounded-input border border-fill-border hover:bg-fill-disabled transition-colors"
            :class="{ 'ring-2 ring-brand': selectedIds.has(pkg.id) }"
          >
            <div class="flex items-center gap-4 flex-1 min-w-0">
              <input
                v-if="pkg.status === 'IN_STORAGE'"
                type="checkbox"
                :id="`pkg-${pkg.id}`"
                :checked="selectedIds.has(pkg.id)"
                @change="toggleSelect(pkg.id)"
                @keydown.enter.prevent="toggleSelect(pkg.id)"
                :aria-label="`选择包裹 ${pkg.trackingNumber}`"
              />
              <div class="flex-1 min-w-0">
                <p class="font-medium text-text-primary truncate">{{ pkg.trackingNumber }}</p>
                <p class="text-text-secondary text-sm mt-1">
                  货架: {{ pkg.shelfCode || '-' }} · 区域: {{ pkg.zone || '-' }} · {{ statusLabel(pkg.status) }}
                </p>
                <p v-if="pkg.pickupCode" class="text-brand text-sm mt-1 font-mono">
                  取件码: {{ pkg.pickupCode }}
                </p>
              <button
                v-if="pkg.status === 'IN_STORAGE' && pkg.pickupCode"
                type="button"
                class="text-sm text-brand hover:underline mt-1"
                @click="speakPickupCode(pkg)"
                aria-label="语音播报取件码"
              >
                🎤 语音播报
              </button>
              </div>
            </div>
            <div class="flex items-center gap-2 shrink-0 flex-wrap">
              <button
                v-if="pkg.status === 'IN_STORAGE'"
                @click="openScheduleModal(pkg)"
                :disabled="scheduleLoading"
                class="btn-secondary text-sm"
              >
                预约配送
              </button>
              <button
                v-if="pkg.status === 'IN_STORAGE'"
                @click="openRewardModal(pkg)"
                :disabled="rewardLoading"
                class="btn-secondary text-sm"
              >
                发布悬赏
              </button>
              <button
                v-if="pkg.status === 'IN_STORAGE'"
                @click="pickup(pkg.id)"
                :disabled="pickupLoading === pkg.id"
                class="btn-primary text-sm"
              >
                {{ pickupLoading === pkg.id ? '取件中...' : '确认取件' }}
              </button>
              <router-link
                v-if="pkg.status === 'OUT_FOR_DELIVERY'"
                :to="`/track?q=${pkg.trackingNumber}`"
                class="btn-secondary text-sm"
              >
                查看位置
              </router-link>
              <span v-else class="text-text-tertiary text-sm">
                {{ pkg.status === 'PICKED_UP' ? '已取件' : statusLabel(pkg.status) }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 地图占位（文档 1.2 智能地图） -->
      <div
        v-if="hasDeliveringPackages"
        class="bg-white rounded-card shadow-card overflow-hidden mb-6"
        role="region"
        aria-label="配送位置"
      >
        <div class="p-4 border-b border-fill-border">
          <h3 class="font-semibold text-text-primary">配送位置</h3>
          <p class="text-text-secondary text-sm mt-1">
            您的包裹正在配送中，预计很快送达
          </p>
        </div>
        <div class="map-skeleton h-64 flex items-center justify-center bg-fill-disabled">
          <div class="text-center text-text-tertiary">
            <p class="text-4xl mb-2">📍</p>
            <p>包裹位于校园驿站 {{ deliveringStation }}</p>
            <p class="text-sm mt-1">取件时请出示取件码</p>
          </div>
        </div>
      </div>

      <!-- 成就进度（文档 1.3） -->
      <div class="bg-white rounded-card shadow-card p-6 mb-6">
        <h2 class="font-semibold text-text-primary mb-4">成就进度</h2>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div
            v-for="a in achievements"
            :key="a.achievementId"
            class="p-4 rounded-input border border-fill-border"
            :class="{ 'border-success bg-success/5': a.unlocked }"
          >
            <div class="flex items-center justify-between mb-2">
              <span class="font-medium">{{ a.title }}</span>
              <span v-if="a.unlocked" class="text-success text-sm">✓ 已解锁</span>
            </div>
            <p class="text-text-secondary text-sm mb-2">{{ a.condition }}</p>
            <div class="h-2 bg-fill-border rounded-full overflow-hidden">
              <div
                class="h-full bg-brand transition-all duration-300"
                :style="{ width: `${Math.min(a.progress * 100, 100)}%` }"
              ></div>
            </div>
            <p v-if="!a.unlocked && a.estimatedUnlockTime" class="text-text-tertiary text-xs mt-2">
              {{ a.estimatedUnlockTime }}
            </p>
          </div>
        </div>
      </div>

      <!-- 推荐 -->
      <div v-if="recommendations.length > 0" class="bg-white rounded-card shadow-card p-6">
        <h2 class="font-semibold text-text-primary mb-4">推荐</h2>
        <div class="space-y-3">
          <div
            v-for="r in recommendations"
            :key="r.type"
            class="p-4 rounded-input border border-fill-border hover:border-brand/50 transition-colors cursor-pointer"
            @click="r.type === 'SCHEDULED_DELIVERY' && incomingPackages[0] && openScheduleModal(incomingPackages[0])"
          >
            <p class="font-medium text-text-primary">{{ r.title }}</p>
            <p class="text-text-secondary text-sm mt-1">{{ r.description }}</p>
            <p
              v-if="r.type === 'SCHEDULED_DELIVERY' && incomingPackages.length > 0"
              class="text-brand text-sm mt-2"
            >
              点击预约 →
            </p>
          </div>
        </div>
      </div>
    </template>

    <!-- 合并取件确认弹窗（焦点陷阱 文档 3.2） -->
    <Teleport to="body">
      <div
        v-if="showMergeConfirm"
        class="fixed inset-0 z-modal-backdrop bg-black/50 flex items-center justify-center p-4"
        role="dialog"
        aria-modal="true"
        aria-labelledby="merge-dialog-title"
        @keydown.escape="showMergeConfirm = false"
        @keydown.tab="trapFocus"
      >
        <div
          class="bg-white rounded-card shadow-card max-w-md w-full p-6"
          ref="mergeDialogRef"
        >
        <h3 id="merge-dialog-title" class="font-semibold text-text-primary mb-4">
          合并取件确认
        </h3>
        <p class="text-text-secondary text-sm mb-4">
          已选择 {{ selectedIncoming.length }} 个包裹，确认后将生成合并取件码，请一次性出示所有取件码。
        </p>
        <div class="flex gap-3 justify-end">
          <button class="btn-secondary" @click="showMergeConfirm = false">
            取消
          </button>
          <button
            class="btn-primary"
            :disabled="mergeLoading"
            @click="doMergePickup"
          >
            {{ mergeLoading ? '处理中...' : '确认合并取件' }}
          </button>
        </div>
      </div>
    </div>
    </Teleport>

    <!-- 预约配送弹窗 -->
    <Teleport to="body">
      <div
        v-if="showScheduleModal"
        class="fixed inset-0 z-modal-backdrop bg-black/50 flex items-center justify-center p-4"
        role="dialog"
        aria-modal="true"
        aria-labelledby="schedule-dialog-title"
        @keydown.escape="showScheduleModal = false"
      >
        <div class="bg-white rounded-card shadow-card max-w-md w-full p-6">
          <h3 id="schedule-dialog-title" class="font-semibold text-text-primary mb-4">
            预约配送
          </h3>
          <p v-if="schedulePkg" class="text-text-secondary text-sm mb-4">
            包裹 {{ schedulePkg.trackingNumber }} 将配送到您选择的地点
          </p>
          <div class="mb-4">
            <label class="block text-sm font-medium text-text-primary mb-2">配送目的地</label>
            <select
              v-model="scheduleDestination"
              class="input-base w-full"
              aria-label="选择配送目的地"
            >
              <option value="">请选择目的地</option>
              <option
                v-for="d in deliveryDestinations"
                :key="d"
                :value="d"
              >
                {{ destinationLabel(d) }}
              </option>
            </select>
          </div>
          <div class="flex gap-3 justify-end">
            <button class="btn-secondary" @click="showScheduleModal = false">
              取消
            </button>
            <button
              class="btn-primary"
              :disabled="!scheduleDestination || scheduleLoading"
              @click="doScheduleDelivery"
            >
              {{ scheduleLoading ? '预约中...' : '确认预约' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 悬赏代取弹窗 -->
    <Teleport to="body">
      <div
        v-if="showRewardModal"
        class="fixed inset-0 z-modal-backdrop bg-black/50 flex items-center justify-center p-4"
        role="dialog"
        aria-modal="true"
        aria-labelledby="reward-dialog-title"
        @keydown.escape="showRewardModal = false"
      >
        <div class="bg-white rounded-card shadow-card max-w-md w-full p-6">
          <h3 id="reward-dialog-title" class="font-semibold text-text-primary mb-4">
            发布悬赏代取
          </h3>
          <p v-if="rewardPkg" class="text-text-secondary text-sm mb-4">
            包裹 {{ rewardPkg.trackingNumber }} 将配送到您选择的地点，悬赏金额会被冻结，任务完成后结算给接单人。
          </p>
          <div class="mb-4">
            <label class="block text-sm font-medium text-text-primary mb-2">配送目的地</label>
            <select
              v-model="rewardDestination"
              class="input-base w-full"
              aria-label="选择配送目的地"
            >
              <option value="">请选择目的地</option>
              <option
                v-for="d in deliveryDestinations"
                :key="d"
                :value="d"
              >
                {{ destinationLabel(d) }}
              </option>
            </select>
          </div>
          <div class="mb-4">
            <label class="block text-sm font-medium text-text-primary mb-2">悬赏金额（元）</label>
            <input
              v-model="rewardAmount"
              class="input-base w-full"
              inputmode="decimal"
              placeholder="例如 3.00"
              aria-label="输入悬赏金额"
            />
          </div>
          <div class="flex gap-3 justify-end">
            <button class="btn-secondary" @click="showRewardModal = false">
              取消
            </button>
            <button
              class="btn-primary"
              :disabled="!rewardDestination || !rewardAmount || rewardLoading"
              @click="doPublishReward"
            >
              {{ rewardLoading ? '发布中...' : '确认发布' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getHomeData, executeMergedPickup, getAchievements } from '@/api/packageCenter'
import { pickupPackage as singlePickup } from '@/api/package'
import { scheduleDelivery, getDestinations, publishRewardTask } from '@/api/delivery'
import { getMyWallet } from '@/api/wallet'

const loading = ref(true)
const homeData = ref(null)
const achievements = ref([])
const selectedIds = ref(new Set())
const showMergeConfirm = ref(false)
const mergeLoading = ref(false)
const pickupLoading = ref(null)
const mergeDialogRef = ref(null)
const showScheduleModal = ref(false)
const schedulePkg = ref(null)
const scheduleDestination = ref('')
const scheduleLoading = ref(false)
const showRewardModal = ref(false)
const rewardPkg = ref(null)
const rewardDestination = ref('')
const rewardAmount = ref('')
const rewardLoading = ref(false)
const deliveryDestinations = ref([])
const wallet = ref(null)

function speakPickupCode(pkg) {
  if (typeof window !== 'undefined' && window.speechSynthesis) {
    const u = new SpeechSynthesisUtterance(
      `您的包裹${pkg.trackingNumber}已到达，取件码是${pkg.pickupCode}`
    )
    u.lang = 'zh-CN'
    window.speechSynthesis.speak(u)
  }
}

function trapFocus(e) {
  if (e.key !== 'Tab' || !mergeDialogRef.value) return
  const focusable = mergeDialogRef.value.querySelectorAll(
    'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
  )
  if (focusable.length === 0) return
  const first = focusable[0]
  const last = focusable[focusable.length - 1]
  if (e.shiftKey && document.activeElement === first) {
    e.preventDefault()
    last.focus()
  } else if (!e.shiftKey && document.activeElement === last) {
    e.preventDefault()
    first.focus()
  }
}

const packages = computed(() => homeData.value?.packages ?? [])
const stats = computed(() => homeData.value?.stats ?? {
  incomingCount: 0,
  deliveringCount: 0,
  deliveredCount: 0
})
const recommendations = computed(() => homeData.value?.recommendations ?? [])

const selectedIncoming = computed(() =>
  packages.value.filter((p) => selectedIds.value.has(p.id) && p.status === 'IN_STORAGE')
)
const incomingPackages = computed(() =>
  packages.value.filter((p) => p.status === 'IN_STORAGE')
)

const hasDeliveringPackages = computed(() =>
  packages.value.some((p) => p.status === 'OUT_FOR_DELIVERY')
)
const deliveringStation = computed(() => {
  const p = packages.value.find((pkg) => pkg.status === 'OUT_FOR_DELIVERY')
  return p?.zone || 'A区'
})

const statusLabel = (status) => {
  const map = {
    IN_STORAGE: '待取件',
    OUT_FOR_DELIVERY: '配送中',
    DELIVERED: '已送达',
    PICKED_UP: '已取件',
    RETURNED: '已退回',
    COMPLETED: '已完成'
  }
  return map[status] || status
}

const toggleSelect = (id) => {
  const next = new Set(selectedIds.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  selectedIds.value = next
}

const load = async () => {
  loading.value = true
  try {
    const [homeRes, achieveRes] = await Promise.all([
      getHomeData(),
      getAchievements()
    ])
    homeData.value = homeRes
    achievements.value = achieveRes
    wallet.value = await getMyWallet()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const destinationLabel = (id) => {
  const map = {
    DORM_1: '宿舍1号楼',
    DORM_2: '宿舍2号楼',
    DORM_3: '宿舍3号楼',
    CAFETERIA: '食堂',
    STATION_1: '驿站'
  }
  return map[id] || id
}

const openScheduleModal = async (pkg) => {
  schedulePkg.value = pkg
  scheduleDestination.value = ''
  showScheduleModal.value = true
  if (deliveryDestinations.value.length === 0) {
    try {
      const dests = await getDestinations()
      deliveryDestinations.value = [...dests].filter((d) => d !== 'STATION_1')
    } catch (e) {
      deliveryDestinations.value = ['DORM_1', 'DORM_2', 'DORM_3', 'CAFETERIA']
    }
  }
}

const openRewardModal = async (pkg) => {
  rewardPkg.value = pkg
  rewardDestination.value = ''
  rewardAmount.value = ''
  showRewardModal.value = true
  if (deliveryDestinations.value.length === 0) {
    try {
      const dests = await getDestinations()
      deliveryDestinations.value = [...dests].filter((d) => d !== 'STATION_1')
    } catch (e) {
      deliveryDestinations.value = ['DORM_1', 'DORM_2', 'DORM_3', 'CAFETERIA']
    }
  }
}

const doScheduleDelivery = async () => {
  if (!schedulePkg.value || !scheduleDestination.value) return
  scheduleLoading.value = true
  try {
    await scheduleDelivery(schedulePkg.value.id, scheduleDestination.value)
    showScheduleModal.value = false
    schedulePkg.value = null
    scheduleDestination.value = ''
    await load()
    alert('预约配送成功！配送员将尽快为您配送')
  } catch (e) {
    alert(e.message || '预约失败')
  } finally {
    scheduleLoading.value = false
  }
}

const doPublishReward = async () => {
  if (!rewardPkg.value || !rewardDestination.value) return
  const amt = Number(rewardAmount.value)
  if (!Number.isFinite(amt) || amt <= 0) {
    alert('请输入正确的悬赏金额（>0）')
    return
  }
  rewardLoading.value = true
  try {
    await publishRewardTask(rewardPkg.value.id, rewardDestination.value, amt)
    showRewardModal.value = false
    rewardPkg.value = null
    rewardDestination.value = ''
    rewardAmount.value = ''
    await load()
    alert('悬赏发布成功，已冻结对应金额')
  } catch (e) {
    alert(e?.message || '发布失败')
  } finally {
    rewardLoading.value = false
  }
}

const pickup = async (id) => {
  pickupLoading.value = id
  try {
    await singlePickup(id)
    await load()
  } finally {
    pickupLoading.value = null
  }
}

const doMergePickup = async () => {
  const ids = selectedIncoming.value.map((p) => p.id)
  if (ids.length < 2) return
  mergeLoading.value = true
  try {
    const result = await executeMergedPickup(ids)
    if (result.success) {
      showMergeConfirm.value = false
      selectedIds.value = new Set()
      await load()
      alert('合并取件成功！取件码：' + result.pickupCodes.join('、'))
    } else {
      alert(result.error || '合并取件失败')
    }
  } catch (e) {
    alert(e.message || '合并取件失败')
  } finally {
    mergeLoading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.map-skeleton {
  background: linear-gradient(
    90deg,
    #f4f5f6 25%,
    #e8e9ed 50%,
    #f4f5f6 75%
  );
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.5s infinite;
}
@keyframes skeleton-shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
