<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import sidebarPanelIcon from '../assets/celan.png'
import { useWorkspaceStore } from '../stores/workspace'

const router = useRouter()
const {
  state,
  logout,
  createConversation,
  deleteConversation,
  loadConversations,
  selectConversation,
  appendUserMessage,
  workspaceRole,
  currentConversation,
  currentMessages,
} = useWorkspaceStore()

const toast = ref({
  visible: false,
  type: 'success',
  text: '',
})
const messageDraft = ref('')
const sidebarOpen = ref(true)
let toastTimer = null

const userProfile = computed(() => state.userProfile)
const conversations = computed(() => state.conversations)
const currentConversationId = computed(() => state.currentConversationId)
const sidebarToggleLabel = computed(() => (sidebarOpen.value ? '收起侧栏' : '展开侧栏'))
const userAvatarImage = computed(() => {
  const candidate = userProfile.value?.avatarUrl ?? userProfile.value?.avatar ?? ''
  return typeof candidate === 'string' ? candidate.trim() : ''
})
const userAvatarText = computed(() => {
  const label = userProfile.value?.name?.trim() || workspaceRole.value?.label || 'U'
  return label.slice(0, 1)
})
const isLoadingSessions = computed(() => state.isLoadingSessions)
const isLoadingMessages = computed(() => state.isLoadingMessages)
const hasConversations = computed(() => conversations.value.length > 0)
const currentConversationTitle = computed(() => {
  if (isLoadingSessions.value && !currentConversation.value) {
    return '正在加载会话...'
  }

  return currentConversation.value?.title || '聊天工作台'
})

function showToast(type, text) {
  if (toastTimer) clearTimeout(toastTimer)

  toast.value = {
    visible: true,
    type,
    text,
  }

  toastTimer = setTimeout(() => {
    toast.value.visible = false
  }, 2600)
}

function getErrorMessage(error, fallbackText) {
  return error instanceof Error ? error.message : fallbackText
}

async function initializeChatWorkspace() {
  try {
    await loadConversations()
  } catch (error) {
    showToast('error', getErrorMessage(error, '聊天数据加载失败，请稍后重试。'))
  }
}

async function sendMessage() {
  const text = messageDraft.value.trim()
  if (!text) {
    showToast('error', '请输入消息后再发送。')
    return
  }

  const isSent = await appendUserMessage(text)
  if (!isSent) {
    showToast('error', '发送消息接口尚未接入，当前先展示后端历史消息。')
    return
  }

  messageDraft.value = ''
}

function handleComposerKeydown(event) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendMessage()
  }
}

async function handleCreateConversation() {
  const isCreated = await createConversation()
  if (!isCreated) {
    showToast('error', '后端暂未开放新建会话接口。')
  }
}

async function handleDeleteConversation(id) {
  const isDeleted = await deleteConversation(id)
  if (!isDeleted) {
    showToast('error', '后端暂未开放删除会话接口。')
    return
  }

  showToast('success', '聊天记录已删除。')
}

async function handleSelectConversation(id) {
  try {
    await selectConversation(id)
  } catch (error) {
    showToast('error', getErrorMessage(error, '会话内容加载失败，请稍后重试。'))
  }
}

function toggleSidebar() {
  sidebarOpen.value = !sidebarOpen.value
}

async function handleLogout() {
  logout()
  await router.push('/login')
}

onMounted(() => {
  if (state.isAuthenticated) {
    initializeChatWorkspace()
  }
})
</script>

<template>
  <main class="app-shell workspace">
    <transition name="toast-fade">
      <div v-if="toast.visible" class="toast" :class="toast.type">
        {{ toast.text }}
      </div>
    </transition>

    <section class="workspace-shell" :class="{ 'sidebar-collapsed': !sidebarOpen }">
      <aside class="workspace-sidebar">
        <div class="workspace-sidebar-top">
          <div v-if="sidebarOpen" class="workspace-brand">
            <div class="brand-mark">IA</div>
            <div>
              <p class="brand-label">IntelliOffice</p>
              <h2>{{ workspaceRole.workspaceTitle }}</h2>
            </div>
          </div>

          <button
            type="button"
            class="sidebar-icon-button"
            :aria-label="sidebarToggleLabel"
            :title="sidebarToggleLabel"
            @click="toggleSidebar"
          >
            <img class="sidebar-icon-image" :src="sidebarPanelIcon" alt="" aria-hidden="true" />
          </button>
        </div>

        <button
          v-if="sidebarOpen"
          type="button"
          class="new-chat-button"
          @click="handleCreateConversation"
        >
          新建对话
        </button>

        <div v-if="sidebarOpen" class="conversation-pane">
          <p class="sidebar-title">聊天记录</p>
          <div class="conversation-scroll">
            <div
              v-for="item in conversations"
              :key="item.id"
              class="conversation-item"
              :class="{ active: currentConversationId === item.id }"
            >
              <button
                type="button"
                class="conversation-main"
                @click="handleSelectConversation(item.id)"
              >
                <strong>{{ item.title }}</strong>
              </button>

              <button
                type="button"
                class="conversation-delete"
                aria-label="删除聊天记录"
                @click="handleDeleteConversation(item.id)"
              >
                删除
              </button>
            </div>
          </div>
        </div>

        <div class="sidebar-footer">
          <div class="footer-mini-card" :class="{ compact: !sidebarOpen }">
            <div class="avatar-menu">
              <button
                type="button"
                class="footer-avatar-button"
                aria-label="打开个人菜单"
                title="个人菜单"
              >
                <div class="footer-avatar">
                  <img
                    v-if="userAvatarImage"
                    class="avatar-image"
                    :src="userAvatarImage"
                    :alt="`${userProfile?.name || workspaceRole.label}头像`"
                  />
                  <span v-else>{{ userAvatarText }}</span>
                </div>
              </button>

              <div class="avatar-menu-popover">
                <button type="button" class="avatar-menu-item">个人信息</button>
                <button type="button" class="avatar-menu-item danger" @click="handleLogout">
                  退出登录
                </button>
              </div>
            </div>
            <div v-if="sidebarOpen" class="footer-mini-copy">
              <span>当前身份</span>
              <strong>{{ workspaceRole.label }}</strong>
            </div>
          </div>
        </div>
      </aside>

      <section class="chat-stage">
        <header class="chat-topbar">
          <div class="chat-topbar-left">
            <div>
              <p class="topbar-kicker">AI Workspace</p>
              <h1>{{ currentConversationTitle }}</h1>
            </div>
          </div>
          <div class="topbar-chip">Agent Online</div>
        </header>

        <div class="chat-stream">
          <div class="welcome-panel">
            <p class="welcome-kicker">Office Agent</p>
            <h3>你好，{{ userProfile?.name }}</h3>
            <p>
              会话列表和历史消息已经改成从后端读取。后续接入发送、新建和删除接口后，这里可以继续扩展成完整的真实聊天工作台。
            </p>
          </div>

          <div v-if="isLoadingSessions" class="empty-state-card">
            正在加载你的聊天会话...
          </div>

          <div v-else-if="!hasConversations" class="empty-state-card">
            当前还没有可展示的聊天会话。
          </div>

          <div v-else-if="isLoadingMessages" class="empty-state-card">
            正在加载会话内容...
          </div>

          <div v-else-if="currentMessages.length === 0" class="empty-state-card">
            这个会话暂时还没有消息内容。
          </div>

          <article
            v-if="!isLoadingSessions && !isLoadingMessages && hasConversations && currentMessages.length > 0"
            v-for="message in currentMessages"
            :key="message.id"
            class="message-row"
            :class="message.sender"
          >
            <div class="message-badge">{{ message.badge }}</div>
            <div class="message-bubble">
              <div class="message-meta">
                <strong>{{ message.name }}</strong>
                <span>{{ message.time }}</span>
              </div>
              <p>{{ message.text }}</p>
            </div>
          </article>
        </div>

        <div class="composer-shell">
          <div class="composer-box">
            <textarea
              v-model="messageDraft"
              class="composer-input"
              placeholder="给 IntelliOffice Agent 发送消息..."
              rows="1"
              @keydown="handleComposerKeydown"
            />
            <button type="button" class="composer-send" @click="sendMessage">
              发送
            </button>
          </div>
          <p class="composer-tip">Enter 发送，Shift + Enter 换行。当前已接入会话读取，发送接口待接入。</p>
        </div>
      </section>
    </section>
  </main>
</template>
