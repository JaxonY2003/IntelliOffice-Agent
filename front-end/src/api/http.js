const SUCCESS_CODE = 200
const TOKEN_STORAGE_KEY = 'token'

function getStoredAuthToken() {
  return sessionStorage.getItem(TOKEN_STORAGE_KEY) || localStorage.getItem(TOKEN_STORAGE_KEY) || ''
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
    throw new Error(result?.message || '请求失败，请稍后重试。')
  }

  if (result?.code !== SUCCESS_CODE) {
    throw new Error(result?.message || '接口返回异常，请稍后重试。')
  }

  return result?.data
}
