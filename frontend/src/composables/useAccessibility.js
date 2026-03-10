import { onMounted, onUnmounted } from 'vue'

/**
 * 无障碍支持：键盘导航、高对比度、语音播报（文档 3.2）
 */
export function useAccessibility() {
  const shortcuts = new Map([
    ['KeyP', { action: 'focus_package_search', description: '聚焦包裹搜索' }],
    ['KeyM', { action: 'toggle_map_view', description: '切换地图视图' }],
    ['KeyR', { action: 'refresh_list', description: '刷新列表' }],
    ['KeyH', { action: 'show_help', description: '显示帮助' }],
    ['Slash', { action: 'focus_search', description: '聚焦搜索框' }]
  ])

  const handlers = {}

  function setupKeyboardShortcuts(customHandlers = {}) {
    Object.assign(handlers, customHandlers)
  }

  function handleKeydown(e) {
    if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return
    const key = e.code
    const shortcut = shortcuts.get(key)
    if (shortcut && handlers[shortcut.action]) {
      e.preventDefault()
      handlers[shortcut.action]()
    }
  }

  function applyTheme(theme) {
    document.body.classList.remove(
      'theme-high-contrast',
      'theme-dark-high-contrast',
      'theme-yellow-black'
    )
    if (theme && theme !== 'default') {
      document.body.classList.add(`theme-${theme}`)
    }
    try {
      localStorage.setItem('preferredTheme', theme || 'default')
    } catch (_) {}
  }

  function speak(text, options = {}) {
    if (typeof window === 'undefined' || !window.speechSynthesis) return
    const u = new SpeechSynthesisUtterance(text)
    u.lang = 'zh-CN'
    u.rate = options.rate ?? 1
    u.volume = options.volume ?? 1
    window.speechSynthesis.speak(u)
  }

  onMounted(() => {
    const saved = localStorage.getItem('preferredTheme')
    if (saved) applyTheme(saved)
    window.addEventListener('keydown', handleKeydown)
  })

  onUnmounted(() => {
    window.removeEventListener('keydown', handleKeydown)
  })

  return { setupKeyboardShortcuts, applyTheme, speak }
}
