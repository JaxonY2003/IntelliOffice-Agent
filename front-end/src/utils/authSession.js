const storageKeys = {
  token: 'token',
  refreshToken: 'refreshToken',
  role: 'role',
  profile: 'userProfile',
}

function getSessionStorages() {
  return [sessionStorage, localStorage]
}

function clearSessionFromStorage(storage) {
  storage.removeItem(storageKeys.token)
  storage.removeItem(storageKeys.refreshToken)
  storage.removeItem(storageKeys.role)
  storage.removeItem(storageKeys.profile)
}

function readSessionFromStorage(storage) {
  const token = storage.getItem(storageKeys.token)
  const refreshToken = storage.getItem(storageKeys.refreshToken) || ''
  const role = storage.getItem(storageKeys.role)
  const rawProfile = storage.getItem(storageKeys.profile)

  if (!token || !role || !rawProfile) {
    return null
  }

  return {
    storage,
    token,
    refreshToken,
    role,
    rawProfile,
  }
}

function buildAuthHeader(token, tokenType = 'Bearer') {
  const normalizedToken = typeof token === 'string' ? token.trim() : ''
  const normalizedType = typeof tokenType === 'string' ? tokenType.trim() : ''

  if (!normalizedToken) {
    return ''
  }

  return normalizedType ? `${normalizedType} ${normalizedToken}` : normalizedToken
}

function readStoredAuthSession() {
  for (const storage of getSessionStorages()) {
    const session = readSessionFromStorage(storage)
    if (session) {
      return session
    }
  }

  return null
}

function getStoredAccessToken() {
  return readStoredAuthSession()?.token || ''
}

function getStoredRefreshToken() {
  return readStoredAuthSession()?.refreshToken || ''
}

function persistAuthSession({ token, refreshToken, profile, role, remember, tokenType = 'Bearer' }) {
  const targetStorage = remember ? localStorage : sessionStorage
  const normalizedToken = buildAuthHeader(token, tokenType)
  const normalizedRefreshToken = typeof refreshToken === 'string' ? refreshToken.trim() : ''

  getSessionStorages().forEach(clearSessionFromStorage)

  if (normalizedToken) {
    targetStorage.setItem(storageKeys.token, normalizedToken)
  }

  if (normalizedRefreshToken) {
    targetStorage.setItem(storageKeys.refreshToken, normalizedRefreshToken)
  }

  targetStorage.setItem(storageKeys.role, role)
  targetStorage.setItem(storageKeys.profile, JSON.stringify(profile))
}

function updateStoredAuthTokens({ token, refreshToken, tokenType = 'Bearer' }) {
  const storedSession = readStoredAuthSession()
  const targetStorage = storedSession?.storage ?? sessionStorage
  const normalizedToken = buildAuthHeader(token, tokenType)
  const normalizedRefreshToken = typeof refreshToken === 'string' ? refreshToken.trim() : ''

  if (normalizedToken) {
    targetStorage.setItem(storageKeys.token, normalizedToken)
  }

  if (normalizedRefreshToken) {
    targetStorage.setItem(storageKeys.refreshToken, normalizedRefreshToken)
  }
}

function clearAuthSession() {
  getSessionStorages().forEach(clearSessionFromStorage)
}

function extractJwtToken(authToken) {
  if (typeof authToken !== 'string') return ''
  return authToken.startsWith('Bearer ') ? authToken.slice('Bearer '.length).trim() : authToken.trim()
}

function decodeJwtPayload(authToken) {
  const jwtToken = extractJwtToken(authToken)
  if (!jwtToken) return null

  const segments = jwtToken.split('.')
  if (segments.length !== 3) return null

  try {
    const base64 = segments[1].replace(/-/g, '+').replace(/_/g, '/')
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=')
    return JSON.parse(atob(padded))
  } catch {
    return null
  }
}

function isAuthTokenExpired(authToken) {
  const payload = decodeJwtPayload(authToken)
  const expiresAt = Number(payload?.exp)

  if (!Number.isFinite(expiresAt) || expiresAt <= 0) {
    return true
  }

  return expiresAt * 1000 <= Date.now()
}

export {
  clearAuthSession,
  getStoredAccessToken,
  getStoredRefreshToken,
  isAuthTokenExpired,
  persistAuthSession,
  readStoredAuthSession,
  storageKeys,
  updateStoredAuthTokens,
}
