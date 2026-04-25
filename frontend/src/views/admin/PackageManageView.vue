<template>
  <div class="space-y-6">
    <section class="flex flex-wrap items-start justify-between gap-4">
      <div>
        <p class="text-xs font-semibold uppercase tracking-[0.2em] text-brand">Warehouse Inbound</p>
        <h1 class="mt-2 text-3xl font-bold text-slate-900">模拟扫码入库</h1>
        <p class="mt-3 max-w-3xl text-sm leading-7 text-slate-500">
          对应论文中的“模拟扫码入库”场景，支持录入运单号、收件学生与虚拟货架码，并校验重复单号。
        </p>
      </div>

      <button type="button" class="primary-cta h-11 px-4" :disabled="loading" @click="load">
        {{ loading ? '刷新中...' : '刷新列表' }}
      </button>
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

    <section class="panel-card">
      <div>
        <h2 class="panel-title">入库表单</h2>
        <p class="panel-subtitle">可手动录入结构化数据，替代真实扫码外设完成功能验证。</p>
      </div>

      <div class="mt-5 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <div>
          <label class="mb-2 block text-sm font-medium text-slate-700">运单号</label>
          <input
            v-model.trim="createForm.trackingNumber"
            class="w-full rounded-2xl border border-slate-200 px-4 py-3 outline-none focus:border-brand"
            placeholder="例如 SF1234567890"
          >
        </div>
        <div>
          <label class="mb-2 block text-sm font-medium text-slate-700">学生用户名</label>
          <input
            v-model.trim="createForm.studentUsername"
            class="w-full rounded-2xl border border-slate-200 px-4 py-3 outline-none focus:border-brand"
            placeholder="例如 student1"
          >
        </div>
        <div>
          <label class="mb-2 block text-sm font-medium text-slate-700">包裹尺寸</label>
          <select v-model="createForm.size" class="w-full rounded-2xl border border-slate-200 px-4 py-3 outline-none focus:border-brand">
            <option value="SMALL">小件</option>
            <option value="MEDIUM">中件</option>
            <option value="LARGE">大件</option>
          </select>
        </div>
        <div>
          <label class="mb-2 block text-sm font-medium text-slate-700">虚拟货架码</label>
          <input
            v-model.trim="createForm.shelfCode"
            class="w-full rounded-2xl border border-slate-200 px-4 py-3 outline-none focus:border-brand"
            placeholder="可留空自动分配"
          >
        </div>
      </div>

      <div class="mt-5 flex flex-wrap gap-3">
        <button type="button" class="primary-cta h-11 px-5" :disabled="createLoading" @click="submitCreate">
          {{ createLoading ? '入库中...' : '确认入库' }}
        </button>
        <button type="button" class="secondary-cta h-11 px-5" @click="resetForm">重置表单</button>
      </div>
    </section>

    <section class="panel-card">
      <div class="flex items-start justify-between gap-4">
        <div>
          <h2 class="panel-title">包裹清单</h2>
          <p class="panel-subtitle">用于核验入库、货架分配、提货码生成和状态流转结果。</p>
        </div>
        <span class="rounded-full bg-brand/10 px-3 py-1 text-sm font-medium text-brand">共 {{ packages.length }} 条</span>
      </div>

      <div v-if="packages.length === 0" class="empty-state mt-6">
        <div class="text-5xl">📥</div>
        <h3 class="mt-5 text-2xl font-semibold text-slate-900">暂无入库数据</h3>
        <p class="mt-3 text-sm leading-7 text-slate-500">完成模拟扫码入库后，包裹会显示在这里。</p>
      </div>

      <div v-else class="mt-6 space-y-4">
        <article
          v-for="pkg in packages"
          :key="pkg.id"
          class="flex flex-col gap-4 rounded-[24px] border border-slate-200 bg-white p-5 shadow-sm xl:flex-row xl:items-center xl:justify-between"
        >
          <div class="min-w-0 flex-1">
            <div class="flex flex-wrap items-center gap-2">
              <p class="text-lg font-semibold text-slate-900">{{ pkg.trackingNumber }}</p>
              <span class="rounded-full px-3 py-1 text-xs font-medium" :class="statusClass(pkg.status)">
                {{ statusLabel(pkg.status) }}
              </span>
            </div>
            <div class="mt-4 flex flex-wrap gap-2 text-sm">
              <span class="rounded-full bg-slate-100 px-3 py-1 text-slate-600">货架码 {{ pkg.shelfCode || '-' }}</span>
              <span class="rounded-full bg-slate-100 px-3 py-1 text-slate-600">尺寸 {{ sizeLabel(pkg.size) }}</span>
              <span class="rounded-full bg-brand/10 px-3 py-1 font-mono text-brand">提货码 {{ pkg.pickupCode || '-' }}</span>
            </div>
          </div>
          <div class="text-sm text-slate-400">
            {{ pkg.storageTime ? new Date(pkg.storageTime).toLocaleString('zh-CN') : '-' }}
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { createPackage, listPackages } from '@/api/package'

const loading = ref(false)
const createLoading = ref(false)
const packages = ref([])
const feedback = ref({ type: 'success', message: '' })

const createForm = reactive({
  trackingNumber: '',
  studentUsername: '',
  size: 'MEDIUM',
  shelfCode: '',
})

function sizeLabel(value) {
  return { SMALL: '小件', MEDIUM: '中件', LARGE: '大件' }[value] || value
}

function statusLabel(value) {
  return {
    IN_STORAGE: '待取件',
    OUT_FOR_DELIVERY: '配送中',
    DELIVERED: '已送达',
    PICKED_UP: '已签收',
    COMPLETED: '已完成',
  }[value] || value
}

function statusClass(value) {
  if (value === 'IN_STORAGE') return 'bg-brand/10 text-brand'
  if (value === 'DELIVERED' || value === 'PICKED_UP' || value === 'COMPLETED') return 'bg-emerald-100 text-emerald-700'
  return 'bg-slate-100 text-slate-500'
}

function setFeedback(type, message) {
  feedback.value = { type, message }
}

function resetForm() {
  createForm.trackingNumber = ''
  createForm.studentUsername = ''
  createForm.size = 'MEDIUM'
  createForm.shelfCode = ''
}

async function load() {
  loading.value = true
  try {
    const list = await listPackages()
    packages.value = Array.isArray(list)
      ? [...list].sort((a, b) => new Date(b.storageTime || 0) - new Date(a.storageTime || 0))
      : []
  } catch (error) {
    setFeedback('warning', error?.message || '包裹列表加载失败')
  } finally {
    loading.value = false
  }
}

async function submitCreate() {
  if (!createForm.studentUsername) {
    setFeedback('warning', '请输入学生用户名')
    return
  }

  createLoading.value = true
  try {
    const pkg = await createPackage({ ...createForm })
    resetForm()
    await load()
    setFeedback('success', `入库成功，运单号：${pkg.trackingNumber}，提货码：${pkg.pickupCode}`)
  } catch (error) {
    setFeedback('warning', error?.message || '入库失败')
  } finally {
    createLoading.value = false
  }
}

onMounted(load)
</script>
