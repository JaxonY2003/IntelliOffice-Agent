import { requestJson } from './http'

export async function loginWithPassword(payload) {
  return requestJson('/api/auth/login', {
    method: 'POST',
    attachAuthToken: false,
    skipAuthRefresh: true,
    body: JSON.stringify(payload),
  })
}
