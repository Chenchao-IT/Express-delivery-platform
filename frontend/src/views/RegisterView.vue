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
          <h3>学生注册</h3>
          <p>REGISTRATION</p>
        </div>

        <form @submit.prevent="handleRegister" class="auth-form">
          <div class="form-row">
            <div class="fancy-input" :class="{ 'has-error': errors.username }">
              <input
                id="reg-username"
                v-model="form.username"
                type="text"
                placeholder=" "
                autocomplete="username"
                maxlength="50"
                @blur="validateField('username')"
              />
              <label for="reg-username">用户名 <span class="required">*</span></label>
              <span class="focus-border"></span>
              <p v-if="errors.username" class="form-error">{{ errors.username }}</p>
            </div>
            <div class="fancy-input" :class="{ 'has-error': errors.password }">
              <input
                id="reg-password"
                v-model="form.password"
                type="password"
                placeholder=" "
                autocomplete="new-password"
                @blur="validateField('password')"
              />
              <label for="reg-password">密码 <span class="required">*</span></label>
              <span class="focus-border"></span>
              <p v-if="errors.password" class="form-error">{{ errors.password }}</p>
            </div>
          </div>

          <div class="fancy-input">
            <input
              id="reg-realName"
              v-model="form.realName"
              type="text"
              placeholder=" "
              autocomplete="name"
            />
            <label for="reg-realName">姓名</label>
            <span class="focus-border"></span>
          </div>

          <div class="form-row">
            <div class="fancy-input">
              <input
                id="reg-phone"
                v-model="form.phone"
                type="tel"
                placeholder=" "
                autocomplete="tel"
              />
              <label for="reg-phone">手机号</label>
              <span class="focus-border"></span>
            </div>
            <div class="fancy-input" :class="{ 'has-error': errors.email }">
              <input
                id="reg-email"
                v-model="form.email"
                type="email"
                placeholder=" "
                autocomplete="email"
                @blur="validateField('email')"
              />
              <label for="reg-email">邮箱</label>
              <span class="focus-border"></span>
              <p v-if="errors.email" class="form-error">{{ errors.email }}</p>
            </div>
          </div>

          <div class="fancy-input">
            <input
              id="reg-college"
              v-model="form.college"
              type="text"
              placeholder=" "
            />
            <label for="reg-college">学院</label>
            <span class="focus-border"></span>
          </div>

          <div class="fancy-input">
            <input
              id="reg-address"
              v-model="form.address"
              type="text"
              placeholder=" "
              autocomplete="street-address"
            />
            <label for="reg-address">收货地址</label>
            <span class="focus-border"></span>
          </div>

          <p v-if="error" class="form-error form-error-global">{{ error }}</p>

          <button type="submit" class="launch-btn" :class="{ launching: loading }" :disabled="loading">
            <span class="text">{{ loading ? '注册中...' : '创建账号' }}</span>
            <span class="rocket">🚀</span>
          </button>
        </form>

        <p class="auth-footer">
          已有账号？ <router-link to="/login" class="auth-link">立即登录</router-link>
        </p>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const form = reactive({
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: '',
  college: '',
  address: '',
})
const errors = reactive({
  username: '',
  password: '',
  email: '',
})
const loading = ref(false)
const error = ref('')

const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function validateField(field) {
  if (field === 'username') {
    const v = form.username.trim()
    if (!v) errors.username = '请输入用户名'
    else if (v.length < 3) errors.username = '用户名至少3个字符'
    else if (v.length > 50) errors.username = '用户名最多50个字符'
    else errors.username = ''
  }
  if (field === 'password') {
    if (!form.password) errors.password = '请输入密码'
    else if (form.password.length < 6) errors.password = '密码至少6位'
    else errors.password = ''
  }
  if (field === 'email') {
    if (!form.email.trim()) errors.email = ''
    else if (!emailRegex.test(form.email)) errors.email = '请输入有效的邮箱地址'
    else errors.email = ''
  }
}

function validateForm() {
  validateField('username')
  validateField('password')
  validateField('email')
  return !errors.username && !errors.password && !errors.email
}

const handleRegister = async () => {
  error.value = ''
  if (!validateForm()) return
  loading.value = true
  try {
    await authStore.register(form)
    router.push('/')
  } catch (e) {
    error.value = e?.message || '注册失败'
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
  max-height: 90vh;
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

/* 右侧注册区 */
.auth-form-area {
  flex: 1;
  padding: 32px 56px 40px;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow-y: auto;
}

.form-header {
  margin-bottom: 24px;
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

.auth-form {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.fancy-input {
  position: relative;
  margin-bottom: 20px;
}

.fancy-input input {
  width: 100%;
  box-sizing: border-box;
  background: transparent;
  border: none;
  border-bottom: 2px solid #e2e8f0;
  padding: 10px 0;
  color: #1e293b;
  font-size: 1rem;
  transition: 0.3s;
  outline: none;
}

.fancy-input input::placeholder {
  color: transparent;
}

.fancy-input label {
  position: absolute;
  left: 0;
  top: 10px;
  color: #94a3b8;
  pointer-events: none;
  transition: 0.3s;
  font-size: 0.95rem;
}

.fancy-input input:focus ~ label,
.fancy-input input:not(:placeholder-shown) ~ label,
.fancy-input input:valid ~ label {
  top: -16px;
  font-size: 0.8rem;
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

.fancy-input .required {
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
  margin-top: 4px;
}

.form-error {
  font-size: 0.8125rem;
  color: #dc2626;
  margin: 0;
}

.form-error-global {
  margin-bottom: 10px;
}

.launch-btn {
  width: 100%;
  height: 48px;
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
  margin-top: 12px;
  flex-shrink: 0;
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

.auth-footer {
  margin-top: 20px;
  text-align: center;
  font-size: 0.875rem;
  color: #64748b;
  flex-shrink: 0;
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
    max-height: none;
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
    padding: 28px 24px 32px;
  }

  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>
