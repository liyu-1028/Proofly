<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <div class="app-shell">
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
        <RouterLink to="/admin/staff">员工管理</RouterLink>
        <RouterLink to="/admin/settings">系统设置</RouterLink>
        <RouterLink to="/review/demo-token">客户审稿页</RouterLink>
      </nav>
    </aside>

    <main class="main-panel">
      <RouterView />
    </main>
  </div>
</template>
