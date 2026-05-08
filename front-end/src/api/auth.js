import { requestJson } from './http'

export async function loginWithPassword(payload) {
  return requestJson('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}
