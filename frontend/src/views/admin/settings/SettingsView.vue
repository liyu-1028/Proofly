<script setup lang="ts">
import { useSessionStore } from '@/stores/session'

const session = useSessionStore()
</script>

<template>
  <section class="page">
    <header class="page-header">
      <div>
        <h1 class="page-title">系统设置</h1>
        <p class="page-subtitle">配置门店资料、文件上传规则和审稿链接策略。</p>
      </div>
    </header>

    <div class="panel">
      <div class="panel-body">
        <div class="settings-grid">
          <div class="setting-item">
            <span>当前门店 ID</span>
            <strong>{{ session.user?.storeId ?? '-' }}</strong>
          </div>
          <div class="setting-item">
            <span>当前用户</span>
            <strong>{{ session.displayName }}</strong>
          </div>
          <div class="setting-item">
            <span>账号状态</span>
            <strong>{{ session.user?.status ?? '-' }}</strong>
          </div>
        </div>

        <p class="panel-note">门店资料更新接口尚未实现，当前页面先展示登录态和门店隔离信息。</p>
      </div>
    </div>
  </section>
</template>
