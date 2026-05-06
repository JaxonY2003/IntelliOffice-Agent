<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { loginWithPassword } from '../api/auth'
import { mockAccounts, roleOptions } from '../data/mockWorkspace'
import { useWorkspaceStore } from '../stores/workspace'

const router = useRouter()
const { login } = useWorkspaceStore()

const form = reactive({
  role: 'employee',
  username: '',
  password: '',
  remember: true,
})

const toast = ref({
  visible: false,
  type: 'success',
  text: '',
})

const isSubmitting = ref(false)
let toastTimer = null

const activeRole = computed(() =>
  roleOptions.find((item) => item.key === form.role) ?? roleOptions[0],
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

function fillDemoAccount() {
  const account = mockAccounts[form.role]
  form.username = account.username
  form.password = account.password
}

function handleRoleChange(role) {
  if (form.role === role) return

  form.role = role
  fillDemoAccount()
}

function validateForm() {
  if (!form.username.trim()) {
    showToast('error', '请输入用户名后再继续登录。')
    return false
  }

  if (!form.password.trim()) {
    showToast('error', '请输入密码后再继续登录。')
    return false
  }

  return true
}

async function handleSubmit() {
  if (!validateForm()) return

  isSubmitting.value = true

  try {
    const payload = {
      type: form.role,
      username: form.username.trim(),
      password: form.password,
    }
    const authData = await loginWithPassword(payload)

    login({
      token: authData.token,
      tokenType: authData.tokenType,
      type: authData.type ?? payload.type,
      username: authData.username ?? payload.username,
    })

    showToast('success', `${activeRole.value.label}登录成功，正在进入对话工作台。`)
    await router.push('/chat')
  } catch (error) {
    showToast(
      'error',
      error instanceof Error ? error.message : '登录失败，请稍后重试。',
    )
  } finally {
    isSubmitting.value = false
  }
}

fillDemoAccount()
</script>

<template>
  <main class="app-shell">
    <transition name="toast-fade">
      <div v-if="toast.visible" class="toast" :class="toast.type">
        {{ toast.text }}
      </div>
    </transition>

    <section class="hero-panel">
      <div class="hero-badge">Enterprise AI Workspace</div>
      <h1>企业内部智能知识助手</h1>
      <p class="hero-copy">
        面向真实办公场景的企业级 AI 助手，整合知识问答、业务查询、文档管理与权限控制，让信息获取和流程处理更高效。
      </p>

      <div class="hero-grid">
        <article class="feature-card">
          <span class="feature-index">01</span>
          <h2>知识库问答</h2>
          <p>基于企业文档检索生成回答，减少人工查资料时间。</p>
        </article>
        <article class="feature-card">
          <span class="feature-index">02</span>
          <h2>Agent 工具调用</h2>
          <p>连接报销、请假、员工信息等业务能力，支持自然语言触发。</p>
        </article>
        <article class="feature-card">
          <span class="feature-index">03</span>
          <h2>权限与审计</h2>
          <p>区分员工、部门管理者和管理员入口，为后续日志与安全控制预留完整空间。</p>
        </article>
      </div>
    </section>

    <section class="login-panel">
      <div class="login-card" :class="`theme-${activeRole.accent}`">
        <div class="role-tabs" aria-label="选择登录角色">
          <button
            v-for="role in roleOptions"
            :key="role.key"
            type="button"
            class="role-tab"
            :class="{ active: form.role === role.key, inactive: form.role !== role.key }"
            @click="handleRoleChange(role.key)"
          >
            {{ role.label }}
          </button>
        </div>

        <div class="card-body">
          <div class="card-header">
            <p class="eyebrow">Secure Sign In</p>
            <h2>{{ activeRole.heading }}</h2>
            <p class="card-copy">{{ activeRole.description }}</p>
            <div class="role-meta">
              <span class="meta-pill primary">{{ activeRole.label }}入口</span>
              <span class="meta-pill">企业内部统一认证</span>
            </div>
          </div>

          <form class="login-form" @submit.prevent="handleSubmit">
            <label class="field">
              <span>用户名</span>
              <input
                v-model="form.username"
                type="text"
                placeholder="请输入用户名"
                autocomplete="username"
              />
            </label>

            <label class="field">
              <span>密码</span>
              <input
                v-model="form.password"
                type="password"
                placeholder="请输入密码"
                autocomplete="current-password"
              />
            </label>

            <label class="remember-row">
              <input v-model="form.remember" type="checkbox" />
              <span>记住当前设备的登录状态</span>
            </label>

            <div class="helper-row">
              <div class="demo-tip">
                <p>员工：`employee01 / 123456`</p>
                <p>部门管理者：`manager01 / manager123`</p>
                <p>管理员：`admin01 / admin123`</p>
              </div>

              <button type="button" class="ghost-button" @click="fillDemoAccount">
                填入演示账号
              </button>
            </div>

            <button class="submit-button" type="submit" :disabled="isSubmitting">
              {{ isSubmitting ? '正在验证身份...' : `以${activeRole.label}身份登录` }}
            </button>
          </form>
        </div>
      </div>
    </section>
  </main>
</template>
