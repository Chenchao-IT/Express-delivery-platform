<template>
  <div class="space-y-6">
    <section class="flex flex-wrap items-start justify-between gap-4">
      <div>
        <p class="text-xs font-semibold uppercase tracking-[0.2em] text-brand">Notifications</p>
        <h1 class="mt-2 text-3xl font-bold text-slate-900">消息通知</h1>
        <p class="mt-3 max-w-2xl text-sm leading-7 text-slate-500">
          查看包裹入库、预约配送、悬赏接单、配送完成等系统消息，确保业务流转可追踪。
        </p>
      </div>

      <div class="flex flex-wrap items-center gap-3">
        <span class="rounded-full px-4 py-2 text-sm font-medium" :class="unreadCount ? 'bg-amber-100 text-amber-700' : 'bg-slate-100 text-slate-500'">
          未读 {{ unreadCount }} 条
        </span>
        <button type="button" class="secondary-cta h-11 px-4" :disabled="loading" @click="load">
          {{ loading ? '刷新中...' : '刷新通知' }}
        </button>
        <button type="button" class="primary-cta h-11 px-4" :disabled="!unreadCount || actionLoading" @click="markAllRead">
          全部已读
        </button>
      </div>
    </section>

    <section
      v-if="feedback"
      class="rounded-[22px] border border-brand/20 bg-brand/10 px-5 py-4 text-sm text-brand shadow-sm"
    >
      {{ feedback }}
    </section>

    <section class="panel-card">
      <div v-if="items.length === 0 && !loading" class="empty-state">
        <div class="text-5xl">🔔</div>
        <h2 class="mt-5 text-2xl font-semibold text-slate-900">暂无通知</h2>
        <p class="mt-3 text-sm leading-7 text-slate-500">
          当包裹入库、任务接单或配送完成时，系统会在这里同步提醒。
        </p>
      </div>

      <div v-else class="space-y-4">
        <article
          v-for="item in items"
          :key="item.id"
          class="flex flex-col gap-4 rounded-[24px] border p-5 shadow-sm md:flex-row md:items-start md:justify-between"
          :class="item.read ? 'border-slate-200 bg-white' : 'border-amber-200 bg-amber-50/60'"
        >
          <div class="min-w-0 flex-1">
            <div class="flex flex-wrap items-center gap-2">
              <span class="rounded-full px-3 py-1 text-xs font-medium" :class="item.read ? 'bg-slate-100 text-slate-500' : 'bg-amber-100 text-amber-700'">
                {{ item.read ? '已读' : '未读' }}
              </span>
              <span class="rounded-full bg-brand/10 px-3 py-1 text-xs font-medium text-brand">
                {{ typeLabel(item.type) }}
              </span>
            </div>
            <h2 class="mt-3 text-lg font-semibold text-slate-900">{{ item.title }}</h2>
            <p class="mt-2 text-sm leading-7 text-slate-600">{{ item.content }}</p>
          </div>
          <div class="text-sm text-slate-400">
            {{ formatTime(item.createdAt) }}
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { listNotifications, openNotificationStream, readAllNotifications } from '@/api/notifications'

const loading = ref(false)
const actionLoading = ref(false)
const items = ref([])
const unreadCount = ref(0)
const feedback = ref('')
let source = null

function typeLabel(type) {
  return {
    package_inbound: '包裹入库',
    delivery_scheduled: '预约配送',
    delivery_completed: '配送完成',
    reward_published: '悬赏发布',
    reward_accepted: '悬赏接单',
    reward_cancelled: '任务取消',
    reward_completed: '任务完成',
  }[type] || '系统通知'
}

function formatTime(value) {
  return value ? new Date(value).toLocaleString('zh-CN') : '-'
}

function upsertMessage(message) {
  if (!message?.id) return
  const index = items.value.findIndex((item) => item.id === message.id)
  if (index >= 0) {
    items.value[index] = { ...items.value[index], ...message }
  } else {
    items.value.unshift(message)
  }
  unreadCount.value = Number(message.unreadCount ?? unreadCount.value)
}

async function load() {
  loading.value = true
  try {
    const res = await listNotifications()
    items.value = Array.isArray(res.items) ? res.items : []
    unreadCount.value = Number(res.unreadCount || 0)
  } catch (error) {
    feedback.value = error?.message || '通知加载失败'
  } finally {
    loading.value = false
  }
}

async function markAllRead() {
  actionLoading.value = true
  try {
    await readAllNotifications()
    items.value = items.value.map((item) => ({ ...item, read: true }))
    unreadCount.value = 0
    feedback.value = '已将全部通知标记为已读'
  } catch (error) {
    feedback.value = error?.message || '操作失败'
  } finally {
    actionLoading.value = false
  }
}

function initStream() {
  try {
    source = openNotificationStream()
    ;[
      'package_inbound',
      'delivery_scheduled',
      'delivery_completed',
      'reward_published',
      'reward_accepted',
      'reward_cancelled',
      'reward_completed',
    ].forEach((eventName) => {
      source.addEventListener(eventName, (event) => upsertMessage(JSON.parse(event.data)))
    })
    source.addEventListener('message_read_all', () => {
      items.value = items.value.map((item) => ({ ...item, read: true }))
      unreadCount.value = 0
    })
  } catch {
    source = null
  }
}

onMounted(async () => {
  await load()
  initStream()
})

onBeforeUnmount(() => {
  source?.close()
})
</script>
