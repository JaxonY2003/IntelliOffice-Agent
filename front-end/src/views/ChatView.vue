<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import sidebarPanelIcon from '../assets/celan.png'
import conversationMoreIcon from '../assets/gengduo.png'
import { useWorkspaceStore } from '../stores/workspace'

const router = useRouter()
const {
  state,
  logout,
  createConversation,
  deleteConversation,
  loadConversations,
  renameConversation,
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
const avatarMenuOpen = ref(false)
const activeConversationMenuId = ref('')
const deleteDialogVisible = ref(false)
const pendingDeleteConversation = ref(null)
const renameDialogVisible = ref(false)
const pendingRenameConversation = ref(null)
const renameDraft = ref('')
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
const isCreatingConversation = computed(() => state.isCreatingConversation)
const deletingConversationId = computed(() => state.deletingConversationId)
const renamingConversationId = computed(() => state.renamingConversationId)
const hasConversations = computed(() => conversations.value.length > 0)
const currentConversationTitle = computed(() => {
  if (isLoadingSessions.value && !currentConversation.value) {
    return '正在加载会话...'
  }

  return currentConversation.value?.title || '聊天工作台'
})
const pendingDeleteConversationTitle = computed(
  () => pendingDeleteConversation.value?.title || '当前会话',
)
const isDeletingPendingConversation = computed(
  () => deletingConversationId.value === pendingDeleteConversation.value?.id,
)
const pendingRenameConversationTitle = computed(
  () => pendingRenameConversation.value?.title || '当前会话',
)
const isRenamingPendingConversation = computed(
  () => renamingConversationId.value === pendingRenameConversation.value?.id,
)

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

function closeMenus() {
  avatarMenuOpen.value = false
  activeConversationMenuId.value = ''
}

function handleDocumentClick(event) {
  if (!(event.target instanceof Element)) {
    closeMenus()
    return
  }

  if (!event.target.closest('.avatar-menu')) {
    avatarMenuOpen.value = false
  }

  if (!event.target.closest('.conversation-action-menu')) {
    activeConversationMenuId.value = ''
  }
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

function toggleAvatarMenu() {
  avatarMenuOpen.value = !avatarMenuOpen.value
  activeConversationMenuId.value = ''
}

function toggleConversationMenu(id) {
  const normalizedId = String(id)
  activeConversationMenuId.value =
    activeConversationMenuId.value === normalizedId ? '' : normalizedId
  avatarMenuOpen.value = false
}

async function handleCreateConversation() {
  try {
    const createdConversation = await createConversation()
    if (!createdConversation) {
      showToast('error', '新建会话失败，请稍后重试。')
      return
    }

    showToast('success', '新会话已创建。')
  } catch (error) {
    showToast('error', getErrorMessage(error, '新建会话失败，请稍后重试。'))
  }
}

function handleDeleteConversation(id) {
  closeMenus()
  const targetConversation = conversations.value.find((item) => item.id === id)
  if (!targetConversation) {
    showToast('error', '未找到要删除的会话。')
    return
  }

  pendingDeleteConversation.value = targetConversation
  deleteDialogVisible.value = true
}

function closeDeleteDialog() {
  if (isDeletingPendingConversation.value) return

  deleteDialogVisible.value = false
  pendingDeleteConversation.value = null
}

function openRenameDialog(id) {
  closeMenus()
  const targetConversation = conversations.value.find((item) => item.id === id)
  if (!targetConversation) {
    showToast('error', '未找到要重命名的会话。')
    return
  }

  pendingRenameConversation.value = targetConversation
  renameDraft.value = targetConversation.title
  renameDialogVisible.value = true
}

function closeRenameDialog() {
  if (isRenamingPendingConversation.value) return

  renameDialogVisible.value = false
  pendingRenameConversation.value = null
  renameDraft.value = ''
}

async function confirmRenameConversation() {
  const targetConversationId = pendingRenameConversation.value?.id
  const nextTitle = renameDraft.value.trim()

  if (!targetConversationId) {
    closeRenameDialog()
    return
  }

  if (!nextTitle) {
    showToast('error', '请输入新的会话标题。')
    return
  }

  try {
    const isRenamed = await renameConversation(targetConversationId, nextTitle)
    if (!isRenamed) {
      showToast('error', '重命名失败，请稍后重试。')
      return
    }

    closeRenameDialog()
    showToast('success', '会话名称已更新。')
  } catch (error) {
    showToast('error', getErrorMessage(error, '重命名失败，请稍后重试。'))
  }
}

async function confirmDeleteConversation() {
  const targetConversationId = pendingDeleteConversation.value?.id
  if (!targetConversationId) {
    closeDeleteDialog()
    return
  }

  try {
    const isDeleted = await deleteConversation(targetConversationId)
    if (!isDeleted) {
      showToast('error', '删除会话失败，请稍后重试。')
      return
    }

    closeDeleteDialog()
    showToast('success', '聊天记录已删除。')
  } catch (error) {
    showToast('error', getErrorMessage(error, '删除会话失败，请稍后重试。'))
  }
}

async function handleSelectConversation(id) {
  try {
    closeMenus()
    await selectConversation(id)
  } catch (error) {
    showToast('error', getErrorMessage(error, '会话内容加载失败，请稍后重试。'))
  }
}

function toggleSidebar() {
  sidebarOpen.value = !sidebarOpen.value
}

async function handleLogout() {
  closeMenus()
  logout()
  await router.push('/login')
}

onMounted(() => {
  document.addEventListener('click', handleDocumentClick)
  if (state.isAuthenticated) {
    initializeChatWorkspace()
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocumentClick)
})
</script>

<template>
  <main class="app-shell workspace">
    <transition name="toast-fade">
      <div v-if="toast.visible" class="toast" :class="toast.type">
        {{ toast.text }}
      </div>
    </transition>

    <transition name="dialog-fade">
      <div
        v-if="deleteDialogVisible"
        class="confirm-dialog-overlay"
        @click.self="closeDeleteDialog"
      >
        <div class="confirm-dialog">
          <p class="confirm-dialog-kicker">删除确认</p>
          <h3>确认删除这个聊天会话吗？</h3>
          <p class="confirm-dialog-copy">
            删除后将无法恢复：
            <strong>{{ pendingDeleteConversationTitle }}</strong>
          </p>
          <div class="confirm-dialog-actions">
            <button
              type="button"
              class="confirm-dialog-button secondary"
              :disabled="isDeletingPendingConversation"
              @click="closeDeleteDialog"
            >
              取消
            </button>
            <button
              type="button"
              class="confirm-dialog-button danger"
              :disabled="isDeletingPendingConversation"
              @click="confirmDeleteConversation"
            >
              {{ isDeletingPendingConversation ? '删除中...' : '确认删除' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <transition name="dialog-fade">
      <div
        v-if="renameDialogVisible"
        class="confirm-dialog-overlay"
        @click.self="closeRenameDialog"
      >
        <div class="confirm-dialog">
          <p class="confirm-dialog-kicker">重命名会话</p>
          <h3>给这个聊天会话换个名字</h3>
          <p class="confirm-dialog-copy">
            当前会话：
            <strong>{{ pendingRenameConversationTitle }}</strong>
          </p>
          <label class="dialog-field">
            <span>新的会话标题</span>
            <input
              v-model="renameDraft"
              type="text"
              maxlength="50"
              placeholder="请输入新的会话标题"
              @keydown.enter.prevent="confirmRenameConversation"
            />
          </label>
          <div class="confirm-dialog-actions">
            <button
              type="button"
              class="confirm-dialog-button secondary"
              :disabled="isRenamingPendingConversation"
              @click="closeRenameDialog"
            >
              取消
            </button>
            <button
              type="button"
              class="confirm-dialog-button primary"
              :disabled="isRenamingPendingConversation"
              @click="confirmRenameConversation"
            >
              {{ isRenamingPendingConversation ? '保存中...' : '保存名称' }}
            </button>
          </div>
        </div>
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
          :disabled="isCreatingConversation"
          @click="handleCreateConversation"
        >
          {{ isCreatingConversation ? '创建中...' : '新建对话' }}
        </button>

        <div v-if="sidebarOpen" class="conversation-pane">
          <p class="sidebar-title">聊天记录</p>
          <div class="conversation-scroll">
            <div
              v-for="item in conversations"
              :key="item.id"
              class="conversation-item"
              :class="{
                active: currentConversationId === item.id,
                'menu-open': activeConversationMenuId === item.id,
              }"
            >
              <button
                type="button"
                class="conversation-main"
                @click="handleSelectConversation(item.id)"
              >
                <strong>{{ item.title }}</strong>
              </button>

              <div
                class="conversation-action-menu"
                :class="{ open: activeConversationMenuId === item.id }"
              >
                <button
                  type="button"
                  class="conversation-menu-button"
                  :aria-label="`打开${item.title}更多功能`"
                  :title="`打开${item.title}更多功能`"
                  @click.stop="toggleConversationMenu(item.id)"
                >
                  <img
                    class="conversation-menu-icon"
                    :src="conversationMoreIcon"
                    alt=""
                    aria-hidden="true"
                  />
                </button>

                <div class="conversation-menu-popover">
                  <button
                    type="button"
                    class="conversation-menu-item"
                    :disabled="renamingConversationId === item.id"
                    @click.stop="openRenameDialog(item.id)"
                  >
                    {{ renamingConversationId === item.id ? '处理中...' : '重命名' }}
                  </button>
                  <button
                    type="button"
                    class="conversation-menu-item danger"
                    :disabled="deletingConversationId === item.id"
                    @click.stop="handleDeleteConversation(item.id)"
                  >
                    {{ deletingConversationId === item.id ? '删除中...' : '删除' }}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="sidebar-footer">
          <div class="footer-mini-card" :class="{ compact: !sidebarOpen }">
            <div class="avatar-menu" :class="{ open: avatarMenuOpen }">
              <button
                type="button"
                class="footer-avatar-button"
                aria-label="打开个人菜单"
                title="个人菜单"
                @click.stop="toggleAvatarMenu"
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
                <button type="button" class="avatar-menu-item" @click.stop="closeMenus">
                  个人信息
                </button>
                <button type="button" class="avatar-menu-item danger" @click.stop="handleLogout">
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
