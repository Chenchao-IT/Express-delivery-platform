import { ref } from 'vue'

/**
 * 前端性能预算监控（文档 2.3）
 */
const THRESHOLDS = {
  fcp: 1800,
  lcp: 2500,
  fid: 100,
  cls: 0.1,
  budgetWarning: 0.8
}

export function usePerformanceMonitor() {
  const metrics = ref({ fcp: 0, lcp: 0, fid: 0, cls: 0, ttfb: 0 })

  function startMonitoring() {
    if (typeof window === 'undefined' || !window.PerformanceObserver) return

    try {
      new PerformanceObserver((entryList) => {
        const entries = entryList.getEntries()
        const lastEntry = entries[entries.length - 1]
        if (lastEntry) {
          metrics.value.lcp = lastEntry.startTime
          checkBudget('lcp', lastEntry.startTime)
        }
      }).observe({ type: 'largest-contentful-paint', buffered: true })

      new PerformanceObserver((entryList) => {
        for (const entry of entryList.getEntries()) {
          metrics.value.cls += entry.value
        }
        checkBudget('cls', metrics.value.cls)
      }).observe({ type: 'layout-shift', buffered: true })
    } catch (e) {
      console.warn('Performance monitoring failed:', e)
    }
  }

  function checkBudget(metric, value) {
    const threshold = THRESHOLDS[metric]
    if (!threshold) return
    const usage = value / threshold
    if (usage > THRESHOLDS.budgetWarning) {
      console.warn(`[Performance] ${metric} 超预算: ${value.toFixed(0)}ms (阈值 ${threshold}ms)`)
    }
  }

  return { metrics, startMonitoring }
}
