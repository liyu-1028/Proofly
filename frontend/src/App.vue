<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { RouterLink, RouterView } from 'vue-router'
import { useRoute, useRouter } from 'vue-router'

import prooflyLogo from '@/assets/proofly-logo.svg'
import { useSessionStore } from '@/stores/session'
import { useNotificationStore } from '@/stores/notification'
import NotificationCenter from '@/components/NotificationCenter.vue'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()
const notificationStore = useNotificationStore()

const showNotifications = ref(false)
const useBareLayout = computed(() => route.meta.public || route.name === 'login')

async function handleLogout() {
  await session.logout()
  await router.replace('/login')
}

onMounted(() => {
  if (session.isAuthenticated) {
    notificationStore.fetchUnreadCount()
    notificationStore.connectWebSocket()
  }
})
</script>

<template>
  <RouterView v-if="useBareLayout" />

  <div v-else class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <img class="brand-logo" :src="prooflyLogo" alt="审稿宝 Logo" />
        <div>
          <strong>审稿宝</strong>
          <span>Proofly</span>
        </div>
      </div>

      <nav class="nav-list" aria-label="主导航">
        <RouterLink to="/admin/dashboard">工作台</RouterLink>
        <RouterLink to="/admin/projects">审稿项目</RouterLink>
        <RouterLink v-if="session.canManageStaff" to="/admin/staff">员工管理</RouterLink>
        <RouterLink to="/admin/referral">推荐奖励</RouterLink>
        <RouterLink to="/admin/settings">系统设置</RouterLink>
      </nav>

      <div class="sidebar-user">
        <span>{{ session.displayName }}</span>
        <small>门店 ID：{{ session.user?.storeId ?? '-' }}</small>
        <button type="button" @click="handleLogout">退出登录</button>
      </div>
    </aside>

    <main class="main-panel">
      <RouterView />
    </main>

    <NotificationCenter v-model:visible="showNotifications" />
  </div>
</template>

<style scoped>
.nav-item-notification {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  color: #c0c4cc;
  text-decoration: none;
  transition: all 0.3s;
}
.nav-item-notification:hover {
  background: rgba(255, 255, 255, 0.05);
  color: #fff;
}
.unread-badge {
  margin-left: 8px;
}
</style>
