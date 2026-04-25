<template>
  <div class="space-y-6">
    <section class="flex flex-wrap items-start justify-between gap-4">
      <div>
        <p class="text-xs font-semibold uppercase tracking-[0.2em] text-brand">Package Center</p>
        <h1 class="mt-2 text-3xl font-bold text-slate-900">包裹中心</h1>
        <p class="mt-3 max-w-3xl text-sm leading-7 text-slate-500">
          聚焦论文核心流程：物流状态追踪、虚拟货架精准定位、提货码展示、预约配送与悬赏代取。
        </p>
      </div>

      <div class="flex flex-wrap gap-3">
        <router-link to="/notifications" class="secondary-cta h-11 px-4">查看通知</router-link>
        <button type="button" class="primary-cta h-11 px-4" :disabled="loading" @click="load">
          {{ loading ? '刷新中...' : '刷新数据' }}
        </button>
      </div>
    </section>

    <section
      v-if="feedback.message"
      class="rounded-[22px] border px-5 py-4 text-sm shadow-sm"
      :class="feedback.type === 'success'
        ? 'border-emerald-200 bg-emerald-50 text-emerald-700'
        : 'border-amber-200 bg-amber-50 text-amber-700'"
    >
      {{ feedback.message }}
    </section>

    <section class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
      <article class="rounded-[28px] border border-slate-200 bg-white p-6 shadow-sm">
        <p class="text-sm text-slate-500">待取件</p>
        <p class="mt-4 text-4xl font-bold text-slate-900">{{ stats.inStorage }}</p>
        <p class="mt-2 text-sm text-slate-500">可查看货架位置与提货码</p>
      </article>
      <article class="rounded-[28px] border border-slate-200 bg-white p-6 shadow-sm">
        <p class="text-sm text-slate-500">配送中</p>
        <p class="mt-4 text-4xl font-bold text-slate-900">{{ stats.delivering }}</p>
        <p class="mt-2 text-sm text-slate-500">可前往物流追踪查看节点</p>
      </article>
      <article class="rounded-[28px] border border-slate-200 bg-white p-6 shadow-sm">
        <p class="text-sm text-slate-500">已送达/已签收</p>
        <p class="mt-4 text-4xl font-bold text-slate-900">{{ stats.finished }}</p>
        <p class="mt-2 text-sm text-slate-500">覆盖完整业务闭环</p>
      </article>
      <article class="rounded-[28px] border border-slate-200 bg-white p-6 shadow-sm">
        <p class="text-sm text-slate-500">冻结悬赏金额</p>
        <p class="mt-4 text-4xl font-bold text-slate-900">¥{{ money(wallet?.frozen) }}</p>
        <p class="mt-2 text-sm text-slate-500">任务完成后自动结算或退回</p>
      </article>
    </section>

    <section class="panel-card">
      <div class="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h2 class="panel-title">我的包裹</h2>
          <p class="panel-subtitle">支持按状态筛选，并搜索运单号、货架码与提货码。</p>
        </div>
      </div>

      <div class="mt-5 grid gap-3 lg:grid-cols-[minmax(0,1fr)_auto]">
        <input
          v-model.trim="searchKeyword"
          type="text"
          class="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm outline-none transition focus:border-brand"
          placeholder="搜索运单号 / 货架码 / 提货码"
        >
        <div class="flex flex-wrap gap-3">
          <button v-if="searchKeyword" type="button" class="secondary-cta h-11 px-4" @click="searchKeyword = ''">
            清空搜索
          </button>
          <router-link to="/track" class="secondary-cta h-11 px-4">物流追踪</router-link>
        </div>
      </div>

      <div class="mt-5 flex flex-wrap gap-2">
        <button
          v-for="filter in filters"
          :key="filter.key"
          type="button"
          class="rounded-full border px-4 py-2 text-sm font-medium transition"
          :class="activeFilter === filter.key
            ? 'border-brand bg-brand text-white'
            : 'border-slate-200 bg-slate-50 text-slate-600 hover:border-brand/30 hover:bg-brand/5 hover:text-brand'"
          @click="activeFilter = filter.key"
        >
          {{ filter.label }} {{ filter.count }}
        </button>
      </div>

      <div v-if="filteredPackages.length === 0" class="empty-state mt-8">
        <div class="text-5xl">📦</div>
        <h3 class="mt-5 text-2xl font-semibold text-slate-900">暂无匹配包裹</h3>
        <p class="mt-3 text-sm leading-7 text-slate-500">请尝试调整筛选条件，或等待新的包裹入库通知。</p>
      </div>

      <div v-else class="mt-6 space-y-4">
        <article
          v-for="pkg in filteredPackages"
          :key="pkg.id"
          class="flex flex-col gap-4 rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm xl:flex-row xl:items-center xl:justify-between"
        >
          <div class="min-w-0 flex-1">
            <div class="flex flex-wrap items-center gap-2">
              <h3 class="text-lg font-semibold text-slate-900">{{ pkg.trackingNumber }}</h3>
              <span class="rounded-full px-3 py-1 text-xs font-medium" :class="statusClass(pkg.status)">
                {{ statusLabel(pkg.status) }}
              </span>
            </div>

            <div class="mt-4 flex flex-wrap gap-2 text-sm">
              <span class="rounded-full bg-slate-100 px-3 py-1 text-slate-600">货架码 {{ pkg.shelfCode || '-' }}</span>
              <span class="rounded-full bg-slate-100 px-3 py-1 text-slate-600">虚拟货架 {{ shelfLabel(pkg.shelfCode) }}</span>
              <span v-if="pkg.pickupCode" class="rounded-full bg-brand/10 px-3 py-1 font-mono text-brand">提货码 {{ pkg.pickupCode }}</span>
            </div>

            <p class="mt-4 text-sm leading-7 text-slate-500">{{ packageHint(pkg) }}</p>
          </div>

          <div class="flex flex-wrap items-center gap-3">
            <router-link :to="`/track?q=${pkg.trackingNumber}`" class="secondary-cta h-11 px-4">
              查看轨迹
            </router-link>
            <button
              v-if="pkg.status === 'IN_STORAGE'"
              type="button"
              class="secondary-cta h-11 px-4"
              @click="openScheduleModal(pkg)"
            >
              预约配送
            </button>
            <button
              v-if="pkg.status === 'IN_STORAGE'"
              type="button"
              class="secondary-cta h-11 px-4"
              @click="openRewardModal(pkg)"
            >
              发布悬赏
            </button>
            <button
              v-if="pkg.status === 'IN_STORAGE' || pkg.status === 'DELIVERED'"
              type="button"
              class="primary-cta h-11 px-4"
              :disabled="pickupLoading === pkg.id"
              @click="pickup(pkg.id)"
            >
              {{ pickupLoading === pkg.id ? '处理中...' : (pkg.status === 'DELIVERED' ? '确认签收' : '确认取件') }}
            </button>
          </div>
        </article>
      </div>
    </section>

    <Teleport to="body">
      <div v-if="showScheduleModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/45 p-4">
        <div class="w-full max-w-md rounded-[28px] bg-white p-6 shadow-2xl">
          <h3 class="text-xl font-semibold text-slate-900">预约配送</h3>
          <p class="mt-3 text-sm leading-7 text-slate-500">
            包裹 {{ schedulePkg?.trackingNumber }} 将配送到你选择的目的地。
          </p>
          <div class="mt-5">
            <label class="mb-2 block text-sm font-medium text-slate-700">配送目的地</label>
            <select v-model="scheduleDestination" class="w-full rounded-2xl border border-slate-200 px-4 py-3 outline-none focus:border-brand">
              <option value="">请选择目的地</option>
              <option v-for="destination in destinations" :key="destination" :value="destination">
                {{ destinationLabel(destination) }}
              </option>
            </select>
          </div>
          <div class="mt-6 flex justify-end gap-3">
            <button type="button" class="secondary-cta h-11 px-4" @click="closeScheduleModal">取消</button>
            <button
              type="button"
              class="primary-cta h-11 px-4"
              :disabled="!scheduleDestination || scheduleLoading"
              @click="submitSchedule"
            >
              {{ scheduleLoading ? '提交中...' : '确认预约' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <Teleport to="body">
      <div v-if="showRewardModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/45 p-4">
        <div class="w-full max-w-md rounded-[28px] bg-white p-6 shadow-2xl">
          <h3 class="text-xl font-semibold text-slate-900">发布悬赏代取</h3>
          <p class="mt-3 text-sm leading-7 text-slate-500">
            包裹 {{ rewardPkg?.trackingNumber }} 将进入任务大厅，由代取员或学生接单完成配送。
          </p>

          <div class="mt-5 space-y-4">
            <div>
              <label class="mb-2 block text-sm font-medium text-slate-700">配送目的地</label>
              <select v-model="rewardDestination" class="w-full rounded-2xl border border-slate-200 px-4 py-3 outline-none focus:border-brand">
                <option value="">请选择目的地</option>
                <option v-for="destination in destinations" :key="destination" :value="destination">
                  {{ destinationLabel(destination) }}
                </option>
              </select>
            </div>

            <div>
              <label class="mb-2 block text-sm font-medium text-slate-700">悬赏金额（元）</label>
              <input
                v-model="rewardAmount"
                class="w-full rounded-2xl border border-slate-200 px-4 py-3 outline-none focus:border-brand"
                inputmode="decimal"
                placeholder="例如 3.00"
              >
            </div>
          </div>

          <div class="mt-6 flex justify-end gap-3">
            <button type="button" class="secondary-cta h-11 px-4" @click="closeRewardModal">取消</button>
            <button
              type="button"
              class="primary-cta h-11 px-4"
              :disabled="!rewardDestination || !rewardAmount || rewardLoading"
              @click="submitReward"
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
import { computed, onMounted, ref } from 'vue'
import { getMyPackages, pickupPackage } from '@/api/package'
import { getMyWallet } from '@/api/wallet'
import { getDestinations, publishRewardTask, scheduleDelivery } from '@/api/delivery'

const loading = ref(false)
const pickupLoading = ref(null)
const packages = ref([])
const wallet = ref(null)
const searchKeyword = ref('')
const activeFilter = ref('ALL')
const feedback = ref({ type: 'success', message: '' })

const showScheduleModal = ref(false)
const schedulePkg = ref(null)
const scheduleDestination = ref('')
const scheduleLoading = ref(false)

const showRewardModal = ref(false)
const rewardPkg = ref(null)
const rewardDestination = ref('')
const rewardAmount = ref('')
const rewardLoading = ref(false)

const destinations = ref([])

const filters = computed(() => [
  { key: 'ALL', label: '全部', count: packages.value.length },
  { key: 'IN_STORAGE', label: '待取件', count: packages.value.filter((item) => item.status === 'IN_STORAGE').length },
  { key: 'OUT_FOR_DELIVERY', label: '配送中', count: packages.value.filter((item) => item.status === 'OUT_FOR_DELIVERY').length },
  { key: 'DONE', label: '已完成', count: packages.value.filter((item) => ['DELIVERED', 'PICKED_UP', 'COMPLETED'].includes(item.status)).length },
])

const stats = computed(() => ({
  inStorage: packages.value.filter((item) => item.status === 'IN_STORAGE').length,
  delivering: packages.value.filter((item) => item.status === 'OUT_FOR_DELIVERY').length,
  finished: packages.value.filter((item) => ['DELIVERED', 'PICKED_UP', 'COMPLETED'].includes(item.status)).length,
}))

const filteredPackages = computed(() => {
  const keyword = searchKeyword.value.trim().toUpperCase()
  return [...packages.value]
    .sort((a, b) => new Date(b.storageTime || b.createdAt || 0) - new Date(a.storageTime || a.createdAt || 0))
    .filter((pkg) => {
      const matchesFilter = activeFilter.value === 'ALL'
        || (activeFilter.value === 'DONE' && ['DELIVERED', 'PICKED_UP', 'COMPLETED'].includes(pkg.status))
        || pkg.status === activeFilter.value

      const matchesKeyword = !keyword || [pkg.trackingNumber, pkg.shelfCode, pkg.pickupCode]
        .filter(Boolean)
        .some((item) => String(item).toUpperCase().includes(keyword))

      return matchesFilter && matchesKeyword
    })
})

function money(value) {
  return Number(value ?? 0).toFixed(2)
}

function statusLabel(status) {
  return {
    IN_STORAGE: '待取件',
    OUT_FOR_DELIVERY: '配送中',
    DELIVERED: '已送达',
    PICKED_UP: '已签收',
    COMPLETED: '已完成',
    RETURNED: '已退回',
  }[status] || status
}

function statusClass(status) {
  if (status === 'IN_STORAGE') return 'bg-amber-100 text-amber-700'
  if (status === 'OUT_FOR_DELIVERY') return 'bg-brand/10 text-brand'
  if (['DELIVERED', 'PICKED_UP', 'COMPLETED'].includes(status)) return 'bg-emerald-100 text-emerald-700'
  return 'bg-slate-100 text-slate-500'
}

function shelfLabel(shelfCode) {
  if (!shelfCode) return '暂无定位'
  const parts = shelfCode.split('-')
  if (parts.length < 4) return shelfCode
  return `${parts[0]} 区-${parts[1]} 排-${parts[2]} 层-${parts[3]} 号`
}

function packageHint(pkg) {
  if (pkg.status === 'IN_STORAGE') {
    return `当前位于 ${shelfLabel(pkg.shelfCode)}，可凭提货码自取，也可预约配送或发布悬赏代取。`
  }
  if (pkg.status === 'OUT_FOR_DELIVERY') {
    return '包裹正在配送中，可进入物流追踪页面查看状态节点。'
  }
  if (pkg.status === 'DELIVERED') {
    return '包裹已送达目的地，请尽快确认签收。'
  }
  return '该包裹已完成流转，可继续查询历史信息。'
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

function setFeedback(type, message) {
  feedback.value = { type, message }
}

async function ensureDestinations() {
  if (destinations.value.length) return
  try {
    const res = await getDestinations()
    destinations.value = Array.from(res || []).filter((item) => item !== 'STATION_1')
  } catch {
    destinations.value = ['DORM_1', 'DORM_2', 'DORM_3', 'CAFETERIA']
  }
}

async function load() {
  loading.value = true
  try {
    const [pkgList, walletInfo] = await Promise.all([getMyPackages(), getMyWallet()])
    packages.value = Array.isArray(pkgList) ? pkgList : []
    wallet.value = walletInfo
  } catch (error) {
    setFeedback('warning', error?.message || '包裹中心加载失败')
  } finally {
    loading.value = false
  }
}

async function pickup(id) {
  pickupLoading.value = id
  try {
    await pickupPackage(id)
    await load()
    setFeedback('success', '包裹状态已更新为已签收')
  } catch (error) {
    setFeedback('warning', error?.message || '取件失败')
  } finally {
    pickupLoading.value = null
  }
}

async function openScheduleModal(pkg) {
  schedulePkg.value = pkg
  scheduleDestination.value = ''
  showScheduleModal.value = true
  await ensureDestinations()
}

function closeScheduleModal() {
  showScheduleModal.value = false
  schedulePkg.value = null
  scheduleDestination.value = ''
}

async function submitSchedule() {
  if (!schedulePkg.value || !scheduleDestination.value) return
  scheduleLoading.value = true
  try {
    await scheduleDelivery(schedulePkg.value.id, scheduleDestination.value)
    closeScheduleModal()
    await load()
    setFeedback('success', '预约配送成功，系统已生成配送任务')
  } catch (error) {
    setFeedback('warning', error?.message || '预约配送失败')
  } finally {
    scheduleLoading.value = false
  }
}

async function openRewardModal(pkg) {
  rewardPkg.value = pkg
  rewardDestination.value = ''
  rewardAmount.value = ''
  showRewardModal.value = true
  await ensureDestinations()
}

function closeRewardModal() {
  showRewardModal.value = false
  rewardPkg.value = null
  rewardDestination.value = ''
  rewardAmount.value = ''
}

async function submitReward() {
  if (!rewardPkg.value || !rewardDestination.value) return
  const amount = Number(rewardAmount.value)
  if (!Number.isFinite(amount) || amount <= 0) {
    setFeedback('warning', '请输入正确的悬赏金额')
    return
  }

  rewardLoading.value = true
  try {
    await publishRewardTask(rewardPkg.value.id, rewardDestination.value, amount)
    closeRewardModal()
    await load()
    setFeedback('success', '悬赏任务已发布，请前往任务大厅查看流转状态')
  } catch (error) {
    setFeedback('warning', error?.message || '悬赏发布失败')
  } finally {
    rewardLoading.value = false
  }
}

onMounted(load)
</script>
