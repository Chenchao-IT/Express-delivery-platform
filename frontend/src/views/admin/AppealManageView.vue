<template>
  <div class="admin-appeals">
    <h1 class="page-title">申诉审核</h1>
    <p class="page-desc">待处理的身份申诉，请在 30 分钟内完成审核。</p>

    <div v-if="loading" class="text-text-secondary text-sm">加载中...</div>
    <div v-else-if="!appeals.length" class="empty">暂无待审核申诉</div>
    <div v-else class="appeal-table-wrap">
      <table class="appeal-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>用户ID</th>
            <th>影子ID</th>
            <th>说明</th>
            <th>提交时间</th>
            <th>SLA 截止</th>
            <th>优先级</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="a in appeals" :key="a.id">
            <td>{{ a.id }}</td>
            <td>{{ a.userId }}</td>
            <td>{{ a.shadowId }}</td>
            <td class="desc-cell">{{ a.description || '-' }}</td>
            <td>{{ formatTime(a.submittedAt) }}</td>
            <td>{{ formatTime(a.slaDeadline) }}</td>
            <td>
              <span class="priority" :class="a.priority?.toLowerCase()">{{ a.priority || 'NORMAL' }}</span>
            </td>
            <td>
              <button
                type="button"
                class="btn-approve"
                @click="review(a.id, true)"
              >
                通过
              </button>
              <button
                type="button"
                class="btn-reject"
                @click="openReject(a.id)"
              >
                拒绝
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="rejectingId" class="reject-modal">
      <div class="modal-content">
        <h3>拒绝申诉</h3>
        <textarea
          v-model="rejectComments"
          placeholder="请输入拒绝原因（可选）"
          rows="3"
          class="input-base w-full"
        />
        <div class="modal-actions">
          <button type="button" class="btn-secondary" @click="rejectingId = null">取消</button>
          <button type="button" class="btn-reject" @click="review(rejectingId, false); rejectingId = null">
            确认拒绝
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getPendingAppeals, reviewAppeal } from '@/api/appeal'

const appeals = ref([])
const loading = ref(false)
const rejectingId = ref(null)
const rejectComments = ref('')

function formatTime(s) {
  if (!s) return ''
  return new Date(s).toLocaleString('zh-CN')
}

async function load() {
  loading.value = true
  try {
    appeals.value = await getPendingAppeals()
  } catch {
    appeals.value = []
  } finally {
    loading.value = false
  }
}

function openReject(id) {
  rejectingId.value = id
  rejectComments.value = ''
}

async function review(id, approved) {
  try {
    await reviewAppeal(id, {
      approved,
      comments: approved ? undefined : rejectComments.value,
    })
    rejectComments.value = ''
    load()
  } catch (e) {
    alert(e?.message || '操作失败')
  }
}

onMounted(load)
</script>

<style scoped>
.admin-appeals {
  max-width: 100%;
}
.page-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #171e26;
  margin-bottom: 0.5rem;
}
.page-desc {
  font-size: 0.875rem;
  color: #586574;
  margin-bottom: 1.5rem;
}
.empty {
  color: #818e9c;
  font-size: 0.875rem;
}
.appeal-table-wrap {
  overflow-x: auto;
}
.appeal-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.875rem;
}
.appeal-table th,
.appeal-table td {
  padding: 0.5rem 0.75rem;
  text-align: left;
  border-bottom: 1px solid #edeeef;
}
.appeal-table th {
  font-weight: 600;
  color: #171e26;
  background: #f4f5f6;
}
.desc-cell {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.priority {
  padding: 0.125rem 0.5rem;
  border-radius: 4px;
  font-size: 0.75rem;
}
.priority.urgent {
  background: #feeae6;
  color: #f64c4c;
}
.btn-approve {
  margin-right: 0.5rem;
  padding: 0.25rem 0.5rem;
  font-size: 0.8125rem;
  background: #47b881;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}
.btn-reject {
  padding: 0.25rem 0.5rem;
  font-size: 0.8125rem;
  background: #f64c4c;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}
.reject-modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 50;
}
.modal-content {
  background: #fff;
  padding: 1.5rem;
  border-radius: 12px;
  min-width: 320px;
}
.modal-content h3 {
  margin-bottom: 1rem;
  font-size: 1rem;
}
.modal-actions {
  margin-top: 1rem;
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
}
</style>
