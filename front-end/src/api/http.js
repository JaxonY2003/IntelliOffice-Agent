const SUCCESS_CODE = 200
const TOKEN_STORAGE_KEY = 'token'
const AUTH_EXPIRED_EVENT = 'app:auth-expired'
const AUTH_EXPIRED_CODE = 601
const AUTH_INVALID_CODE = 602
const UNAUTHORIZED_CODE = 305

function getStoredAuthToken() {
  return sessionStorage.getItem(TOKEN_STORAGE_KEY) || localStorage.getItem(TOKEN_STORAGE_KEY) || ''
}

function createAuthExpiredError(message) {
  const error = new Error(message || '登录状态已失效，请重新登录。')
  error.isAuthExpired = true
  return error
}

function notifyAuthExpired(message) {
  if (typeof window === 'undefined') return

  window.dispatchEvent(
    new CustomEvent(AUTH_EXPIRED_EVENT, {
      detail: {
        message: message || '登录状态已失效，请重新登录。',
      },
    }),
  )
}

function isAuthFailureStatus(responseStatus, resultCode) {
  return (
    responseStatus === 401
    || resultCode === AUTH_EXPIRED_CODE
    || resultCode === AUTH_INVALID_CODE
    || resultCode === UNAUTHORIZED_CODE
  )
}

export async function requestJson(url, options = {}) {
  const headers = new Headers(options.headers || {})
  const hasJsonBody = options.body && !headers.has('Content-Type')

  if (hasJsonBody) {
    headers.set('Content-Type', 'application/json')
  }

  const authToken = getStoredAuthToken()
  if (authToken && !headers.has('Authorization')) {
    headers.set('Authorization', authToken)
  }

  const response = await fetch(url, {
    ...options,
    headers,
  })

  let result = null

  try {
    result = await response.json()
  } catch {
    throw new Error('接口响应解析失败，请确认后端服务是否正常运行。')
  }

  if (!response.ok) {
    if (isAuthFailureStatus(response.status, result?.code)) {
      notifyAuthExpired(result?.message)
      throw createAuthExpiredError(result?.message)
    }

    throw new Error(result?.message || '请求失败，请稍后重试。')
  }

  if (result?.code !== SUCCESS_CODE) {
    if (isAuthFailureStatus(response.status, result?.code)) {
      notifyAuthExpired(result?.message)
      throw createAuthExpiredError(result?.message)
    }

    throw new Error(result?.message || '接口返回异常，请稍后重试。')
  }

  return result?.data
}

export { AUTH_EXPIRED_EVENT }
