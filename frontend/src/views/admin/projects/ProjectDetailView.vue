<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Upload, View, Files } from '@element-plus/icons-vue'

import { ApiError } from '@/api/http'
import * as projectApi from '@/api/projects'
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
const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const errorMessage = ref('')
const editMode = ref(false)

const projectId = computed(() => String(route.params.projectId))
const designerUsers = computed(() => users.value.filter((user) => user.roles.includes('designer') || user.roles.includes('owner')))

const currentVersion = computed(() => versions.value.find(v => v.isCurrent) || versions.value[0])

const form = reactive({
  name: '',
  customerName: '',
  customerContact: '',
  ownerUserId: '',
  remark: '',
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
    const [projectData, userData, versionData] = await Promise.all([
      projectApi.getProject(projectId.value),
      getUsers().catch(() => []),
      versionApi.listVersions(projectId.value).catch(() => []),
    ])
    project.value = projectData
    users.value = userData
    versions.value = versionData
    fillForm(projectData)
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : '项目加载失败'
  } finally {
    loading.value = false
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
  uploading.value = true
  try {
    const newVersion = await versionApi.uploadVersion(projectId.value, options.file)
    versions.value = [newVersion, ...versions.value]
    // Refresh project to get updated status and currentVersionId
    const updatedProject = await projectApi.getProject(projectId.value)
    project.value = updatedProject
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

onMounted(() => {
  if (!projectId.value || projectId.value === 'undefined') {
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
                <img v-if="currentVersion.fileExt?.toLowerCase() !== 'pdf'" :src="currentVersion.previewUrl" alt="预览图" />
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
              <el-table-column prop="originalFilename" label="文件名" min-width="200" show-overflow-tooltip />
              <el-table-column prop="fileSize" label="大小" width="100">
                <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
              </el-table-column>
              <el-table-column prop="uploaderNickname" label="上传人" width="120" />
              <el-table-column prop="createdAt" label="上传时间" width="160">
                <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="100" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" :icon="View" @click="versions.forEach(v => v.isCurrent = false); row.isCurrent = true">
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

          <div class="panel" style="margin-top: 20px">
            <div class="section-header">
              <h2>审稿链接</h2>
            </div>
            <div class="empty-state">
              <p>审稿链接模块将在后续接入</p>
              <el-button type="primary" disabled>生成审稿链接</el-button>
            </div>
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
  grid-template-columns: 1fr 300px;
  gap: 24px;
}

.panel {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
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

.info-list dt {
  color: #999;
  font-size: 12px;
  margin-top: 16px;
}
.info-list dd {
  font-weight: 500;
  margin-top: 4px;
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
</style>
