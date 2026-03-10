<template>
  <div class="auth-container">
    <main class="glass-wrapper">
      <section class="auth-brand">
        <div class="brand-content">
          <div class="badge">LIVE 实时校内物流</div>
          <h2 class="hero-title">数字化校园<br /><span>快递调度中心</span></h2>
          <div class="stats-mini">
            <div class="stat-item">
              <span class="num">1,284</span>
              <span class="label">今日派送</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <span class="num">99.8%</span>
              <span class="label">准时率</span>
            </div>
          </div>
        </div>
        <div class="brand-illustration">📦</div>
      </section>

      <section class="auth-form-area">
        <div class="form-header">
          <h3>身份验证</h3>
          <p>AUTHENTICATION</p>
        </div>

        <form @submit.prevent="handleSubmit">
          <div class="fancy-input" :class="{ 'has-error': errors.username }">
            <input
              id="login-username"
              v-model="loginForm.username"
              type="text"
              placeholder=" "
              required
              autocomplete="username"
              @blur="validateField('username')"
            />
            <label for="login-username">学号/工号</label>
            <span class="focus-border"></span>
            <p v-if="errors.username" class="form-error">{{ errors.username }}</p>
          </div>

          <div class="fancy-input" :class="{ 'has-error': errors.password }">
            <input
              id="login-password"
              v-model="loginForm.password"
              type="password"
              placeholder=" "
              required
              autocomplete="current-password"
              @blur="validateField('password')"
            />
            <label for="login-password">访问密码</label>
            <span class="focus-border"></span>
            <p v-if="errors.password" class="form-error">{{ errors.password }}</p>
          </div>

          <!-- 文档 2.2：关键冲突需申诉 / 非关键冲突提示 -->
          <div v-if="authNotice" class="auth-notice" :class="authNoticeType">
            {{ authNotice }}
          </div>
          <p v-if="error" class="form-error form-error-global">{{ error }}</p>
          <p v-if="error && (error.includes('频繁') || error.includes('秒后再试'))" class="rate-limit-tip">
            <button type="button" class="clear-rate-btn" @click="handleClearRateLimit" :disabled="clearingRate">限流了？点击清空</button>
          </p>

          <button type="submit" class="launch-btn" :class="{ launching: loading }" :disabled="loading">
            <span class="text">{{ loading ? '同步中...' : '同步进入' }}</span>
            <span class="rocket">🚀</span>
          </button>
        </form>

        <div class="admin-quick-tag" @click="fillAdmin">
          DEBUG MODE: ADMIN ACCESS
        </div>

        <p class="auth-footer">
          还没有账号？ <router-link to="/register" class="auth-link">立即注册</router-link>
        </p>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { clearRateLimit } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const loginForm = reactive({ username: '', password: '' })
const errors = reactive({ username: '', password: '' })
const loading = ref(false)
const error = ref('')
/** 文档 2.2：登录成功后的提示（关键冲突/非关键冲突） */
const authNotice = ref('')
const authNoticeType = ref('warning') // warning | info
const clearingRate = ref(false)

async function handleClearRateLimit() {
  clearingRate.value = true
  error.value = ''
  try {
    await clearRateLimit()
    error.value = '限流已清空，请重新登录'
  } catch (e) {
    error.value = (e && (e.message || e.msg || e)) || '清空失败'
  } finally {
    clearingRate.value = false
  }
}

function validateField(field) {
  if (field === 'username') {
    errors.username = loginForm.username.trim() ? '' : '请输入学号/工号'
  }
  if (field === 'password') {
    errors.password = loginForm.password ? '' : '请输入密码'
  }
}

function validateForm() {
  validateField('username')
  validateField('password')
  return !errors.username && !errors.password
}

/** 文档 4.2：设备维度限流用，可选 */
function getDeviceId() {
  let id = localStorage.getItem('deviceId')
  if (!id) {
    id = 'web_' + Math.random().toString(36).slice(2) + '_' + Date.now()
    localStorage.setItem('deviceId', id)
  }
  return id
}

function fillAdmin() {
  loginForm.username = 'admin'
  loginForm.password = 'admin123'
  errors.username = ''
  errors.password = ''
  error.value = ''
}

const handleSubmit = async () => {
  error.value = ''
  authNotice.value = ''
  errors.username = ''
  errors.password = ''
  if (!validateForm()) return
  loading.value = true
  try {
    const payload = { username: loginForm.username, password: loginForm.password }
    const deviceId = getDeviceId()
    if (deviceId) payload.deviceId = deviceId
    const res = await authStore.login(payload)
    if (res.conflictResolutionRequired && res.message) {
      authNotice.value = res.message
      authNoticeType.value = 'warning'
    } else if (res.hasConflicts && res.message) {
      authNotice.value = res.message
      authNoticeType.value = 'info'
    }
    const redirect = route.query.redirect || (res.conflictResolutionRequired ? '/appeal' : '/')
    router.push(redirect)
  } catch (e) {
    error.value = (e && (e.message || e.msg || e)) || '登录失败，请检查学号/工号和密码'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* 1. 浅色科技底色 */
.auth-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #f0f4f8 0%, #e2e8f0 50%, #f8fafc 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: 'Inter', 'PingFang SC', sans-serif;
  overflow: hidden;
  position: relative;
  padding: 1rem;
}

/* 2. 动态背景层：物流网格线（浅色下提高可见度） */
.auth-container::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(59, 130, 246, 0.14) 1px, transparent 1px),
    linear-gradient(90deg, rgba(59, 130, 246, 0.14) 1px, transparent 1px);
  background-size: 40px 40px;
  mask-image: radial-gradient(ellipse at center, black 20%, transparent 75%);
  animation: grid-travel 60s linear infinite;
  pointer-events: none;
}

/* 3. 流动光晕（浅色下提高可见度） */
.auth-container::after {
  content: '';
  position: absolute;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle at 50% 50%, rgba(59, 130, 246, 0.18) 0%, rgba(147, 197, 253, 0.06) 40%, transparent 55%);
  animation: orbit 20s infinite linear;
  pointer-events: none;
}

@keyframes grid-travel {
  from { background-position: 0 0; }
  to { background-position: 400px 400px; }
}

@keyframes orbit {
  from { transform: rotate(0deg) translate(-10%, -10%); }
  to { transform: rotate(360deg) translate(-10%, -10%); }
}

.glass-wrapper {
  width: 100%;
  max-width: 900px;
  min-height: 520px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(25px);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 40px;
  display: flex;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.1), 0 0 0 1px rgba(0, 0, 0, 0.04);
  z-index: 10;
  overflow: hidden;
}

/* 左侧品牌区 */
.auth-brand {
  flex: 1.2;
  padding: 48px 56px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.08), rgba(45, 212, 191, 0.06));
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  border-right: 1px solid rgba(0, 0, 0, 0.06);
}

.brand-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.badge {
  display: inline-block;
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.05em;
  color: #2563eb;
  background: rgba(59, 130, 246, 0.12);
  padding: 6px 12px;
  border-radius: 20px;
  margin-bottom: 24px;
  width: fit-content;
}

.hero-title {
  font-size: 2.25rem;
  color: #1e293b;
  line-height: 1.2;
  margin: 0 0 20px 0;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.hero-title span {
  color: #2563eb;
  text-shadow: 0 0 40px rgba(59, 130, 246, 0.2);
}

.stats-mini {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-top: 28px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-item .num {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1e293b;
}

.stat-item .label {
  font-size: 0.8125rem;
  color: #64748b;
}

.stat-divider {
  width: 1px;
  height: 32px;
  background: #e2e8f0;
}

.brand-illustration {
  font-size: 3rem;
  opacity: 0.9;
  margin-top: 24px;
}

/* 右侧登录区 */
.auth-form-area {
  flex: 1;
  padding: 48px 56px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
}

.form-header {
  margin-bottom: 32px;
}

.form-header h3 {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 4px 0;
}

.form-header p {
  font-size: 0.75rem;
  letter-spacing: 0.15em;
  color: #94a3b8;
  margin: 0;
}

.fancy-input {
  position: relative;
  margin-bottom: 28px;
}

.fancy-input input {
  width: 100%;
  box-sizing: border-box;
  background: transparent;
  border: none;
  border-bottom: 2px solid #e2e8f0;
  padding: 12px 0;
  color: #1e293b;
  font-size: 1.05rem;
  transition: 0.3s;
  outline: none;
}

.fancy-input input::placeholder {
  color: transparent;
}

.fancy-input label {
  position: absolute;
  left: 0;
  top: 12px;
  color: #94a3b8;
  pointer-events: none;
  transition: 0.3s;
  font-size: 1rem;
}

.fancy-input input:focus ~ label,
.fancy-input input:not(:placeholder-shown) ~ label,
.fancy-input input:valid ~ label {
  top: -18px;
  font-size: 0.85rem;
  color: #2563eb;
}

.fancy-input input:focus {
  border-bottom-color: #2563eb;
}

.fancy-input.has-error input {
  border-bottom-color: #ef4444;
}

.fancy-input.has-error input ~ label {
  color: #ef4444;
}

.focus-border {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 0;
  height: 2px;
  background: #2563eb;
  transition: width 0.3s ease;
}

.fancy-input input:focus ~ .focus-border {
  width: 100%;
}

.fancy-input .form-error {
  margin-top: 6px;
}

.form-error {
  font-size: 0.8125rem;
  color: #dc2626;
  margin: 0;
}

.form-error-global {
  margin-bottom: 12px;
}

.rate-limit-tip {
  margin-top: 8px;
}

.clear-rate-btn {
  font-size: 0.8125rem;
  color: #2563eb;
  background: none;
  border: none;
  cursor: pointer;
  text-decoration: underline;
}

.clear-rate-btn:hover:not(:disabled) {
  color: #1d4ed8;
}

.clear-rate-btn:disabled {
  color: #94a3b8;
  cursor: not-allowed;
}

.auth-notice {
  font-size: 0.8125rem;
  padding: 0.5rem 0.75rem;
  border-radius: 8px;
  margin-bottom: 12px;
}

.auth-notice.warning {
  color: #92400e;
  background: #fef3c7;
  border: 1px solid #fcd34d;
}

.auth-notice.info {
  color: #1e40af;
  background: #dbeafe;
  border: 1px solid #93c5fd;
}

.launch-btn {
  width: 100%;
  height: 52px;
  background: #1e293b;
  color: #fff;
  border-radius: 14px;
  border: none;
  font-weight: 700;
  font-size: 1rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  transition: 0.3s;
  margin-top: 8px;
}

.launch-btn:hover:not(:disabled) {
  background: #334155;
  transform: translateY(-3px);
  box-shadow: 0 12px 24px rgba(59, 130, 246, 0.25);
}

.launch-btn:disabled {
  opacity: 0.8;
  cursor: not-allowed;
}

.launch-btn.launching .rocket {
  animation: launch 0.8s forwards;
}

@keyframes launch {
  0% { transform: translate(0, 0); opacity: 1; }
  100% { transform: translate(120px, -120px); opacity: 0; }
}

.admin-quick-tag {
  margin-top: 24px;
  font-size: 0.7rem;
  letter-spacing: 0.08em;
  color: #64748b;
  padding: 10px 14px;
  background: #f1f5f9;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
  text-align: center;
}

.admin-quick-tag:hover {
  background: #e2e8f0;
  color: #475569;
}

.auth-footer {
  margin-top: 20px;
  text-align: center;
  font-size: 0.875rem;
  color: #64748b;
}

.auth-link {
  color: #2563eb;
  text-decoration: none;
}

.auth-link:hover {
  text-decoration: underline;
}

@media (max-width: 768px) {
  .glass-wrapper {
    flex-direction: column;
    min-height: auto;
  }

  .auth-brand {
    border-right: none;
    border-bottom: 1px solid rgba(0, 0, 0, 0.06);
    padding: 32px 24px;
  }

  .hero-title {
    font-size: 1.75rem;
  }

  .stats-mini {
    margin-top: 20px;
  }

  .brand-illustration {
    display: none;
  }

  .auth-form-area {
    padding: 32px 24px;
  }
}
</style>
