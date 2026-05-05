<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useWorkspaceStore } from '../stores/workspace'

const router = useRouter()
const {
  state,
  logout,
  createConversation,
  deleteConversation,
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

async function sendMessage() {
  const text = messageDraft.value.trim()
  if (!text) {
    showToast('error', '请输入消息后再发送。')
    return
  }

  messageDraft.value = ''
  await appendUserMessage(text)
}

function handleComposerKeydown(event) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendMessage()
  }
}

function handleDeleteConversation(id) {
  deleteConversation(id)
  showToast('success', '聊天记录已删除。')
}

function toggleSidebar() {
  sidebarOpen.value = !sidebarOpen.value
}

async function handleLogout() {
  logout()
  await router.push('/login')
}
</script>

<template>
  <main class="app-shell workspace">
    <transition name="toast-fade">
      <div v-if="toast.visible" class="toast" :class="toast.type">
        {{ toast.text }}
      </div>
    </transition>

    <section class="workspace-shell" :class="{ 'sidebar-collapsed': !sidebarOpen }">
      <aside class="workspace-sidebar" :class="{ hidden: !sidebarOpen }">
        <div class="workspace-sidebar-top">
          <div class="workspace-brand">
            <div class="brand-mark">IA</div>
            <div>
              <p class="brand-label">IntelliOffice</p>
              <h2>{{ workspaceRole.workspaceTitle }}</h2>
            </div>
          </div>

          <button type="button" class="sidebar-toggle ghost-dark" @click="toggleSidebar">
            收起
          </button>
        </div>

        <div class="profile-card">
          <div class="avatar-ring">{{ userProfile?.name?.slice(0, 1) }}</div>
          <div class="profile-copy">
            <strong>{{ userProfile?.name }}</strong>
            <span>{{ userProfile?.roleName }}</span>
          </div>
        </div>

        <button type="button" class="new-chat-button" @click="createConversation">
          新建对话
        </button>

        <div class="conversation-pane">
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
                @click="selectConversation(item.id)"
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
          <div class="footer-mini-card">
            <span>当前身份</span>
            <strong>{{ workspaceRole.label }}</strong>
          </div>
          <button type="button" class="logout-button" @click="handleLogout">退出登录</button>
        </div>
      </aside>

      <section class="chat-stage">
        <header class="chat-topbar">
          <div class="chat-topbar-left">
            <button type="button" class="sidebar-toggle light" @click="toggleSidebar">
              {{ sidebarOpen ? '隐藏侧栏' : '显示侧栏' }}
            </button>
            <div>
              <p class="topbar-kicker">AI Workspace</p>
              <h1>{{ currentConversation?.title || '新的对话' }}</h1>
            </div>
          </div>
          <div class="topbar-chip">Agent Online</div>
        </header>

        <div class="chat-stream">
          <div class="welcome-panel">
            <p class="welcome-kicker">Office Agent</p>
            <h3>你好，{{ userProfile?.name }}</h3>
            <p>
              这里已经接好一个前端聊天工作台。后续接入真实 agent 后，可以继续扩展成知识检索、业务流程和工具调用入口。
            </p>
          </div>

          <article
            v-for="message in currentMessages"
            :key="message.id"
            class="message-row"
            :class="message.sender"
          >
            <div class="message-badge">{{ message.sender === 'assistant' ? 'AI' : '我' }}</div>
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
          <p class="composer-tip">Enter 发送，Shift + Enter 换行。当前为前端模拟对话界面。</p>
        </div>
      </section>
    </section>
  </main>
</template>
