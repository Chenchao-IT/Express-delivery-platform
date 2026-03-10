<template>
  <div class="appeal-page">
    <h1 class="page-title">身份申诉</h1>
    <p class="page-desc">检测到身份冲突，请上传学生证等证明材料并提交申诉，我们将在 30 分钟内处理。</p>

    <div v-if="!authStore.user?.conflictResolutionRequired" class="notice info">
      您当前无需申诉。若此前已提交申诉，可查看下方记录。
    </div>

    <form v-else @submit.prevent="handleSubmit" class="appeal-form">
      <div class="form-group">
        <label class="form-label">申诉说明</label>
        <textarea
          v-model="form.description"
          class="input-base w-full min-h-[100px]"
          placeholder="请简要说明身份冲突情况（可选）"
          rows="4"
        />
      </div>
      <div class="form-group">
        <label class="form-label">证明材料 URL</label>
        <input
          v-model="form.proofUrlsText"
          type="text"
          class="input-base w-full"
          placeholder="可填写学生证等图片链接，多个用逗号分隔（可选）"
        />
      </div>
      <p v-if="error" class="form-error">{{ error }}</p>
      <p v-if="success" class="form-success">{{ success }}</p>
      <button type="submit" :disabled="loading" class="btn-primary py-3 rounded-button font-medium">
        {{ loading ? '提交中...' : '提交申诉' }}
      </button>
    </form>

    <section class="my-appeals">
      <h2 class="section-title">我的申诉记录</h2>
      <div v-if="loadingList" class="text-text-secondary text-sm">加载中...</div>
      <div v-else-if="!appeals.length" class="text-text-secondary text-sm">暂无记录</div>
      <ul v-else class="appeal-list">
        <li v-for="a in appeals" :key="a.id" class="appeal-item">
          <span class="appeal-id">#{{ a.id }}</span>
          <span class="appeal-status" :class="a.status.toLowerCase()">{{ statusText(a.status) }}</span>
          <span class="appeal-time">{{ formatTime(a.submittedAt) }}</span>
          <span v-if="a.reviewComments" class="appeal-comments">审核意见：{{ a.reviewComments }}</span>
        </li>
      </ul>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { submitAppeal, getMyAppeals } from '@/api/appeal'

const authStore = useAuthStore()

const form = reactive({
  description: '',
  proofUrlsText: '',
})
const loading = ref(false)
const loadingList = ref(false)
const error = ref('')
const success = ref('')
const appeals = ref([])

function statusText(status) {
  const m = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝' }
  return m[status] || status
}

function formatTime(s) {
  if (!s) return ''
  return new Date(s).toLocaleString('zh-CN')
}

async function handleSubmit() {
  error.value = ''
  success.value = ''
  if (!authStore.user?.shadowId) {
    error.value = '缺少影子账户信息，请重新登录'
    return
  }
  const proofUrls = form.proofUrlsText
    ? form.proofUrlsText.split(/[,，]/).map((s) => s.trim()).filter(Boolean)
    : []
  loading.value = true
  try {
    const res = await submitAppeal({
      shadowId: authStore.user.shadowId,
      appealType: 'IDENTITY_CONFLICT',
      description: form.description || undefined,
      proofUrls: proofUrls.length ? proofUrls : undefined,
    })
    if (res.success) {
      success.value = res.message || '申诉已提交'
      form.description = ''
      form.proofUrlsText = ''
      loadMyAppeals()
    } else {
      error.value = res.message || '提交失败'
    }
  } catch (e) {
    error.value = e?.message || '提交失败'
  } finally {
    loading.value = false
  }
}

async function loadMyAppeals() {
  loadingList.value = true
  try {
    appeals.value = await getMyAppeals()
  } catch {
    appeals.value = []
  } finally {
    loadingList.value = false
  }
}

onMounted(() => {
  loadMyAppeals()
})
</script>

<style scoped>
.appeal-page {
  max-width: 32rem;
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
.notice {
  padding: 0.75rem 1rem;
  border-radius: 8px;
  font-size: 0.875rem;
  margin-bottom: 1.5rem;
}
.notice.info {
  background: #e0efff;
  color: #0756b0;
}
.appeal-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-bottom: 2rem;
}
.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.form-label {
  font-size: 0.875rem;
  font-weight: 500;
  color: #171e26;
}
.form-error {
  font-size: 0.8125rem;
  color: #f64c4c;
}
.form-success {
  font-size: 0.875rem;
  color: #47b881;
}
.my-appeals {
  margin-top: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid #edeeef;
}
.section-title {
  font-size: 1rem;
  font-weight: 600;
  color: #171e26;
  margin-bottom: 0.75rem;
}
.appeal-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.appeal-item {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem 1rem;
  padding: 0.75rem;
  background: #fafafa;
  border-radius: 8px;
  margin-bottom: 0.5rem;
  font-size: 0.875rem;
}
.appeal-id {
  font-weight: 500;
  color: #171e26;
}
.appeal-status {
  padding: 0.125rem 0.5rem;
  border-radius: 4px;
  font-size: 0.75rem;
}
.appeal-status.pending {
  background: #fffbeb;
  color: #d97706;
}
.appeal-status.approved {
  background: #e5fff4;
  color: #47b881;
}
.appeal-status.rejected {
  background: #feeae6;
  color: #f64c4c;
}
.appeal-time {
  color: #818e9c;
}
.appeal-comments {
  width: 100%;
  color: #586574;
  margin-top: 0.25rem;
}
</style>
