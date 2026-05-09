<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ElMessage } from 'element-plus'

import { ApiError } from '@/api/http'
import * as projectApi from '@/api/projects'
import { getUsers, type UserResponse } from '@/api/admin'
import { useSessionStore } from '@/stores/session'
import type { ProjectListQuery, ProjectPayload, ProjectResponse, ProjectStatus } from '@/types/project'

const statusOptions: Array<{ value: ProjectStatus | ''; label: string }> = [
  { value: '', label: '全部状态' },
  { value: 'draft', label: '草稿' },
  { value: 'waiting_feedback', label: '待客户反馈' },
  { value: 'change_requested', label: '需修改' },
  { value: 'waiting_confirm', label: '待确认' },
  { value: 'confirmed', label: '已确认' },
  { value: 'archived', label: '已归档' },
]

const statusLabels: Record<ProjectStatus, string> = {
  draft: '草稿',
  waiting_feedback: '待客户反馈',
  change_requested: '需修改',
  waiting_confirm: '待确认',
  confirmed: '已确认',
  archived: '已归档',
}

const session = useSessionStore()

const projects = ref<ProjectResponse[]>([])
const users = ref<UserResponse[]>([])
const loading = ref(false)
const saving = ref(false)
const showForm = ref(false)
const editingProject = ref<ProjectResponse | null>(null)
const errorMessage = ref('')

const filters = reactive<ProjectListQuery>({
  keyword: '',
  status: '',
  ownerUserId: '',
})

const form = reactive({
  name: '',
  customerName: '',
  customerContact: '',
  ownerUserId: '',
  remark: '',
})

const metrics = computed(() => ({
  total: projects.value.length,
  waiting: projects.value.filter((project) => project.status === 'waiting_feedback').length,
  changing: projects.value.filter((project) => project.status === 'change_requested').length,
  confirmed: projects.value.filter((project) => project.status === 'confirmed').length,
}))

const designerUsers = computed(() => users.value.filter((user) => user.roles.includes('designer') || user.roles.includes('owner')))

function resetForm() {
  form.name = ''
  form.customerName = ''
  form.customerContact = ''
  form.ownerUserId = (session.user?.roles.includes('designer') || session.user?.roles.includes('owner')) && session.user.userId ? String(session.user.userId) : ''
  form.remark = ''
}

function openCreateForm() {
  editingProject.value = null
  resetForm()
  showForm.value = true
  errorMessage.value = ''
}

function openEditForm(project: ProjectResponse) {
  editingProject.value = project
  form.name = project.name
  form.customerName = project.customerName ?? ''
  form.customerContact = project.customerContact ?? ''
  form.ownerUserId = String(project.ownerUserId)
  form.remark = project.remark ?? ''
  showForm.value = true
  errorMessage.value = ''
}

function closeForm() {
  showForm.value = false
  editingProject.value = null
  resetForm()
}

function toPayload(): ProjectPayload {
  return {
    name: form.name.trim(),
    customerName: form.customerName.trim() || undefined,
    customerContact: form.customerContact.trim() || undefined,
    ownerUserId: form.ownerUserId,
    remark: form.remark.trim() || undefined,
  }
}

async function loadProjects() {
  loading.value = true
  errorMessage.value = ''
  try {
    projects.value = await projectApi.listProjects(filters)
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : '项目列表加载失败'
  } finally {
    loading.value = false
  }
}

async function loadUsers() {
  try {
    users.value = await getUsers()
  } catch {
    if (session.user?.roles.includes('designer')) {
      users.value = [
        {
          userId: session.user.userId,
          storeId: session.user.storeId,
          username: session.user.username,
          nickname: session.user.nickname,
          phone: session.user.phone,
          email: '',
          status: 'active' as any,
          roles: session.user.roles,
          lastLoginAt: '',
          createdAt: '',
          updatedAt: '',
        },
      ]
    }
  }
}

async function submitProject() {
  if (!form.name.trim() || !form.ownerUserId) {
    errorMessage.value = '请填写项目名称并选择负责人'
    return
  }

  saving.value = true
  errorMessage.value = ''
  try {
    if (editingProject.value) {
      await projectApi.updateProject(editingProject.value.id, toPayload())
      ElMessage.success('保存成功')
    } else {
      await projectApi.createProject(toPayload())
      ElMessage.success('创建成功')
    }
    closeForm()
    await loadProjects()
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : '项目保存失败'
  } finally {
    saving.value = false
  }
}

async function archiveOrRestore(project: ProjectResponse) {
  saving.value = true
  errorMessage.value = ''
  try {
    if (project.status === 'archived') {
      await projectApi.restoreProject(project.id)
    } else {
      await projectApi.archiveProject(project.id)
    }
    await loadProjects()
    ElMessage.success('操作成功')
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : '项目状态更新失败'
  } finally {
    saving.value = false
  }
}

function formatTime(value: string | null) {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString()
}

onMounted(async () => {
  resetForm()
  await Promise.all([loadProjects(), loadUsers()])
})
</script>

<template>
  <section class="page">
    <header class="page-header">
      <div>
        <h1 class="page-title">审稿项目</h1>
        <p class="page-subtitle">管理项目、客户信息、负责人和项目状态。</p>
      </div>
      <el-button type="primary" @click="openCreateForm">创建项目</el-button>
    </header>

    <div class="metric-grid">
      <div class="metric">
        <span>全部项目</span>
        <strong>{{ metrics.total }}</strong>
      </div>
      <div class="metric">
        <span>待客户反馈</span>
        <strong>{{ metrics.waiting }}</strong>
      </div>
      <div class="metric">
        <span>需修改</span>
        <strong>{{ metrics.changing }}</strong>
      </div>
      <div class="metric">
        <span>已确认</span>
        <strong>{{ metrics.confirmed }}</strong>
      </div>
    </div>

    <div class="panel">
      <div class="panel-body">
        <div class="filter-bar">
          <label class="field">
            <span>搜索</span>
            <input v-model="filters.keyword" placeholder="项目名称或客户名称" @keyup.enter="loadProjects" />
          </label>
          <label class="field">
            <span>状态</span>
            <select v-model="filters.status">
              <option v-for="option in statusOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
            </select>
          </label>
          <label class="field">
            <span>负责人</span>
            <select v-model="filters.ownerUserId">
              <option value="">全部负责人</option>
              <option v-for="user in designerUsers" :key="user.userId" :value="user.userId">{{ user.nickname || user.username }}</option>
            </select>
          </label>
          <div class="filter-actions">
            <el-button type="primary" @click="loadProjects" :loading="loading">筛选</el-button>
          </div>
        </div>

        <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>

        <div class="data-table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th>项目名称</th>
                <th>客户</th>
                <th>负责人</th>
                <th>状态</th>
                <th>更新时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="6">正在加载项目...</td>
              </tr>
              <tr v-else-if="projects.length === 0">
                <td colspan="6">暂无项目</td>
              </tr>
              <tr v-for="project in projects" v-else :key="project.id">
                <td>
                  <RouterLink class="table-link" :to="`/admin/projects/${project.id}`">{{ project.name }}</RouterLink>
                  <small v-if="project.remark" class="table-subtext">{{ project.remark }}</small>
                </td>
                <td>
                  <span>{{ project.customerName || '-' }}</span>
                  <small class="table-subtext">{{ project.customerContact || '' }}</small>
                </td>
                <td>{{ project.ownerNickname || `用户 ${project.ownerUserId}` }}</td>
                <td><span class="status-pill" :class="`status-${project.status}`">{{ statusLabels[project.status] }}</span></td>
                <td>{{ formatTime(project.updatedAt) }}</td>
                <td>
                  <div class="table-actions">
                    <RouterLink class="text-button" :to="`/admin/projects/${project.id}`">查看</RouterLink>
                    <button class="text-button" type="button" @click="openEditForm(project)" :disabled="project.status === 'archived'">编辑</button>
                    <button class="text-button" type="button" @click="archiveOrRestore(project)" :disabled="saving">
                      {{ project.status === 'archived' ? '恢复' : '归档' }}
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <el-dialog v-model="showForm" :title="editingProject ? '编辑项目' : '创建项目'" width="500px">
      <el-form :model="form" label-position="top">
        <el-form-item label="项目名称" required>
          <el-input v-model="form.name" placeholder="例如：门头招牌设计稿" />
        </el-form-item>
        <el-form-item label="负责人" required>
          <el-select v-model="form.ownerUserId" style="width: 100%">
            <el-option v-for="user in designerUsers" :key="user.userId" :label="user.nickname || user.username" :value="String(user.userId)" />
          </el-select>
          <small v-if="designerUsers.length === 0" class="field-help">当前没有可选设计师，请先创建设计师账号。</small>
        </el-form-item>
        <el-form-item label="客户名称">
          <el-input v-model="form.customerName" placeholder="客户或公司名称" />
        </el-form-item>
        <el-form-item label="客户联系方式">
          <el-input v-model="form.customerContact" placeholder="手机号、微信或备注联系方式" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="4" placeholder="项目要求、交付说明或沟通备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeForm">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitProject">确定</el-button>
      </template>
    </el-dialog>
  </section>
</template>
