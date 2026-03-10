<template>
  <router-view />
</template>

<script setup>
import { onMounted } from 'vue'
import { usePerformanceMonitor } from '@/composables/usePerformanceMonitor'

const { startMonitoring } = usePerformanceMonitor()
onMounted(() => startMonitoring())
</script>
