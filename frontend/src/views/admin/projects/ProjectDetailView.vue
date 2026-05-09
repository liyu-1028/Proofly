<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import { ApiError } from '@/api/http'
import * as projectApi from '@/api/projects'
import { listUsers, type UserResponse } from '@/api/users'
import { useSessionStore } from '@/stores/session'
import type { ProjectPayload, ProjectResponse, ProjectStatus } from '@/types/project'

const statusLabels: Record<ProjectStatus, string> = {
  draft: '草稿',
  waiting_feedback: '待客户反馈',
  change_requested: '需修改',
  waiting_confirm: '待确认',
  confirmed: '已确认',
  archived: '已归档',
}

const route = useRoute()
const router = useRouter()
const session = useSessionStore()

const project = ref<ProjectResponse | null>(null)
const users = ref<UserResponse[]>([])
const loading = ref(false)
const saving = ref(false)
const errorMessage = ref('')
const editMode = ref(false)

const projectId = computed(() => Number(route.params.projectId))
const designerUsers = computed(() => users.value.filter((user) => user.roles.includes('designer')))

const form = reactive({
  name: '',
  customerName: '',
  customerContact: '',
  ownerUserId: '',
  remark: '',
})

function token() {
  if (!session.accessToken) {
    throw new Error('缺少访问令牌')
  }
  return session.accessToken
}

function fillForm(data: ProjectResponse) {
  form.name = data.name
  form.customerName = data.customerName ?? ''
  form.customerContact = data.customerContact ?? ''
  form.ownerUserId = String(data.ownerUserId)
  form.remark = data.remark ?? ''
}

async function loadData() {
  loading.value = true
  errorMessage.value = ''
  try {
    const [projectData, userData] = await Promise.all([
      projectApi.getProject(token(), projectId.value),
      listUsers(token()).catch(() => []),
    ])
    project.value = projectData
    users.value = userData
    fillForm(projectData)
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : '项目加载失败'
  } finally {
    loading.value = false
  }
}

function toPayload(): ProjectPayload {
  return {
    name: form.name.trim(),
    customerName: form.customerName.trim() || undefined,
    customerContact: form.customerContact.trim() || undefined,
    ownerUserId: Number(form.ownerUserId),
    remark: form.remark.trim() || undefined,
  }
}

async function submitEdit() {
  if (!project.value) {
    return
  }
  if (!form.name.trim() || !form.ownerUserId) {
    errorMessage.value = '请填写项目名称并选择负责人'
    return
  }
  saving.value = true
  errorMessage.value = ''
  try {
    project.value = await projectApi.updateProject(token(), project.value.id, toPayload())
    fillForm(project.value)
    editMode.value = false
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : '项目保存失败'
  } finally {
    saving.value = false
  }
}

async function archiveOrRestore() {
  if (!project.value) {
    return
  }
  saving.value = true
  errorMessage.value = ''
  try {
    project.value =
      project.value.status === 'archived'
        ? await projectApi.restoreProject(token(), project.value.id)
        : await projectApi.archiveProject(token(), project.value.id)
    fillForm(project.value)
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : '项目状态更新失败'
  } finally {
    saving.value = false
  }
}

function cancelEdit() {
  if (project.value) {
    fillForm(project.value)
  }
  editMode.value = false
  errorMessage.value = ''
}

function formatTime(value: string | null) {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString()
}

onMounted(() => {
  if (!Number.isFinite(projectId.value)) {
    router.replace('/admin/projects')
    return
  }
  void loadData()
})
</script>

<template>
  <section class="page">
    <header class="page-header">
      <div>
        <p class="breadcrumb"><RouterLink to="/admin/projects">审稿项目</RouterLink> / 项目详情</p>
        <h1 class="page-title">{{ project?.name ?? '项目详情' }}</h1>
        <p class="page-subtitle">查看项目客户信息、负责人和当前状态。</p>
      </div>
      <div class="action-row">
        <button class="secondary-button" type="button" @click="editMode = true" :disabled="!project || project.status === 'archived'">
          编辑
        </button>
        <button class="secondary-button" type="button" @click="archiveOrRestore" :disabled="!project || saving">
          {{ project?.status === 'archived' ? '恢复项目' : '归档项目' }}
        </button>
      </div>
    </header>

    <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>

    <div v-if="loading" class="panel">
      <div class="panel-body">正在加载项目...</div>
    </div>

    <template v-else-if="project">
      <div class="detail-grid">
        <div class="panel">
          <div class="panel-body">
            <div class="section-header">
              <div>
                <h2>项目信息</h2>
                <p>项目基础资料和客户联系方式。</p>
              </div>
              <span class="status-pill" :class="`status-${project.status}`">{{ statusLabels[project.status] }}</span>
            </div>

            <form v-if="editMode" class="form-grid" @submit.prevent="submitEdit">
              <label class="field">
                <span>项目名称</span>
                <input v-model="form.name" />
              </label>
              <label class="field">
                <span>负责人</span>
                <select v-model="form.ownerUserId">
                  <option value="">请选择负责人</option>
                  <option v-for="user in designerUsers" :key="user.userId" :value="String(user.userId)">
                    {{ user.nickname || user.username }}
                  </option>
                </select>
                <small v-if="designerUsers.length === 0" class="field-help">当前没有可选设计师，请先创建设计师账号。</small>
              </label>
              <label class="field">
                <span>客户名称</span>
                <input v-model="form.customerName" />
              </label>
              <label class="field">
                <span>客户联系方式</span>
                <input v-model="form.customerContact" />
              </label>
              <label class="field field-wide">
                <span>备注</span>
                <textarea v-model="form.remark" rows="4" />
              </label>
              <div class="form-actions field-wide">
                <button class="secondary-button" type="button" @click="cancelEdit">取消</button>
                <button class="primary-button" type="submit" :disabled="saving">{{ saving ? '保存中' : '保存' }}</button>
              </div>
            </form>

            <dl v-else class="detail-list">
              <div>
                <dt>客户名称</dt>
                <dd>{{ project.customerName || '-' }}</dd>
              </div>
              <div>
                <dt>客户联系方式</dt>
                <dd>{{ project.customerContact || '-' }}</dd>
              </div>
              <div>
                <dt>负责人</dt>
                <dd>{{ project.ownerNickname || `用户 ${project.ownerUserId}` }}</dd>
              </div>
              <div>
                <dt>创建人</dt>
                <dd>{{ project.createdByNickname || '-' }}</dd>
              </div>
              <div class="field-wide">
                <dt>备注</dt>
                <dd>{{ project.remark || '-' }}</dd>
              </div>
            </dl>
          </div>
        </div>

        <div class="panel">
          <div class="panel-body">
            <div class="section-header">
              <div>
                <h2>版本与确认</h2>
                <p>版本、标注和确认将在后续模块接入。</p>
              </div>
            </div>
            <dl class="detail-list">
              <div>
                <dt>当前版本</dt>
                <dd>{{ project.currentVersionId ?? '-' }}</dd>
              </div>
              <div>
                <dt>确认版本</dt>
                <dd>{{ project.confirmedVersionId ?? '-' }}</dd>
              </div>
              <div>
                <dt>创建时间</dt>
                <dd>{{ formatTime(project.createdAt) }}</dd>
              </div>
              <div>
                <dt>更新时间</dt>
                <dd>{{ formatTime(project.updatedAt) }}</dd>
              </div>
              <div>
                <dt>归档时间</dt>
                <dd>{{ formatTime(project.archivedAt) }}</dd>
              </div>
            </dl>
          </div>
        </div>
      </div>
    </template>
  </section>
</template>
