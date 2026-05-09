<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import { ApiError } from '@/api/http'
import { createUser, listUsers, type UserResponse } from '@/api/users'
import { useSessionStore } from '@/stores/session'

const roleOptions = [
  { code: 'owner', label: '门店老板' },
  { code: 'designer', label: '设计师' },
  { code: 'admin', label: '管理员' },
]

const statusLabels: Record<string, string> = {
  active: '启用',
  disabled: '停用',
  locked: '锁定',
}

const session = useSessionStore()

const users = ref<UserResponse[]>([])
const loading = ref(false)
const saving = ref(false)
const showCreateForm = ref(false)
const errorMessage = ref('')

const form = reactive({
  username: '',
  nickname: '',
  phone: '',
  email: '',
  password: '',
  roleCodes: ['designer'],
})

const canCreateStaff = computed(() => session.isAdmin || session.canManageStaff)

function token() {
  if (!session.accessToken) {
    throw new Error('缺少访问令牌')
  }
  return session.accessToken
}

function resetForm() {
  form.username = ''
  form.nickname = ''
  form.phone = ''
  form.email = ''
  form.password = ''
  form.roleCodes = ['designer']
}

async function loadUsers() {
  if (!canCreateStaff.value) {
    return
  }

  loading.value = true
  errorMessage.value = ''
  try {
    users.value = await listUsers(token(), '', '')
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : '员工列表加载失败'
  } finally {
    loading.value = false
  }
}

function openCreateForm() {
  resetForm()
  showCreateForm.value = true
  errorMessage.value = ''
}

function closeCreateForm() {
  showCreateForm.value = false
  resetForm()
}

async function submitCreate() {
  if (!form.username.trim() || !form.nickname.trim() || !form.password.trim() || form.roleCodes.length === 0) {
    errorMessage.value = '请填写账号、昵称、初始密码并选择角色'
    return
  }

  saving.value = true
  errorMessage.value = ''
  try {
    await createUser(token(), {
      username: form.username.trim(),
      nickname: form.nickname.trim(),
      phone: form.phone.trim() || undefined,
      email: form.email.trim() || undefined,
      password: form.password,
      roleCodes: form.roleCodes,
    })
    closeCreateForm()
    await loadUsers()
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : '员工创建失败'
  } finally {
    saving.value = false
  }
}

function roleText(roles: string[]) {
  return roles
    .map((role) => roleOptions.find((option) => option.code === role)?.label ?? role)
    .join('、')
}

function statusText(status: string) {
  return statusLabels[status] ?? status
}

onMounted(() => {
  void loadUsers()
})
</script>

<template>
  <section class="page">
    <header class="page-header">
      <div>
        <h1 class="page-title">员工管理</h1>
        <p class="page-subtitle">管理门店老板、设计师和管理员账号。</p>
      </div>
      <button v-if="canCreateStaff" class="primary-button" type="button" @click="openCreateForm">创建员工</button>
    </header>

    <div class="panel">
      <div class="panel-body">
        <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>

        <div v-if="!canCreateStaff" class="empty-state">
          当前账号没有员工管理权限。
        </div>

        <div v-else class="data-table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th>姓名</th>
                <th>账号</th>
                <th>手机号</th>
                <th>邮箱</th>
                <th>角色</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="6">正在加载员工...</td>
              </tr>
              <tr v-else-if="users.length === 0">
                <td colspan="6">暂无员工</td>
              </tr>
              <tr v-for="staff in users" v-else :key="staff.userId">
                <td>{{ staff.nickname }}</td>
                <td>{{ staff.username }}</td>
                <td>{{ staff.phone || '-' }}</td>
                <td>{{ staff.email || '-' }}</td>
                <td>{{ roleText(staff.roles) || '-' }}</td>
                <td><span class="status-pill">{{ statusText(staff.status) }}</span></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <div v-if="showCreateForm" class="modal-backdrop" @click.self="closeCreateForm">
      <form class="modal-panel" @submit.prevent="submitCreate">
        <div class="section-header">
          <div>
            <h2>创建员工</h2>
            <p>管理员可以创建门店老板、设计师或管理员账号。</p>
          </div>
          <button class="icon-text-button" type="button" @click="closeCreateForm">关闭</button>
        </div>

        <div class="form-grid">
          <label class="field">
            <span>账号</span>
            <input v-model="form.username" placeholder="登录账号" />
          </label>
          <label class="field">
            <span>昵称</span>
            <input v-model="form.nickname" placeholder="员工姓名或昵称" />
          </label>
          <label class="field">
            <span>手机号</span>
            <input v-model="form.phone" placeholder="手机号" />
          </label>
          <label class="field">
            <span>邮箱</span>
            <input v-model="form.email" placeholder="邮箱" />
          </label>
          <label class="field">
            <span>初始密码</span>
            <input v-model="form.password" type="password" placeholder="至少 6 位" />
          </label>
          <div class="field">
            <span>角色</span>
            <label v-for="role in roleOptions" :key="role.code" class="check-row">
              <input v-model="form.roleCodes" type="checkbox" :value="role.code" />
              {{ role.label }}
            </label>
          </div>
        </div>

        <div class="form-actions">
          <button class="secondary-button" type="button" @click="closeCreateForm">取消</button>
          <button class="primary-button" type="submit" :disabled="saving">{{ saving ? '创建中' : '创建员工' }}</button>
        </div>
      </form>
    </div>
  </section>
</template>
