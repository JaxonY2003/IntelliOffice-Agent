<script setup>
import { computed, reactive, ref } from 'vue'

const roleOptions = [
  {
    key: 'admin',
    label: '管理员',
    heading: '管理员登录',
    description: '管理知识库、日志记录与系统配置。',
    accent: 'blue',
  },
  {
    key: 'employee',
    label: '员工',
    heading: '员工登录',
    description: '查询制度、发起问答、查看个人业务进度。',
    accent: 'pink',
  },
]

const mockAccounts = {
  employee: {
    username: 'employee01',
    password: '123456',
    displayName: '张晓宁',
    roleName: '普通员工',
  },
  admin: {
    username: 'admin01',
    password: 'admin123',
    displayName: '林知远',
    roleName: '系统管理员',
  },
}

const form = reactive({
  role: 'employee',
  username: '',
  password: '',
  remember: true,
})

const isSubmitting = ref(false)
const toast = ref({
  visible: false,
  type: 'success',
  text: '',
})

let toastTimer = null

const activeRole = computed(() =>
  roleOptions.find((item) => item.key === form.role) ?? roleOptions[0],
)

function fillDemoAccount() {
  const account = mockAccounts[form.role]
  form.username = account.username
  form.password = account.password
}

function showToast(type, text) {
  if (toastTimer) {
    clearTimeout(toastTimer)
  }

  toast.value = {
    visible: true,
    type,
    text,
  }

  toastTimer = setTimeout(() => {
    toast.value.visible = false
  }, 2600)
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
    await new Promise((resolve) => setTimeout(resolve, 800))

    const account = mockAccounts[form.role]
    const isValid =
      form.username === account.username && form.password === account.password

    if (!isValid) {
      showToast('error', '账号、密码或角色不匹配，请检查后重试。')
      return
    }

    const fakeToken = `mock-${form.role}-token`

    localStorage.setItem('token', fakeToken)
    localStorage.setItem('role', form.role)
    localStorage.setItem(
      'userProfile',
      JSON.stringify({
        name: account.displayName,
        roleName: account.roleName,
      }),
    )

    showToast('success', `${account.roleName}登录成功，已完成本地模拟鉴权。`)
  } finally {
    isSubmitting.value = false
  }
}

fillDemoAccount()
</script>

<template>
  <main class="login-shell">
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
          <p>区分员工和管理员入口，为后续日志与安全控制预留完整空间。</p>
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
                <p>管理员：`admin01 / admin123`</p>
              </div>

              <button type="button" class="ghost-button" @click="fillDemoAccount">
                填入账号
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
