import { createRouter, createWebHistory } from 'vue-router'
import ChatView from '../views/ChatView.vue'
import LoginView from '../views/LoginView.vue'
import { useWorkspaceStore } from '../stores/workspace'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/login',
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { guestOnly: true },
    },
    {
      path: '/chat',
      name: 'chat',
      component: ChatView,
      meta: { requiresAuth: true },
    },
  ],
})

router.beforeEach((to) => {
  const { state, hydrateSession } = useWorkspaceStore()

  if (!state.initialized) {
    hydrateSession()
  }

  if (to.meta.requiresAuth && !state.isAuthenticated) {
    return { name: 'login' }
  }

  if (to.meta.guestOnly && state.isAuthenticated) {
    return { name: 'chat' }
  }

  return true
})

export default router
