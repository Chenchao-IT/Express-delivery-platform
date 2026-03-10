<template>
  <div>
    <h1 class="text-2xl font-bold text-text-primary mb-6">配送管理</h1>

    <!-- 快递员视图：可抢单 + 我的任务 -->
    <template v-if="authStore.isCourier && !authStore.isAdmin">
      <!-- 可抢订单（学生预约的待配送单） -->
      <div class="bg-white rounded-card shadow-card p-6 mb-6">
        <h3 class="font-semibold text-text-primary mb-4">可抢订单</h3>
        <p class="text-text-secondary text-sm mb-4">学生预约的配送单，点击抢单后由您负责配送</p>
        <div v-if="pendingList.length === 0" class="text-center py-8 text-text-tertiary">
          暂无待抢订单
        </div>
        <div v-else class="space-y-3">
          <div
            v-for="task in pendingList"
            :key="task.id"
            class="flex items-center justify-between p-4 rounded-input border border-fill-border hover:border-brand/50"
          >
            <div>
              <p class="font-medium text-text-primary">包裹 #{{ task.packageId }} → {{ DESTINATION_LABELS[task.destination] || task.destination }}</p>
              <p class="text-text-secondary text-sm mt-1">
                预估 {{ task.estimatedDistance ? task.estimatedDistance + 'm' : '-' }} · 约 {{ task.estimatedTime ? task.estimatedTime + '分钟' : '-' }}
              </p>
            </div>
            <button
              @click="grabTask(task.id)"
              :disabled="grabLoading === task.id"
              class="btn-primary shrink-0"
            >
              {{ grabLoading === task.id ? '抢单中...' : '抢单' }}
            </button>
          </div>
        </div>
      </div>

      <!-- 我的配送任务 -->
      <div class="bg-white rounded-card shadow-card p-6 mb-6">
        <h3 class="font-semibold text-text-primary mb-4">我的配送</h3>
        <div v-if="myList.length === 0" class="text-center py-8 text-text-tertiary">
          暂无配送任务
        </div>
        <div v-else class="space-y-3">
          <div
            v-for="task in myList"
            :key="task.id"
            class="flex items-center justify-between p-4 rounded-input border border-fill-border"
          >
            <div>
              <p class="font-medium text-text-primary">包裹 #{{ task.packageId }} → {{ DESTINATION_LABELS[task.destination] || task.destination }}</p>
              <p class="text-text-secondary text-sm mt-1">
                <span :class="taskStatusClass(task.status)">{{ taskStatusLabel(task.status) }}</span>
              </p>
            </div>
            <div class="flex gap-2 shrink-0">
              <button
                v-if="task.status === 'ASSIGNED'"
                @click="startTask(task.id)"
                :disabled="startLoading === task.id"
                class="btn-secondary text-sm"
              >
                {{ startLoading === task.id ? '处理中...' : '开始配送' }}
              </button>
              <button
                v-if="(task.status === 'ASSIGNED' || task.status === 'IN_PROGRESS') && task.status !== 'COMPLETED'"
                @click="completeTask(task.id)"
                :disabled="completeLoading === task.id"
                class="btn-primary text-sm"
              >
                {{ completeLoading === task.id ? '处理中...' : '完成配送' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 管理员视图：创建任务 + 全部任务 -->
    <template v-else>
      <div class="bg-white rounded-card shadow-card p-6 mb-6">
        <h3 class="font-semibold text-text-primary mb-4">创建配送任务</h3>
        <div class="flex flex-wrap gap-4 items-end">
          <div>
            <label class="block text-sm text-text-secondary mb-1">包裹ID</label>
            <input v-model.number="createForm.packageId" type="number" class="input-base" placeholder="包裹ID" />
          </div>
          <div>
            <label class="block text-sm text-text-secondary mb-1">目的地</label>
            <select v-model="createForm.destination" class="input-base">
              <option v-for="d in destinations" :key="d" :value="d">{{ DESTINATION_LABELS[d] || d }}</option>
            </select>
          </div>
          <button @click="createDelivery" :disabled="createLoading" class="btn-primary">
            {{ createLoading ? '创建中...' : '创建任务' }}
          </button>
        </div>
      </div>

      <div class="bg-white rounded-card shadow-card overflow-hidden">
        <h3 class="font-semibold text-text-primary p-4 border-b border-fill-border">全部配送任务</h3>
        <div class="overflow-x-auto">
          <table class="min-w-full">
            <thead class="bg-fill-disabled">
              <tr>
                <th class="px-4 py-3 text-left text-sm font-medium text-text-primary">任务ID</th>
                <th class="px-4 py-3 text-left text-sm font-medium text-text-primary">包裹ID</th>
                <th class="px-4 py-3 text-left text-sm font-medium text-text-primary">目的地</th>
                <th class="px-4 py-3 text-left text-sm font-medium text-text-primary">预估距离</th>
                <th class="px-4 py-3 text-left text-sm font-medium text-text-primary">状态</th>
                <th class="px-4 py-3 text-left text-sm font-medium text-text-primary">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="task in deliveries"
                :key="task.id"
                class="border-t border-fill-border hover:bg-fill-disabled/50"
              >
                <td class="px-4 py-3 text-sm">{{ task.id }}</td>
                <td class="px-4 py-3 text-sm">{{ task.packageId }}</td>
                <td class="px-4 py-3 text-sm">{{ DESTINATION_LABELS[task.destination] || task.destination }}</td>
                <td class="px-4 py-3 text-sm">{{ task.estimatedDistance ? task.estimatedDistance + 'm' : '-' }}</td>
                <td class="px-4 py-3">
                  <span :class="taskStatusClass(task.status)">{{ taskStatusLabel(task.status) }}</span>
                </td>
                <td class="px-4 py-3">
                  <button
                    v-if="task.status !== 'COMPLETED'"
                    @click="completeTask(task.id)"
                    class="text-brand hover:underline text-sm"
                  >
                    完成配送
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import {
  listDeliveries,
  listPendingDeliveries,
  listMyDeliveries,
  createDelivery as createApi,
  grabDelivery,
  startDelivery as startDeliveryApi,
  completeDelivery,
  getDestinations,
} from '@/api/delivery'

const authStore = useAuthStore()

// 目的地中文映射（项目方案：驿站、宿舍楼、食堂）
const DESTINATION_LABELS = {
  STATION_1: '驿站1',
  DORM_1: '宿舍1号楼',
  DORM_2: '宿舍2号楼',
  DORM_3: '宿舍3号楼',
  CAFETERIA: '食堂',
}

const deliveries = ref([])
const pendingList = ref([])
const myList = ref([])
const destinations = ref(['DORM_1', 'DORM_2', 'DORM_3'])
const createForm = reactive({ packageId: '', destination: 'DORM_1' })
const createLoading = ref(false)
const grabLoading = ref(null)
const startLoading = ref(null)
const completeLoading = ref(null)

const taskStatusLabel = (s) => ({
  PENDING: '待分配',
  ASSIGNED: '已分配',
  IN_PROGRESS: '配送中',
  COMPLETED: '已完成',
  FAILED: '失败',
}[s] || s)

const taskStatusClass = (s) => {
  const base = 'px-2 py-1 rounded text-xs'
  if (s === 'PENDING') return base + ' bg-warning/20 text-warning-text'
  if (s === 'COMPLETED') return base + ' bg-success/20 text-success'
  return base + ' bg-brand/10 text-brand'
}

const load = async () => {
  if (authStore.isCourier && !authStore.isAdmin) {
    const [pending, my] = await Promise.all([
      listPendingDeliveries(),
      listMyDeliveries(),
    ])
    pendingList.value = pending
    myList.value = my
  } else {
    deliveries.value = await listDeliveries()
  }
}

const grabTask = async (id) => {
  grabLoading.value = id
  try {
    await grabDelivery(id)
    await load()
    alert('抢单成功')
  } catch (e) {
    alert(e?.message || '抢单失败')
  } finally {
    grabLoading.value = null
  }
}

const startTask = async (id) => {
  startLoading.value = id
  try {
    await startDeliveryApi(id)
    await load()
    alert('已开始配送')
  } catch (e) {
    alert(e?.message || '操作失败')
  } finally {
    startLoading.value = null
  }
}

const loadDestinations = async () => {
  try {
    const list = await getDestinations()
    destinations.value = Array.isArray(list) ? list : [...(list || [])]
    if (destinations.value.length === 0) {
      destinations.value = ['STATION_1', 'DORM_1', 'DORM_2', 'DORM_3', 'CAFETERIA']
    }
  } catch {
    destinations.value = ['STATION_1', 'DORM_1', 'DORM_2', 'DORM_3', 'CAFETERIA']
  }
}

const createDelivery = async () => {
  if (!createForm.packageId) return
  createLoading.value = true
  try {
    await createApi({
      packageId: createForm.packageId,
      destination: createForm.destination,
    })
    createForm.packageId = ''
    load()
    alert('配送任务创建成功')
  } catch (e) {
    alert(e?.message || '创建失败')
  } finally {
    createLoading.value = false
  }
}

const completeTask = async (id) => {
  completeLoading.value = id
  try {
    await completeDelivery(id)
    await load()
    alert('配送已完成')
  } catch (e) {
    alert(e?.message || '操作失败')
  } finally {
    completeLoading.value = null
  }
}

onMounted(() => {
  load()
  loadDestinations()
})
</script>
