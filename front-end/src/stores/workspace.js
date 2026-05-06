import { computed, reactive } from 'vue'
import { mockAccounts, roleOptions, rolePresets } from '../data/mockWorkspace'

const storageKeys = {
  token: 'token',
  role: 'role',
  profile: 'userProfile',
}

const state = reactive({
  initialized: false,
  isAuthenticated: false,
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

function getAccount(role) {
  return mockAccounts[normalizeRole(role)]
}

function createStarterConversation(role) {
  const preset = rolePresets[role]
  const roleMeta = getRoleOption(role)
  const id = `${role}-starter`

  return {
    conversation: {
      id,
      title: preset.starterTitle,
      preview: preset.firstReply,
      updatedAt: '刚刚',
    },
    messages: [
      {
        id: `${id}-assistant-welcome`,
        sender: 'assistant',
        name: 'Office Agent',
        time: '09:30',
        text: `欢迎进入${roleMeta.workspaceTitle}。`,
      },
      {
        id: `${id}-user`,
        sender: 'user',
        name: '我',
        time: '09:31',
        text: preset.starterPrompt,
      },
      {
        id: `${id}-assistant`,
        sender: 'assistant',
        name: 'Office Agent',
        time: '09:31',
        text: preset.firstReply,
      },
    ],
  }
}

function ensureWorkspaceSeed(role) {
  if (state.conversations.length > 0) return

  const starter = createStarterConversation(role)
  state.conversations = [starter.conversation]
  state.messagesByConversation = {
    [starter.conversation.id]: starter.messages,
  }
  state.currentConversationId = starter.conversation.id
}

function resetWorkspace() {
  state.conversations = []
  state.currentConversationId = ''
  state.messagesByConversation = {}
}

function persistSession(token, profile, role) {
  localStorage.setItem(storageKeys.token, token)
  localStorage.setItem(storageKeys.role, role)
  localStorage.setItem(storageKeys.profile, JSON.stringify(profile))
}

function hydrateSession() {
  const token = localStorage.getItem(storageKeys.token)
  const role = localStorage.getItem(storageKeys.role)
  const rawProfile = localStorage.getItem(storageKeys.profile)

  if (!token || !role || !rawProfile) {
    state.initialized = true
    return
  }

  try {
    state.userProfile = JSON.parse(rawProfile)
    state.isAuthenticated = true
    ensureWorkspaceSeed(role)
  } catch {
    clearSession()
  } finally {
    state.initialized = true
  }
}

function clearSession() {
  localStorage.removeItem(storageKeys.token)
  localStorage.removeItem(storageKeys.role)
  localStorage.removeItem(storageKeys.profile)
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

  if (!token || !username) return

  const profile = buildUserProfile(role, username)
  const authToken = session?.tokenType ? `${session.tokenType} ${token}` : token

  persistSession(authToken, profile, role)
  state.userProfile = profile
  state.isAuthenticated = true
  resetWorkspace()
  ensureWorkspaceSeed(role)
}

function logout() {
  clearSession()
}

function selectConversation(id) {
  state.currentConversationId = id
}

function createConversation() {
  const role = state.userProfile?.role ?? 'employee'
  const id = `chat-${Date.now()}`
  const conversation = {
    id,
    title: '新的对话',
    preview: '开始新的问题或任务...',
    updatedAt: '刚刚',
  }

  const welcomeMessage = {
    id: `${id}-assistant`,
    sender: 'assistant',
    name: 'Office Agent',
    time: '现在',
    text: `新的对话已创建。你可以继续让我帮你处理${getRoleOption(role).label}相关的问题。`,
  }

  state.conversations = [conversation, ...state.conversations]
  state.messagesByConversation = {
    ...state.messagesByConversation,
    [id]: [welcomeMessage],
  }
  state.currentConversationId = id
}

function deleteConversation(id) {
  if (!id || !state.messagesByConversation[id]) return

  const remainingConversations = state.conversations.filter((item) => item.id !== id)
  const nextMessages = { ...state.messagesByConversation }
  delete nextMessages[id]

  state.conversations = remainingConversations
  state.messagesByConversation = nextMessages

  if (state.currentConversationId === id) {
    state.currentConversationId = remainingConversations[0]?.id ?? ''
  }

  if (state.conversations.length === 0) {
    createConversation()
  }
}

async function appendUserMessage(text) {
  const conversationId = state.currentConversationId
  if (!conversationId) return

  const time = new Date().toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
  })

  const userMessage = {
    id: `${conversationId}-${Date.now()}-user`,
    sender: 'user',
    name: '我',
    time,
    text,
  }

  state.messagesByConversation = {
    ...state.messagesByConversation,
    [conversationId]: [...(state.messagesByConversation[conversationId] ?? []), userMessage],
  }

  state.conversations = state.conversations.map((item) =>
    item.id === conversationId
      ? {
          ...item,
          title: item.title === '新的对话' ? text.slice(0, 10) : item.title,
          preview: text,
          updatedAt: '刚刚',
        }
      : item,
  )

  await new Promise((resolve) => setTimeout(resolve, 720))

  const assistantReply = {
    id: `${conversationId}-${Date.now()}-assistant`,
    sender: 'assistant',
    name: 'Office Agent',
    time: new Date().toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit',
    }),
    text: `已收到你的问题：“${text}”。当前这里先用前端模拟回复，后续接入真实 agent 后，我可以把它替换成流式对话、工具调用和多轮上下文能力。`,
  }

  state.messagesByConversation = {
    ...state.messagesByConversation,
    [conversationId]: [...state.messagesByConversation[conversationId], assistantReply],
  }

  state.conversations = state.conversations.map((item) =>
    item.id === conversationId
      ? {
          ...item,
          preview: assistantReply.text,
          updatedAt: '刚刚',
        }
      : item,
  )
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
    selectConversation,
    appendUserMessage,
    workspaceRole,
    currentConversation,
    currentMessages,
    getRoleOption,
  }
}
