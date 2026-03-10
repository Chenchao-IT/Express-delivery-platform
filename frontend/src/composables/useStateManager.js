import { ref } from 'vue'

/**
 * 前端状态管理器 - 幂等性与冲突解决（文档 3.1）
 */
const stateVersions = new Map()
const pendingOps = new Map()
const OPERATION_TIMEOUT = 30000

export function useStateManager() {
  function generateOpId(op) {
    return `${op.entityId}-${op.type}-${Date.now()}`
  }

  async function executeStateChange(operation, executor) {
    const opId = generateOpId(operation)
    const currentVersion = stateVersions.get(operation.entityId) ?? 0

    if (pendingOps.has(opId)) {
      const pending = pendingOps.get(opId)
      if (Date.now() - pending.startTime < OPERATION_TIMEOUT) {
        return { status: 'processing', operationId: opId }
      }
      pendingOps.delete(opId)
    }

    if (operation.expectedVersion != null && operation.expectedVersion !== currentVersion) {
      return {
        status: 'conflict',
        currentVersion,
        operationId: opId,
        suggestedAction: '请刷新页面后重试'
      }
    }

    pendingOps.set(opId, { operation, startTime: Date.now(), status: 'processing' })

    try {
      const result = await executor(operation)
      stateVersions.set(operation.entityId, currentVersion + 1)
      pendingOps.delete(opId)
      return { status: 'success', operationId: opId, newVersion: currentVersion + 1, result }
    } catch (error) {
      pendingOps.delete(opId)
      return {
        status: 'error',
        operationId: opId,
        error: error.message,
        retryable: isRetryableError(error)
      }
    }
  }

  function isRetryableError(error) {
    const msg = (error?.message || '').toLowerCase()
    return msg.includes('network') || msg.includes('timeout') || error?.response?.status >= 500
  }

  function getVersion(entityId) {
    return stateVersions.get(entityId) ?? 0
  }

  function setVersion(entityId, version) {
    stateVersions.set(entityId, version)
  }

  return { executeStateChange, getVersion, setVersion }
}
