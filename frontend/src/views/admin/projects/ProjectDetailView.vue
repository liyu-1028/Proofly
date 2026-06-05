<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatLineRound, CircleCheck, Clock, Files, Microphone, Upload, View } from '@element-plus/icons-vue'

import * as annotationApi from '@/api/annotations'
import * as auditApi from '@/api/audit'
import * as confirmationApi from '@/api/confirmations'
import * as configApi from '@/api/configs'
import { ApiError } from '@/api/http'
import * as projectApi from '@/api/projects'
import * as reviewLinkApi from '@/api/review-links'
import * as versionApi from '@/api/version'
import { getUsers, type UserResponse } from '@/api/admin'
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
const versions = ref<versionApi.ProjectVersionResponse[]>([])
const reviewLinks = ref<reviewLinkApi.ReviewLinkResponse[]>([])
const annotations = ref<annotationApi.AnnotationResponse[]>([])
const confirmation = ref<confirmationApi.ConfirmationRecordResponse | null>(null)
const timeline = ref<auditApi.AuditLogResponse[]>([])
const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const creatingReviewLink = ref(false)
const loadingAnnotations = ref(false)
const errorMessage = ref('')
const editMode = ref(false)
const latestCreatedLink = ref<reviewLinkApi.ReviewLinkResponse | null>(null)
const maxFileSizeStr = ref('25MB')

const isAnnotationsExpanded = ref(false)
const annotationCurrentPage = ref(1)
const annotationPageSize = 5
const pagedAnnotations = computed(() => {
  if (!isAnnotationsExpanded.value && annotations.value.length > 5) {
    return annotations.value.slice(0, 5)
  }
  const start = (annotationCurrentPage.value - 1) * annotationPageSize
  return annotations.value.slice(start, start + annotationPageSize)
})

const projectId = computed(() => String(route.params.projectId))
const designerUsers = computed(() => users.value.filter((user: UserResponse) => user.roles.includes('designer') || user.roles.includes('owner')))

const currentVersion = computed(() => versions.value.find(v => v.isCurrent) || versions.value[0])

const form = reactive({
  name: '',
  customerName: '',
  customerContact: '',
  ownerUserId: '',
  remark: '',
})

const reviewLinkForm = reactive({
  expiresAt: '',
  maxAccessCount: undefined as number | undefined,
})

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
    const [projectData, userData, versionData, reviewLinkData, confirmationData, timelineData] = await Promise.all([
      projectApi.getProject(projectId.value),
      getUsers().catch(() => []),
      versionApi.listVersions(projectId.value).catch(() => []),
      reviewLinkApi.listReviewLinks(projectId.value).catch(() => []),
      confirmationApi.getProjectConfirmation(projectId.value).catch(() => null),
      auditApi.getProjectTimeline(projectId.value).catch(() => []),
    ])
    project.value = projectData
    users.value = userData
    versions.value = versionData
    reviewLinks.value = reviewLinkData
    confirmation.value = confirmationData
    timeline.value = timelineData
    fillForm(projectData)
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : '项目加载失败'
  } finally {
    loading.value = false
  }
}

async function loadReviewLinks() {
  reviewLinks.value = await reviewLinkApi.listReviewLinks(projectId.value)
}

async function loadAnnotations(versionId?: string) {
  if (!versionId) {
    annotations.value = []
    annotationCurrentPage.value = 1
    isAnnotationsExpanded.value = false
    return
  }
  loadingAnnotations.value = true
  try {
    annotations.value = await annotationApi.listProjectVersionAnnotations(projectId.value, versionId)
    annotationCurrentPage.value = 1
    isAnnotationsExpanded.value = false
  } catch (error: any) {
    ElMessage.error(error?.message || '标注加载失败')
    annotations.value = []
    annotationCurrentPage.value = 1
    isAnnotationsExpanded.value = false
  } finally {
    loadingAnnotations.value = false
  }
}

async function loadProjectTimeline() {
  timeline.value = await auditApi.getProjectTimeline(projectId.value)
}

function selectVersion(row: versionApi.ProjectVersionResponse) {
  versions.value.forEach(v => {
    v.isCurrent = false
  })
  row.isCurrent = true
  void loadAnnotations(row.id)
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
    const payload: ProjectPayload = {
      name: form.name.trim(),
      customerName: form.customerName.trim() || undefined,
      customerContact: form.customerContact.trim() || undefined,
      ownerUserId: form.ownerUserId,
      remark: form.remark.trim() || undefined,
    }
    project.value = await projectApi.updateProject(project.value.id, payload)
    fillForm(project.value)
    editMode.value = false
    ElMessage.success('保存成功')
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
        ? await projectApi.restoreProject(project.value.id)
        : await projectApi.archiveProject(project.value.id)
    fillForm(project.value)
    ElMessage.success(project.value.status === 'archived' ? '项目已归档' : '项目已恢复')
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : '项目状态更新失败'
  } finally {
    saving.value = false
  }
}

async function handleUpload(options: any) {
  if (!project.value) return

  // 解析 25MB -> bytes
  const match = maxFileSizeStr.value.match(/^(\d+)([a-zA-Z]+)$/)
  let maxSizeInBytes = 25 * 1024 * 1024
  if (match) {
    const value = parseInt(match[1])
    const unit = match[2].toUpperCase()
    if (unit === 'KB') maxSizeInBytes = value * 1024
    else if (unit === 'MB') maxSizeInBytes = value * 1024 * 1024
    else if (unit === 'GB') maxSizeInBytes = value * 1024 * 1024 * 1024
  }

  if (options.file.size > maxSizeInBytes) {
    ElMessage.error(`文件太大了，请上传小于 ${maxFileSizeStr.value} 的文件`)
    return
  }

  uploading.value = true
  try {
    const newVersion = await versionApi.uploadVersion(projectId.value, options.file)
    versions.value = [newVersion, ...versions.value]
    // Refresh project to get updated status and currentVersionId
    const updatedProject = await projectApi.getProject(projectId.value)
    project.value = updatedProject
    await loadProjectTimeline().catch(() => undefined)
    ElMessage.success('新版本上传成功')
  } catch (error: any) {
    ElMessage.error(error.message || '上传失败')
  } finally {
    uploading.value = false
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

function formatSize(bytes: number) {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

function statusType(status: string) {
  if (status === 'active') return 'success'
  if (status === 'disabled') return 'warning'
  return 'info'
}

function statusText(status: string) {
  const map: Record<string, string> = {
    active: '启用',
    disabled: '停用',
    expired: '已过期',
  }
  return map[status] ?? status
}

function annotationStatusText(status: annotationApi.AnnotationStatus) {
  const map: Record<annotationApi.AnnotationStatus, string> = {
    open: '待处理',
    resolved: '已处理',
    ignored: '已忽略',
  }
  return map[status] ?? status
}

function annotationStatusType(status: annotationApi.AnnotationStatus) {
  if (status === 'resolved') return 'success'
  if (status === 'ignored') return 'info'
  return 'warning'
}

function annotationMarkerStyle(annotation: annotationApi.AnnotationResponse) {
  return {
    left: `${Number(annotation.xRatio) * 100}%`,
    top: `${Number(annotation.yRatio) * 100}%`,
  }
}

function operatorText(log: auditApi.AuditLogResponse) {
  if (log.operatorName) return log.operatorName
  if (log.operatorType === 'customer') return '客户'
  if (log.operatorType === 'system') return '系统'
  return '用户'
}

async function updateAnnotationStatus(annotation: annotationApi.AnnotationResponse, status: 'resolved' | 'ignored') {
  if (!currentVersion.value) return
  try {
    await annotationApi.updateAnnotationStatus(projectId.value, currentVersion.value.id, annotation.id, status)
    annotation.status = status
    annotation.resolvedAt = new Date().toISOString()
    ElMessage.success(status === 'resolved' ? '已标记处理完成' : '已忽略该意见')
    await loadProjectTimeline().catch(() => undefined)
  } catch (error: any) {
    ElMessage.error(error?.message || '标注状态更新失败')
  }
}

async function copyText(text?: string | null) {
  if (!text) {
    ElMessage.warning('该链接没有可复制的完整地址，请重新生成')
    return
  }
  await navigator.clipboard.writeText(text)
  ElMessage.success('审稿链接已复制')
}

async function copyReminderText(url: string) {
  const text = `您好，项目 [${project.value?.name}] 的设计稿已更新，请点击链接查阅并提出修改意见：${url}`
  await navigator.clipboard.writeText(text)
  ElMessage.success('提醒文案已复制，可直接粘贴发送给客户')
}

async function createReviewLink() {
  creatingReviewLink.value = true
  try {
    const created = await reviewLinkApi.createReviewLink(projectId.value, {
      expiresAt: reviewLinkForm.expiresAt || undefined,
      maxAccessCount: reviewLinkForm.maxAccessCount,
    })
    latestCreatedLink.value = created
    reviewLinkForm.expiresAt = ''
    reviewLinkForm.maxAccessCount = undefined
    await loadReviewLinks()
    await copyText(created.url)
  } catch (error: any) {
    ElMessage.error(error?.message || '生成审稿链接失败')
  } finally {
    creatingReviewLink.value = false
  }
}

async function toggleReviewLink(link: reviewLinkApi.ReviewLinkResponse) {
  try {
    if (link.status === 'disabled') {
      await reviewLinkApi.enableReviewLink(link.id)
      ElMessage.success('审稿链接已启用')
    } else {
      await reviewLinkApi.disableReviewLink(link.id)
      ElMessage.success('审稿链接已禁用')
    }
    await loadReviewLinks()
  } catch (error: any) {
    ElMessage.error(error?.message || '审稿链接状态更新失败')
  }
}

async function removeReviewLink(link: reviewLinkApi.ReviewLinkResponse) {
  try {
    await reviewLinkApi.deleteReviewLink(link.id)
    ElMessage.success('审稿链接已删除')
    await loadReviewLinks()
  } catch (error: any) {
    ElMessage.error(error?.message || '删除审稿链接失败')
  }
}

function playVoice(url: string) {
  const audio = new Audio(url)
  audio.play().catch((err) => {
    ElMessage.error('无法播放语音：' + err.message)
  })
}

onMounted(() => {
  if (!projectId.value || projectId.value === 'undefined') {
    router.replace('/admin/projects')
    return
  }
  void loadData()
  void configApi.getUploadLimits().then(res => {
    maxFileSizeStr.value = res.maxFileSize
  })
})

watch(
  () => currentVersion.value?.id,
  versionId => {
    void loadAnnotations(versionId)
  },
)
</script>

<template>
  <section class="page">
    <header class="page-header">
      <div>
        <p class="breadcrumb"><RouterLink to="/admin/projects">审稿宝</RouterLink> / 项目详情</p>
        <h1 class="page-title">{{ project?.name ?? '项目详情' }}</h1>
        <p class="page-subtitle">管理设计稿版本，查看修改意见和确认状态。</p>
      </div>
      <div class="action-row">
        <el-button @click="editMode = true" :disabled="!project || project.status === 'archived'">
          项目设置
        </el-button>
        <el-button @click="archiveOrRestore" :loading="saving" :type="project?.status === 'archived' ? 'success' : 'warning'">
          {{ project?.status === 'archived' ? '恢复项目' : '归档项目' }}
        </el-button>
      </div>
    </header>

    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="5" animated />
    </div>

    <template v-else-if="project">
      <div class="project-layout">
        <!-- Main Area: Current Preview and Versions -->
        <div class="project-main">
          <div class="panel preview-panel">
            <div class="section-header">
              <div>
                <h2>设计稿预览</h2>
                <p v-if="currentVersion">当前展示：{{ currentVersion.versionName }} - {{ currentVersion.originalFilename }}</p>
              </div>
              <div v-if="project.status !== 'archived' && !project.confirmedVersionId">
                <el-upload
                  action=""
                  :http-request="handleUpload"
                  :show-file-list="false"
                  accept="image/*,.pdf"
                >
                  <el-button type="primary" :icon="Upload" :loading="uploading">上传新版本</el-button>
                </el-upload>
              </div>
            </div>

            <div class="preview-container">
              <div v-if="currentVersion" class="preview-box">
                <template v-if="currentVersion.fileExt?.toLowerCase() !== 'pdf'">
                  <img :src="currentVersion.previewUrl" alt="预览图" />
                  <span
                    v-for="(annotation, index) in annotations"
                    :key="annotation.id"
                    class="annotation-marker"
                    :class="annotation.status"
                    :style="annotationMarkerStyle(annotation)"
                  >
                    {{ index + 1 }}
                  </span>
                </template>
                <div v-else class="pdf-placeholder">
                  <el-icon :size="48"><Files /></el-icon>
                  <p>PDF 文件，点击下方链接下载查看</p>
                  <el-link :href="currentVersion.previewUrl" target="_blank" type="primary">打开 PDF</el-link>
                </div>
              </div>
              <div v-else class="empty-preview">
                <el-icon :size="64"><Upload /></el-icon>
                <p>暂无设计稿，请点击右上角上传第一版</p>
              </div>
            </div>
          </div>

          <div class="panel list-panel">
            <div class="section-header">
              <h2>版本历史</h2>
            </div>
            <el-table :data="versions" style="width: 100%">
              <el-table-column prop="versionName" label="版本" width="80">
                <template #default="{ row }">
                  <el-tag :type="row.isCurrent ? 'success' : 'info'" size="small">{{ row.versionName }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="originalFilename" label="文件名" min-width="180" show-overflow-tooltip />
              <el-table-column label="版本状态" width="100">
                <template #default="{ row }">
                  <el-tag v-if="row.isConfirmed" type="success" size="small">已定稿</el-tag>
                  <el-tag v-else-if="row.isCurrent" type="primary" size="small">当前展示</el-tag>
                  <el-tag v-else type="info" size="small">历史版本</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="反馈" width="80">
                <template #default="{ row }">
                  <div class="version-feedback">
                    <el-badge v-if="row.annotationCount > 0" :value="row.annotationCount" class="badge-item" type="warning">
                      <el-icon :size="14" title="修改意见"><ChatLineRound /></el-icon>
                    </el-badge>
                    <el-icon v-if="row.hasVoice" :size="14" class="voice-icon" title="含语音意见"><Microphone /></el-icon>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="fileSize" label="大小" width="100">
                <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
              </el-table-column>
              <el-table-column prop="uploaderNickname" label="上传人" width="120" />
              <el-table-column prop="createdAt" label="上传时间" width="160">
                <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="100" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" :icon="View" @click="selectVersion(row)">
                    预览
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>

        <!-- Sidebar Area: Info and Settings -->
        <div class="project-side">
          <div class="panel">
            <div class="section-header">
              <h2>项目状态</h2>
              <el-tag :type="project.status === 'confirmed' ? 'success' : 'primary'">
                {{ statusLabels[project.status] }}
              </el-tag>
            </div>
            <dl class="info-list">
              <dt>负责人</dt>
              <dd>{{ project.ownerNickname }}</dd>
              <dt>客户</dt>
              <dd>{{ project.customerName || '-' }}</dd>
              <dt>联系方式</dt>
              <dd>{{ project.customerContact || '-' }}</dd>
              <dt>创建时间</dt>
              <dd>{{ formatTime(project.createdAt) }}</dd>
            </dl>
          </div>

          <div class="panel side-panel">
            <div class="section-header">
              <div>
                <h2>定稿确认</h2>
                <p>客户确认当前项目后的记录。</p>
              </div>
              <el-icon v-if="confirmation" class="success-icon"><CircleCheck /></el-icon>
            </div>
            <div v-if="confirmation" class="confirmation-box">
              <strong>{{ confirmation.customerName }}</strong>
              <span>{{ formatTime(confirmation.confirmedAt) }} 确认</span>
              <small>版本 ID：{{ confirmation.versionId }}</small>
            </div>
            <div v-else class="empty-inline">尚未确认定稿</div>
          </div>

          <div class="panel side-panel">
            <div class="section-header">
              <div>
                <h2>修改意见</h2>
                <p>当前预览版本的客户标注。</p>
              </div>
              <el-icon><ChatLineRound /></el-icon>
            </div>
            <div v-if="loadingAnnotations" class="empty-inline">正在加载标注...</div>
            <div v-else-if="annotations.length === 0" class="empty-inline">当前版本暂无修改意见</div>
            <template v-else>
              <ol class="annotation-list">
                <li v-for="(annotation, index) in pagedAnnotations" :key="annotation.id">
                  <div class="annotation-head">
                    <span>#{{ (annotationCurrentPage - 1) * annotationPageSize + index + 1 }} {{ annotation.customerName || '客户' }}</span>
                    <el-tag :type="annotationStatusType(annotation.status)" size="small">
                      {{ annotationStatusText(annotation.status) }}
                    </el-tag>
                  </div>
                  <div class="annotation-body">
                    <span v-if="annotation.content" class="annotation-text">{{ annotation.content }}</span>
                    <el-button
                      v-if="annotation.mediaUrl"
                      size="small"
                      type="primary"
                      plain
                      :icon="Microphone"
                      @click="playVoice(annotation.mediaUrl)"
                      class="voice-play-btn"
                    >
                      {{ annotation.mediaDuration ? annotation.mediaDuration + '"' : '播放语音' }}
                    </el-button>
                  </div>
                  <small>{{ formatTime(annotation.createdAt) }}</small>
                  <div v-if="annotation.status === 'open'" class="annotation-actions">
                    <el-button size="small" type="primary" link @click="updateAnnotationStatus(annotation, 'resolved')">
                      标记已处理
                    </el-button>
                    <el-button size="small" type="warning" link @click="updateAnnotationStatus(annotation, 'ignored')">
                      忽略
                    </el-button>
                  </div>
                </li>
              </ol>

              <div v-if="!isAnnotationsExpanded && annotations.length > 5" class="annotation-expand-box">
                <el-button type="primary" link @click="isAnnotationsExpanded = true">
                  展开查看全部 {{ annotations.length }} 条意见
                </el-button>
              </div>

              <div v-if="isAnnotationsExpanded && annotations.length > annotationPageSize" class="pagination-wrap">
                <el-pagination
                  v-model:current-page="annotationCurrentPage"
                  :total="annotations.length"
                  :page-size="annotationPageSize"
                  layout="prev, pager, next"
                  small
                />
              </div>
            </template>
          </div>

          <div class="panel side-panel">
            <div class="section-header">
              <div>
                <h2>项目动态</h2>
                <p>审稿和确认流程记录。</p>
              </div>
              <el-icon><Clock /></el-icon>
            </div>
            <div v-if="timeline.length === 0" class="empty-inline">暂无动态记录</div>
            <ol v-else class="timeline-list">
              <li v-for="log in timeline" :key="log.id">
                <span>{{ formatTime(log.createdAt) }}</span>
                <strong>{{ operatorText(log) }}</strong>
                <p>{{ log.summary }}</p>
              </li>
            </ol>
          </div>

          <div class="panel side-panel">
            <div class="section-header">
              <div>
                <h2>审稿链接</h2>
                <p>完整链接只在生成时返回一次，请及时复制给客户。</p>
              </div>
            </div>

            <div v-if="latestCreatedLink?.url" class="review-link-copy">
              <span>{{ latestCreatedLink.url }}</span>
              <el-button type="primary" size="small" @click="copyText(latestCreatedLink.url)">复制</el-button>
              <el-button type="success" size="small" @click="copyReminderText(latestCreatedLink.url)">提醒文案</el-button>
            </div>

            <div class="review-link-form">
              <el-date-picker
                v-model="reviewLinkForm.expiresAt"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ss"
                placeholder="过期时间"
                style="width: 100%"
              />
              <el-input-number
                v-model="reviewLinkForm.maxAccessCount"
                :min="1"
                placeholder="最大访问次数"
                style="width: 100%"
              />
              <el-button type="primary" :loading="creatingReviewLink" @click="createReviewLink">生成审稿链接</el-button>
            </div>

            <div v-if="reviewLinks.length === 0" class="empty-state">
              暂无审稿链接
            </div>
            <ul v-else class="review-link-list">
              <li v-for="link in reviewLinks" :key="link.id">
                <div>
                  <el-tag :type="statusType(link.status)" size="small">{{ statusText(link.status) }}</el-tag>
                  <span>访问 {{ link.accessCount }} 次</span>
                  <small>创建：{{ formatTime(link.createdAt) }}</small>
                  <small>过期：{{ formatTime(link.expiresAt) }}</small>
                </div>
                <div class="review-link-actions">
                  <el-button link type="primary" @click="copyText(link.url)">复制</el-button>
                  <el-button v-if="link.url" link type="success" @click="copyReminderText(link.url)">提醒</el-button>
                  <el-button link type="warning" @click="toggleReviewLink(link)">
                    {{ link.status === 'disabled' ? '启用' : '禁用' }}
                  </el-button>
                  <el-button link type="danger" @click="removeReviewLink(link)">删除</el-button>
                </div>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </template>

    <!-- Settings Dialog -->
    <el-dialog v-model="editMode" title="项目设置" width="500px">
      <el-form :model="form" label-position="top">
        <el-form-item label="项目名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="负责人" required>
          <el-select v-model="form.ownerUserId" style="width: 100%">
            <el-option v-for="user in designerUsers" :key="user.userId" :label="user.nickname" :value="String(user.userId)" />
          </el-select>
        </el-form-item>
        <el-form-item label="客户名称">
          <el-input v-model="form.customerName" />
        </el-form-item>
        <el-form-item label="客户联系方式">
          <el-input v-model="form.customerContact" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelEdit">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.page {
  padding: 24px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}
.breadcrumb {
  font-size: 14px;
  color: #999;
  margin-bottom: 8px;
}
.breadcrumb a {
  color: #666;
  text-decoration: none;
}
.page-title {
  font-size: 24px;
  font-weight: 600;
}
.page-subtitle {
  color: #666;
  font-size: 14px;
}

.project-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 20px;
  align-items: start;
}

.panel {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.section-header h2 {
  font-size: 18px;
  font-weight: 600;
}
.section-header p {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.preview-container {
  background: #f5f7fa;
  border-radius: 4px;
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed #dcdfe6;
}

.preview-box img {
  max-width: 100%;
  max-height: 600px;
  display: block;
}

.preview-box {
  position: relative;
}

.annotation-marker {
  position: absolute;
  width: 26px;
  height: 26px;
  border: 2px solid #fff;
  border-radius: 50%;
  background: #f59e0b;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  line-height: 22px;
  text-align: center;
  transform: translate(-50%, -50%);
  box-shadow: 0 8px 16px rgba(15, 23, 42, 0.18);
}

.annotation-marker.resolved {
  background: #16a34a;
}

.annotation-marker.ignored {
  background: #64748b;
}

.pdf-placeholder, .empty-preview {
  text-align: center;
  color: #909399;
}
.pdf-placeholder p, .empty-preview p {
  margin: 16px 0;
}

.list-panel {
  margin-top: 24px;
}

.version-feedback {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 0;
}

.badge-item {
  margin-right: 8px;
}

:deep(.badge-item .el-badge__content) {
  transform: scale(0.8) translate(20%, -20%);
}

.voice-icon {
  color: #f59e0b;
}

.info-list dt {
  color: #999;
  font-size: 12px;
  margin-top: 16px;
}
.info-list dd {
  font-weight: 500;
  margin-top: 4px;
}

.side-panel {
  margin-top: 20px;
}

.success-icon {
  color: #16a34a;
}

.confirmation-box,
.timeline-list,
.annotation-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.confirmation-box strong {
  color: #15803d;
}

.confirmation-box span,
.confirmation-box small,
.timeline-list span,
.annotation-list small {
  color: #909399;
  font-size: 12px;
}

.empty-inline {
  color: #909399;
  font-size: 13px;
  padding: 8px 0;
}

.annotation-list,
.timeline-list,
.review-link-list {
  padding: 0;
  margin: 0;
  list-style: none;
}

.annotation-list li,
.timeline-list li,
.review-link-list li {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #f8fafc;
}

.annotation-head,
.review-link-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.annotation-head span {
  font-weight: 600;
}

.annotation-list p,
.timeline-list p {
  margin: 8px 0;
  color: #344054;
  line-height: 1.5;
}

.annotation-body {
  margin: 8px 0;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.annotation-text {
  color: #344054;
  line-height: 1.6;
  word-break: break-all;
}

.voice-play-btn {
  flex-shrink: 0;
}

.annotation-actions {
  margin-top: 8px;
}

.annotation-expand-box {
  margin-top: 12px;
  text-align: center;
  border-top: 1px dashed #ebeef5;
  padding-top: 12px;
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}

.timeline-list strong {
  display: block;
  margin-top: 4px;
  font-size: 13px;
}

.review-link-copy {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px;
  margin-bottom: 12px;
  background: #eef6ff;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
}

.review-link-copy span {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #1d4ed8;
  font-size: 12px;
}

.review-link-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 14px;
}

.review-link-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.review-link-list li > div:first-child {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.empty-state {
  text-align: center;
  padding: 40px 0;
  color: #999;
}
.empty-state p {
  margin-bottom: 20px;
  font-size: 14px;
}

.loading-state {
  background: #fff;
  padding: 40px;
  border-radius: 8px;
}

@media (max-width: 1280px) {
  .project-layout {
    grid-template-columns: minmax(0, 1fr) 280px;
  }
}

@media (max-width: 1024px) {
  .project-layout {
    grid-template-columns: 1fr;
  }
}
</style>
