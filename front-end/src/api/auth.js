const SUCCESS_CODE = 200

export async function loginWithPassword(payload) {
  const response = await fetch('/api/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  })

  let result = null

  try {
    result = await response.json()
  } catch {
    throw new Error('登录响应解析失败，请确认后端服务是否正常运行。')
  }

  if (!response.ok) {
    throw new Error(result?.message || '登录请求失败，请稍后重试。')
  }

  if (result?.code !== SUCCESS_CODE || !result?.data) {
    throw new Error(result?.message || '登录失败，请检查账号、密码和角色。')
  }

  return result.data
}
