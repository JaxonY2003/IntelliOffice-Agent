import { computed, reactive } from 'vue'
import {
  createChatSession,
  deleteChatSession,
  fetchChatSessions,
  fetchSessionMessages,
  renameChatSession,
  sendChatMessage,
} from '../api/chat'
import { mockAccounts, roleOptions } from '../data/mockWorkspace'

const storageKeys = {
  token: 'token',
  role: 'role',
  profile: 'userProfile',
}

const state = reactive({
  initialized: false,
  isAuthenticated: false,
  isLoadingSessions: false,
  isLoadingMessages: false,
  isCreatingConversation: false,
  isSendingMessage: false,
  deletingConversationId: '',
  renamingConversationId: '',
  userProfile: null,
  conversations: [],
  currentConversationId: '',
  messagesByConversation: {},
})

function getRoleOption(role) {
  return roleOptions.find((item) => item.key === role) ?? roleOptions[0]
}

function normalizeRole(role) {
  if (typeof role !== 'string') return 'employee'
  return role.trim().toLowerCase()
}

function normalizeConversationId(id) {
  if (id === null || id === undefined) return ''
  return String(id)
}

function getAccount(role) {
  return mockAccounts[normalizeRole(role)]
}

function resetWorkspace() {
  state.conversations = []
  state.currentConversationId = ''
  state.messagesByConversation = {}
  state.isLoadingSessions = false
  state.isLoadingMessages = false
  state.isCreatingConversation = false
  state.isSendingMessage = false
  state.deletingConversationId = ''
  state.renamingConversationId = ''
}

function getSessionStorages() {
  return [sessionStorage, localStorage]
}

function clearSessionFromStorage(storage) {
  storage.removeItem(storageKeys.token)
  storage.removeItem(storageKeys.role)
  storage.removeItem(storageKeys.profile)
}

function readSessionFromStorage(storage) {
  const token = storage.getItem(storageKeys.token)
  const role = storage.getItem(storageKeys.role)
  const rawProfile = storage.getItem(storageKeys.profile)

  if (!token || !role || !rawProfile) {
    return null
  }

  return {
    token,
    role,
    rawProfile,
  }
}

function persistSession(token, profile, role, remember) {
  const targetStorage = remember ? localStorage : sessionStorage

  getSessionStorages().forEach(clearSessionFromStorage)

  targetStorage.setItem(storageKeys.token, token)
  targetStorage.setItem(storageKeys.role, role)
  targetStorage.setItem(storageKeys.profile, JSON.stringify(profile))
}

function hydrateSession() {
  const storedSession =
    readSessionFromStorage(sessionStorage) ?? readSessionFromStorage(localStorage)

  if (!storedSession) {
    state.initialized = true
    return
  }

  try {
    const normalizedRole = normalizeRole(storedSession.role)
    state.userProfile = {
      ...JSON.parse(storedSession.rawProfile),
      role: normalizedRole,
    }
    state.isAuthenticated = true
  } catch {
    clearSession()
  } finally {
    state.initialized = true
  }
}

function clearSession() {
  getSessionStorages().forEach(clearSessionFromStorage)
  state.isAuthenticated = false
  state.userProfile = null
  resetWorkspace()
}

function buildUserProfile(role, username) {
  const normalizedRole = normalizeRole(role)
  const account = getAccount(normalizedRole)
  const roleMeta = getRoleOption(normalizedRole)
  const matchedDemoAccount = account?.username === username ? account : null

  return {
    name: matchedDemoAccount?.displayName ?? username,
    username,
    roleName: matchedDemoAccount?.roleName ?? roleMeta.label,
    role: normalizedRole,
  }
}

function login(session) {
  const role = normalizeRole(session?.type ?? session?.role)
  const username = session?.username?.trim()
  const token = session?.token?.trim()
  const remember = session?.remember ?? true

  if (!token || !username) return

  const profile = buildUserProfile(role, username)
  const authToken = session?.tokenType ? `${session.tokenType} ${token}` : token

  persistSession(authToken, profile, role, remember)
  state.userProfile = profile
  state.isAuthenticated = true
  resetWorkspace()
}

function logout() {
  clearSession()
}

function formatMessageTime(value) {
  if (!value) return ''

  const raw = String(value)
  if (raw.includes('T') && raw.length >= 16) {
    return raw.slice(11, 16)
  }

  const date = new Date(raw)
  if (Number.isNaN(date.getTime())) {
    return raw
  }

  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
  })
}

function formatConversationTime(value) {
  if (!value) return ''

  const raw = String(value)
  if (raw.includes('T') && raw.length >= 16) {
    return raw.slice(5, 16).replace('T', ' ')
  }

  const date = new Date(raw)
  if (Number.isNaN(date.getTime())) {
    return raw
  }

  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function mapConversation(dto) {
  return {
    id: normalizeConversationId(dto?.id),
    title: dto?.title?.trim() || `会话 ${dto?.id ?? ''}`.trim(),
    preview: '',
    updatedAt: formatConversationTime(dto?.createTime),
  }
}

function mapSenderTypeToUi(senderType) {
  const normalizedType = typeof senderType === 'string' ? senderType.trim().toUpperCase() : ''

  if (normalizedType === 'USER') {
    return {
      sender: 'user',
      name: state.userProfile?.name || '我',
      badge: '我',
    }
  }

  if (normalizedType === 'SYSTEM') {
    return {
      sender: 'assistant',
      name: '系统',
      badge: 'SYS',
    }
  }

  return {
    sender: 'assistant',
    name: 'Office Agent',
    badge: 'AI',
  }
}

function mapMessage(dto) {
  const senderMeta = mapSenderTypeToUi(dto?.senderType)

  return {
    id: normalizeConversationId(dto?.id),
    sender: senderMeta.sender,
    badge: senderMeta.badge,
    name: senderMeta.name,
    time: formatMessageTime(dto?.createTime),
    text: dto?.content ?? '',
    messageType: dto?.messageType ?? 'TEXT',
    senderType: dto?.senderType ?? '',
    pending: dto?.pending === true,
  }
}

function syncConversationPreview(conversationId, messages) {
  const latestMessage = messages.at(-1)

  state.conversations = state.conversations.map((item) =>
    item.id === conversationId
      ? {
          ...item,
          preview: latestMessage?.text ?? item.preview,
          updatedAt: latestMessage?.time || item.updatedAt,
        }
      : item,
  )
}

function updateConversationPreview(conversationId, preview, updatedAt) {
  state.conversations = state.conversations.map((item) =>
    item.id === conversationId
      ? {
          ...item,
          preview: preview ?? item.preview,
          updatedAt: updatedAt ?? item.updatedAt,
        }
      : item,
  )
}

function createOptimisticUserMessage(content) {
  const senderMeta = mapSenderTypeToUi('USER')
  const currentTime = formatMessageTime(new Date().toISOString())

  return {
    id: `temp-user-${Date.now()}`,
    sender: senderMeta.sender,
    badge: senderMeta.badge,
    name: senderMeta.name,
    time: currentTime,
    text: content,
    messageType: 'TEXT',
    senderType: 'USER',
    pending: true,
  }
}

async function loadConversationMessages(id, options = {}) {
  const normalizedId = normalizeConversationId(id)
  const force = options.force === true

  if (!state.isAuthenticated || !normalizedId) {
    return []
  }

  state.currentConversationId = normalizedId

  if (!force && state.messagesByConversation[normalizedId]) {
    return state.messagesByConversation[normalizedId]
  }

  state.isLoadingMessages = true

  try {
    const messageList = await fetchSessionMessages(normalizedId)
    const mappedMessages = Array.isArray(messageList) ? messageList.map(mapMessage) : []

    state.messagesByConversation = {
      ...state.messagesByConversation,
      [normalizedId]: mappedMessages,
    }

    syncConversationPreview(normalizedId, mappedMessages)
    return mappedMessages
  } finally {
    state.isLoadingMessages = false
  }
}

async function loadConversations() {
  if (!state.isAuthenticated) {
    return []
  }

  state.isLoadingSessions = true

  try {
    const sessionList = await fetchChatSessions()
    const mappedConversations = Array.isArray(sessionList) ? sessionList.map(mapConversation) : []
    const availableConversationIds = new Set(mappedConversations.map((item) => item.id))
    const nextCurrentConversationId = availableConversationIds.has(state.currentConversationId)
      ? state.currentConversationId
      : mappedConversations[0]?.id ?? ''

    state.conversations = mappedConversations
    state.currentConversationId = nextCurrentConversationId
    state.messagesByConversation = Object.fromEntries(
      Object.entries(state.messagesByConversation).filter(([conversationId]) =>
        availableConversationIds.has(conversationId),
      ),
    )

    if (!nextCurrentConversationId) {
      return []
    }

    return await loadConversationMessages(nextCurrentConversationId, { force: true })
  } finally {
    state.isLoadingSessions = false
  }
}

async function selectConversation(id) {
  return await loadConversationMessages(id)
}

async function createConversation() {
  if (!state.isAuthenticated) {
    return null
  }

  state.isCreatingConversation = true

  try {
    const createdSession = await createChatSession()
    const mappedConversation = mapConversation(createdSession)

    state.conversations = [
      mappedConversation,
      ...state.conversations.filter((item) => item.id !== mappedConversation.id),
    ]
    state.currentConversationId = mappedConversation.id
    state.messagesByConversation = {
      ...state.messagesByConversation,
      [mappedConversation.id]: [],
    }

    return mappedConversation
  } finally {
    state.isCreatingConversation = false
  }
}

async function deleteConversation(id) {
  const normalizedId = normalizeConversationId(id)

  if (!state.isAuthenticated || !normalizedId) {
    return false
  }

  state.deletingConversationId = normalizedId

  try {
    await deleteChatSession(normalizedId)

    const remainingConversations = state.conversations.filter((item) => item.id !== normalizedId)
    const nextMessagesByConversation = { ...state.messagesByConversation }
    delete nextMessagesByConversation[normalizedId]

    state.conversations = remainingConversations
    state.messagesByConversation = nextMessagesByConversation

    if (state.currentConversationId === normalizedId) {
      const nextConversationId = remainingConversations[0]?.id ?? ''
      state.currentConversationId = nextConversationId

      if (nextConversationId) {
        await loadConversationMessages(nextConversationId)
      }
    }

    return true
  } finally {
    state.deletingConversationId = ''
  }
}

async function renameConversation(id, title) {
  const normalizedId = normalizeConversationId(id)
  const normalizedTitle = typeof title === 'string' ? title.trim() : ''

  if (!state.isAuthenticated || !normalizedId || !normalizedTitle) {
    return false
  }

  state.renamingConversationId = normalizedId

  try {
    await renameChatSession(normalizedId, normalizedTitle)

    state.conversations = state.conversations.map((item) =>
      item.id === normalizedId
        ? {
            ...item,
            title: normalizedTitle,
          }
        : item,
    )

    return true
  } finally {
    state.renamingConversationId = ''
  }
}

async function appendUserMessage(content) {
  const conversationId = state.currentConversationId
  const normalizedContent = typeof content === 'string' ? content.trim() : ''

  if (!state.isAuthenticated || !conversationId || !normalizedContent) {
    return false
  }

  if (state.isSendingMessage) {
    throw new Error('Office Agent 正在回复上一条消息，请稍候。')
  }

  state.isSendingMessage = true
  const existingMessages = [...(state.messagesByConversation[conversationId] ?? [])]
  const optimisticUserMessage = createOptimisticUserMessage(normalizedContent)
  const optimisticMessages = [...existingMessages, optimisticUserMessage]

  state.messagesByConversation = {
    ...state.messagesByConversation,
    [conversationId]: optimisticMessages,
  }
  updateConversationPreview(conversationId, normalizedContent, optimisticUserMessage.time)

  try {
    const response = await sendChatMessage({
      sessionId: Number(conversationId),
      messageType: 'TEXT',
      content: normalizedContent,
    })

    const returnedMessages = [response?.userMessage, response?.agentMessage]
      .filter(Boolean)
      .map(mapMessage)

    if (returnedMessages.length === 0) {
      throw new Error('消息已发送，但未收到有效回复。')
    }

    const settledMessages = [...existingMessages, ...returnedMessages]
    state.messagesByConversation = {
      ...state.messagesByConversation,
      [conversationId]: settledMessages,
    }
    syncConversationPreview(conversationId, settledMessages)
    return true
  } catch (error) {
    state.messagesByConversation = {
      ...state.messagesByConversation,
      [conversationId]: existingMessages,
    }
    syncConversationPreview(conversationId, existingMessages)
    throw error
  } finally {
    state.isSendingMessage = false
  }
}

export function useWorkspaceStore() {
  const workspaceRole = computed(() =>
    getRoleOption(state.userProfile?.role ?? 'employee'),
  )

  const currentConversation = computed(
    () => state.conversations.find((item) => item.id === state.currentConversationId) ?? null,
  )

  const currentMessages = computed(
    () => state.messagesByConversation[state.currentConversationId] ?? [],
  )

  return {
    state,
    roleOptions,
    mockAccounts,
    hydrateSession,
    login,
    logout,
    createConversation,
    deleteConversation,
    renameConversation,
    appendUserMessage,
    loadConversations,
    loadConversationMessages,
    selectConversation,
    workspaceRole,
    currentConversation,
    currentMessages,
    getRoleOption,
  }
}
