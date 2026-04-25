<template>
  <div class="space-y-6">
    <section class="rounded-[32px] border border-slate-200 bg-white p-8 shadow-sm">
      <div class="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p class="text-xs font-semibold uppercase tracking-[0.2em] text-brand">Tracking</p>
          <h1 class="mt-2 text-3xl font-bold text-slate-900">物流追踪</h1>
          <p class="mt-3 max-w-2xl text-sm leading-7 text-slate-500">
            输入运单号即可快速查看包裹状态、入库时间、虚拟货架位置与提货码。
          </p>
        </div>

        <div class="flex flex-wrap gap-3">
          <button type="button" class="secondary-cta h-11 px-4" @click="focusInput">聚焦输入框</button>
          <button type="button" class="primary-cta h-11 px-4" :disabled="loading" @click="search">
            {{ loading ? '查询中...' : '立即查询' }}
          </button>
        </div>
      </div>

      <div class="mt-6 grid gap-4 lg:grid-cols-[minmax(0,1fr)_auto]">
        <div>
          <label for="tracking-number" class="mb-2 block text-sm font-medium text-slate-700">运单号</label>
          <input
            id="tracking-number"
            ref="inputRef"
            v-model.trim="trackingNumber"
            type="text"
            class="w-full rounded-2xl border border-slate-200 px-4 py-3 outline-none transition focus:border-brand"
            placeholder="请输入运单号，例如 SF1234567890"
            @keyup.enter="search"
          >
        </div>
        <div class="flex flex-wrap items-end gap-3">
          <button v-if="trackingNumber" type="button" class="secondary-cta h-11 px-4" @click="clearSearch">清空</button>
          <button type="button" class="primary-cta h-11 min-w-[120px] px-4" :disabled="loading" @click="search">
            {{ loading ? '查询中...' : '立即查询' }}
          </button>
        </div>
      </div>

      <div class="mt-5 flex flex-wrap gap-2">
        <button
          v-for="sample in sampleTrackingNumbers"
          :key="sample"
          type="button"
          class="rounded-full bg-slate-100 px-3 py-1 text-sm text-slate-600 transition hover:bg-brand/10 hover:text-brand"
          @click="applySample(sample)"
        >
          {{ sample }}
        </button>
      </div>
    </section>

    <section
      v-if="notice"
      class="rounded-[22px] border border-brand/20 bg-brand/10 px-5 py-4 text-sm text-brand shadow-sm"
    >
      {{ notice }}
    </section>

    <section v-if="loading" class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
      <article v-for="i in 4" :key="i" class="h-36 animate-pulse rounded-[28px] border border-slate-200 bg-white p-6 shadow-sm" />
    </section>

    <template v-else-if="result">
      <section class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <article v-for="item in summaryCards" :key="item.key" class="rounded-[28px] border border-slate-200 bg-white p-6 shadow-sm">
          <p class="text-sm text-slate-500">{{ item.label }}</p>
          <p class="mt-4 text-3xl font-bold text-slate-900">{{ item.value }}</p>
          <p class="mt-2 text-sm text-slate-500">{{ item.hint }}</p>
        </article>
      </section>

      <section class="grid gap-6 xl:grid-cols-[1.15fr_0.85fr]">
        <article class="panel-card">
          <div class="flex flex-wrap items-start justify-between gap-4">
            <div>
              <p class="text-sm text-slate-500">当前查询结果</p>
              <h2 class="mt-2 text-2xl font-semibold text-slate-900">{{ result.trackingNumber }}</h2>
              <p class="mt-2 text-sm text-slate-500">入库时间：{{ formatTime(result.storageTime) }}</p>
            </div>
            <span class="rounded-full px-3 py-1 text-xs font-medium" :class="statusClass(result.status)">
              {{ statusLabel(result.status) }}
            </span>
          </div>

          <div class="mt-6 grid gap-4 md:grid-cols-2">
            <div class="rounded-[24px] bg-slate-50 p-4">
              <p class="text-xs text-slate-400">货架位置</p>
              <p class="mt-3 text-lg font-semibold text-slate-900">{{ result.shelfCode || '-' }}</p>
            </div>
            <div class="rounded-[24px] bg-slate-50 p-4">
              <p class="text-xs text-slate-400">包裹尺寸</p>
              <p class="mt-3 text-lg font-semibold text-slate-900">{{ sizeLabel(result.size) }}</p>
            </div>
            <div class="rounded-[24px] bg-slate-50 p-4">
              <p class="text-xs text-slate-400">提货码</p>
              <p class="mt-3 text-lg font-semibold text-slate-900">{{ result.pickupCode || '-' }}</p>
            </div>
            <div class="rounded-[24px] bg-slate-50 p-4">
              <p class="text-xs text-slate-400">建议操作</p>
              <p class="mt-3 text-lg font-semibold text-slate-900">{{ actionHint(result.status) }}</p>
            </div>
          </div>
        </article>

        <article class="panel-card">
          <h3 class="panel-title">状态说明</h3>
          <p class="panel-subtitle">帮助你快速理解包裹当前所处环节。</p>

          <div class="mt-5 space-y-4">
            <div v-for="item in timeline" :key="item.key" class="flex items-start gap-3">
              <div class="mt-1 flex flex-col items-center">
                <span class="h-3 w-3 rounded-full" :class="item.done ? 'bg-brand' : 'bg-slate-300'" />
                <span v-if="item.key !== timeline[timeline.length - 1].key" class="mt-1 h-10 w-px bg-slate-200" />
              </div>
              <div>
                <p class="font-medium" :class="item.done ? 'text-slate-900' : 'text-slate-500'">{{ item.title }}</p>
                <p class="mt-1 text-sm leading-6 text-slate-500">{{ item.desc }}</p>
              </div>
            </div>
          </div>
        </article>
      </section>
    </template>

    <section v-else-if="searched" class="panel-card">
      <div class="empty-state">
        <div class="text-5xl">🔎</div>
        <h3 class="mt-5 text-2xl font-semibold text-slate-900">未找到对应包裹</h3>
        <p class="mt-3 text-sm leading-7 text-slate-500">请确认运单号是否正确，或稍后再试。</p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { trackPackage } from '@/api/package'

const route = useRoute()
const router = useRouter()

const trackingNumber = ref('')
const result = ref(null)
const loading = ref(false)
const searched = ref(false)
const notice = ref('')
const inputRef = ref(null)

const sampleTrackingNumbers = ['SF100000000001', 'SF100000000025', 'SF100000000108']
const statusOrder = ['IN_STORAGE', 'OUT_FOR_DELIVERY', 'DELIVERED', 'PICKED_UP', 'COMPLETED']

const timeline = computed(() => {
  const currentIndex = result.value ? statusOrder.indexOf(result.value.status) : -1
  return [
    { key: 'IN_STORAGE', title: '已入库', desc: '包裹已进入驿站并完成上架。', done: currentIndex >= 0 },
    { key: 'OUT_FOR_DELIVERY', title: '配送中', desc: '包裹已进入配送流程。', done: currentIndex >= 1 },
    { key: 'DELIVERED', title: '已送达', desc: '包裹已送达指定位置，等待本人确认。', done: currentIndex >= 2 },
    { key: 'PICKED_UP', title: '已签收', desc: '包裹已被本人或代取人成功领取。', done: currentIndex >= 3 },
  ]
})

const summaryCards = computed(() => [
  {
    key: 'status',
    label: '当前状态',
    value: statusLabel(result.value?.status),
    hint: actionHint(result.value?.status),
  },
  {
    key: 'shelf',
    label: '货架位置',
    value: result.value?.shelfCode || '-',
    hint: '支持快速前往指定货架区域',
  },
  {
    key: 'pickupCode',
    label: '提货码',
    value: result.value?.pickupCode || '-',
    hint: '用于线下核验取件身份',
  },
  {
    key: 'time',
    label: '入库日期',
    value: shortTime(result.value?.storageTime),
    hint: '便于判断处理时效',
  },
])

function focusInput() {
  inputRef.value?.focus()
}

function clearSearch() {
  trackingNumber.value = ''
  result.value = null
  searched.value = false
  notice.value = ''
  router.replace({ query: {} })
  focusInput()
}

function sizeLabel(size) {
  return { SMALL: '小件', MEDIUM: '中件', LARGE: '大件' }[size] || size || '未知'
}

function statusLabel(status) {
  return {
    IN_STORAGE: '待取件',
    OUT_FOR_DELIVERY: '配送中',
    DELIVERED: '已送达',
    PICKED_UP: '已签收',
    RETURNED: '已退回',
    COMPLETED: '已完成',
  }[status] || status || '未知状态'
}

function statusClass(status) {
  if (status === 'IN_STORAGE') return 'bg-amber-100 text-amber-700'
  if (status === 'OUT_FOR_DELIVERY') return 'bg-brand/10 text-brand'
  if (status === 'DELIVERED' || status === 'PICKED_UP' || status === 'COMPLETED') return 'bg-emerald-100 text-emerald-700'
  return 'bg-slate-100 text-slate-500'
}

function actionHint(status) {
  if (status === 'IN_STORAGE') return '可前往包裹中心凭提货码取件'
  if (status === 'OUT_FOR_DELIVERY') return '可继续关注配送进度'
  if (status === 'DELIVERED') return '请及时确认签收'
  if (status === 'PICKED_UP' || status === 'COMPLETED') return '当前流程已结束'
  return '暂无建议'
}

function formatTime(value) {
  return value ? new Date(value).toLocaleString('zh-CN') : '-'
}

function shortTime(value) {
  return value ? new Date(value).toLocaleDateString('zh-CN') : '-'
}

function applySample(sample) {
  trackingNumber.value = sample
  search()
}

async function runSearch(keyword, syncRoute = true) {
  const value = keyword?.trim()
  if (!value) return

  loading.value = true
  searched.value = true
  result.value = null

  try {
    result.value = await trackPackage(value)
    notice.value = `已为你查询运单 ${value}`
    if (syncRoute && route.query.q !== value) {
      router.replace({ query: { q: value } })
    }
  } catch {
    result.value = null
    notice.value = ''
    if (syncRoute && route.query.q !== value) {
      router.replace({ query: { q: value } })
    }
  } finally {
    loading.value = false
  }
}

async function search() {
  await runSearch(trackingNumber.value, true)
}

watch(
  () => route.query.q,
  (value) => {
    if (typeof value !== 'string' || !value.trim()) return
    if (value === trackingNumber.value && (result.value || loading.value)) return
    trackingNumber.value = value
    runSearch(value, false)
  },
  { immediate: true },
)

onMounted(() => {
  focusInput()
})
</script>
