import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router'
import { AUTH_EXPIRED_EVENT } from './api/http'
import { useWorkspaceStore } from './stores/workspace'

const { logout } = useWorkspaceStore()

function handleAuthExpired() {
  logout()

  if (router.currentRoute.value.name !== 'login') {
    router.replace({ name: 'login' })
  }
}

window.addEventListener(AUTH_EXPIRED_EVENT, handleAuthExpired)

createApp(App).use(router).mount('#app')
