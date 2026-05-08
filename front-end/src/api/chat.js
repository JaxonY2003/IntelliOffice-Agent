import { requestJson } from './http'

export function fetchChatSessions() {
  return requestJson('/api/chat/sessions')
}

export function createChatSession() {
  return requestJson('/api/chat/newSession', {
    method: 'POST',
  })
}

export function fetchSessionMessages(sessionId) {
  const params = new URLSearchParams({
    sessionId: String(sessionId),
  })

  return requestJson(`/api/chat/messages?${params.toString()}`)
}
