<template>
  <div>
    <h1 class="text-2xl font-bold text-text-primary mb-6">欢迎使用校园快递配送系统</h1>

    <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
      <router-link
        to="/packages"
        class="block bg-white rounded-card shadow-card p-6 hover:shadow-lg hover:-translate-y-0.5 transition-all"
      >
        <div class="text-brand text-3xl mb-3">📦</div>
        <h3 class="font-semibold text-text-primary">我的包裹</h3>
        <p class="text-text-secondary text-sm mt-1">查看我的快递包裹及取件信息</p>
      </router-link>
      <router-link
        to="/track"
        class="block bg-white rounded-card shadow-card p-6 hover:shadow-lg hover:-translate-y-0.5 transition-all"
      >
        <div class="text-brand text-3xl mb-3">🔍</div>
        <h3 class="font-semibold text-text-primary">快递查询</h3>
        <p class="text-text-secondary text-sm mt-1">输入单号查询快递物流状态</p>
      </router-link>
      <router-link
        v-if="authStore.isAdmin || authStore.isCourier"
        to="/admin/deliveries"
        class="block bg-white rounded-card shadow-card p-6 hover:shadow-lg hover:-translate-y-0.5 transition-all"
      >
        <div class="text-brand text-3xl mb-3">🚚</div>
        <h3 class="font-semibold text-text-primary">配送管理</h3>
        <p class="text-text-secondary text-sm mt-1">管理配送任务与路径规划</p>
      </router-link>
    </div>

    <div class="bg-white rounded-card shadow-card p-6">
      <h2 class="font-semibold text-text-primary mb-4">系统简介</h2>
      <p class="text-text-secondary text-sm leading-relaxed">
        本系统基于 A* 算法实现智能路径规划，为校园快递配送提供最优路线。
        支持虚拟货架管理、上门配送预约、包裹状态追踪等功能，
        有效提升校园快递取件效率，减少排队时间。
      </p>
    </div>
  </div>
</template>

<script setup>
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
</script>
