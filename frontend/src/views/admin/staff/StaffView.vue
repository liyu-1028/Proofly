<script setup lang="ts">
import { computed } from 'vue'

import { useSessionStore } from '@/stores/session'

const session = useSessionStore()

const currentStaff = computed(() => {
  if (!session.user) {
    return []
  }

  return [
    {
      id: session.user.userId,
      name: session.user.nickname || session.user.username,
      username: session.user.username,
      phone: session.user.phone || '-',
      roles: session.user.roles,
      status: session.user.status,
    },
  ]
})
</script>

<template>
  <section class="page">
    <header class="page-header">
      <div>
        <h1 class="page-title">员工管理</h1>
        <p class="page-subtitle">管理门店老板、设计师和管理员账号。</p>
      </div>
    </header>

    <div class="panel">
      <div class="panel-body">
        <div class="section-header">
          <div>
            <h2>当前账号</h2>
            <p>员工列表接口尚未实现，当前先展示已登录账号。</p>
          </div>
          <button class="secondary-button" type="button" disabled>创建员工</button>
        </div>

        <div class="data-table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th>姓名</th>
                <th>账号</th>
                <th>手机号</th>
                <th>角色</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="staff in currentStaff" :key="staff.id">
                <td>{{ staff.name }}</td>
                <td>{{ staff.username }}</td>
                <td>{{ staff.phone }}</td>
                <td>{{ staff.roles.join('、') || '-' }}</td>
                <td><span class="status-pill">{{ staff.status }}</span></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </section>
</template>
