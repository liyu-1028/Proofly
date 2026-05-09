<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView } from 'vue-router'
import { useRoute, useRouter } from 'vue-router'

import { useSessionStore } from '@/stores/session'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()

const useBareLayout = computed(() => route.meta.public || route.name === 'login')

async function handleLogout() {
  await session.logout()
  await router.replace('/login')
}
</script>

<template>
  <RouterView v-if="useBareLayout" />

  <div v-else class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-mark">审</span>
        <div>
          <strong>审稿宝</strong>
          <span>Proofly</span>
        </div>
      </div>

      <nav class="nav-list" aria-label="主导航">
        <RouterLink to="/admin/dashboard">工作台</RouterLink>
        <RouterLink to="/admin/projects">审稿项目</RouterLink>
        <RouterLink v-if="session.canManageStaff" to="/admin/staff">员工管理</RouterLink>
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
  </div>
</template>
