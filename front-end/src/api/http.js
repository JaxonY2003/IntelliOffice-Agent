import {
  clearAuthSession,
  getStoredAccessToken,
  getStoredRefreshToken,
  updateStoredAuthTokens,
} from '../utils/authSession'

const SUCCESS_CODE = 200
const AUTH_EXPIRED_EVENT = 'app:auth-expired'
const AUTH_EXPIRED_CODE = 601
const AUTH_INVALID_CODE = 602
const UNAUTHORIZED_CODE = 305
const REFRESH_ENDPOINT = '/api/auth/refresh'

let refreshPromise = null

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

async function parseJsonResponse(response) {
  try {
    return await response.json()
  } catch {
    throw new Error('接口响应解析失败，请确认后端服务是否正常运行。')
  }
}

async function performFetch(url, options = {}, attachAuthToken = true) {
  const headers = new Headers(options.headers || {})
  const hasJsonBody = options.body && !headers.has('Content-Type')

  if (hasJsonBody) {
    headers.set('Content-Type', 'application/json')
  }

  const authToken = getStoredAccessToken()
  if (attachAuthToken && authToken && !headers.has('Authorization')) {
    headers.set('Authorization', authToken)
  }

  const response = await fetch(url, {
    ...options,
    headers,
  })

  const result = await parseJsonResponse(response)

  return {
    response,
    result,
  }
}

async function refreshAuthSession() {
  if (refreshPromise) {
    return refreshPromise
  }

  refreshPromise = (async () => {
    const refreshToken = getStoredRefreshToken()
    if (!refreshToken) {
      throw createAuthExpiredError('登录状态已失效，请重新登录。')
    }

    const { response, result } = await performFetch(
      REFRESH_ENDPOINT,
      {
        method: 'POST',
        body: JSON.stringify({ refreshToken }),
      },
      false,
    )

    if (
      !response.ok
      || result?.code !== SUCCESS_CODE
      || !result?.data?.token
      || !result?.data?.refreshToken
    ) {
      clearAuthSession()
      throw createAuthExpiredError(result?.message || '登录状态已失效，请重新登录。')
    }

    updateStoredAuthTokens({
      token: result.data.token,
      refreshToken: result.data.refreshToken,
      tokenType: result.data.tokenType,
    })

    return result.data
  })().finally(() => {
    refreshPromise = null
  })

  return refreshPromise
}

export async function requestJson(url, options = {}) {
  const {
    skipAuthRefresh = false,
    attachAuthToken = true,
    ...fetchOptions
  } = options

  const { response, result } = await performFetch(url, fetchOptions, attachAuthToken)

  if (!response.ok || result?.code !== SUCCESS_CODE) {
    if (isAuthFailureStatus(response.status, result?.code)) {
      if (!skipAuthRefresh) {
        try {
          await refreshAuthSession()
          return await requestJson(url, {
            ...options,
            skipAuthRefresh: true,
          })
        } catch (error) {
          notifyAuthExpired(error?.message || result?.message)
          throw createAuthExpiredError(error?.message || result?.message)
        }
      }

      notifyAuthExpired(result?.message)
      throw createAuthExpiredError(result?.message)
    }

    throw new Error(result?.message || '请求失败，请稍后重试。')
  }

  return result?.data
}

export {
  AUTH_EXPIRED_EVENT,
  refreshAuthSession,
}
