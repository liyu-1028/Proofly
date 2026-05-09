<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CircleCheck, Message, Pointer } from '@element-plus/icons-vue'

import { ApiError } from '@/api/http'
import * as annotationApi from '@/api/annotations'
import * as confirmationApi from '@/api/confirmations'
import * as publicReviewApi from '@/api/public-review'

const route = useRoute()

const loading = ref(false)
const submittingAnnotation = ref(false)
const confirming = ref(false)
const errorMessage = ref('')
const reviewData = ref<publicReviewApi.PublicProjectReviewResponse | null>(null)
const selectedPoint = ref<{ xRatio: number; yRatio: number } | null>(null)

const token = computed(() => String(route.params.token ?? ''))
const activeVersion = computed(() => {
  const data = reviewData.value
  if (!data) return null
  return (
    data.versions.find(version => version.id === data.activeVersionId) ??
    data.versions.find(version => version.isCurrent) ??
    data.versions[0] ??
    null
  )
})

const isImageVersion = computed(() => activeVersion.value?.fileExt?.toLowerCase() !== 'pdf')
const hasConfirmation = computed(() => Boolean(reviewData.value?.confirmation))

const annotationForm = reactive({
  customerName: '',
  content: '',
})

const confirmationForm = reactive({
  customerName: '',
  customerContact: '',
})

async function loadReview() {
  if (!token.value) return
  loading.value = true
  errorMessage.value = ''
  try {
    reviewData.value = await publicReviewApi.getPublicReview(token.value)
    if (reviewData.value.project.customerName) {
      annotationForm.customerName = reviewData.value.project.customerName
      confirmationForm.customerName = reviewData.value.project.customerName
    }
    if (reviewData.value.project.customerContact) {
      confirmationForm.customerContact = reviewData.value.project.customerContact
    }
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : '审稿数据加载失败'
  } finally {
    loading.value = false
  }
}

function formatTime(value?: string | null) {
  if (!value) return '-'
  return new Date(value).toLocaleString()
}

function statusText(status: annotationApi.AnnotationResponse['status']) {
  const map = {
    open: '待处理',
    resolved: '已处理',
    ignored: '已忽略',
  }
  return map[status] ?? status
}

function statusType(status: annotationApi.AnnotationResponse['status']) {
  if (status === 'resolved') return 'success'
  if (status === 'ignored') return 'info'
  return 'warning'
}

function handlePreviewClick(event: MouseEvent) {
  if (!isImageVersion.value || hasConfirmation.value) return
  const target = event.currentTarget as HTMLElement
  const rect = target.getBoundingClientRect()
  selectedPoint.value = {
    xRatio: Number(((event.clientX - rect.left) / rect.width).toFixed(4)),
    yRatio: Number(((event.clientY - rect.top) / rect.height).toFixed(4)),
  }
}

function markerStyle(annotation: Pick<annotationApi.AnnotationResponse, 'xRatio' | 'yRatio'>) {
  return {
    left: `${Number(annotation.xRatio) * 100}%`,
    top: `${Number(annotation.yRatio) * 100}%`,
  }
}

async function submitAnnotation() {
  if (!selectedPoint.value) {
    ElMessage.warning('请先点击设计稿上的修改位置')
    return
  }
  if (!annotationForm.content.trim()) {
    ElMessage.warning('请填写修改意见')
    return
  }
  submittingAnnotation.value = true
  try {
    const created = await annotationApi.createPublicAnnotation(token.value, {
      type: 'point',
      xRatio: selectedPoint.value.xRatio,
      yRatio: selectedPoint.value.yRatio,
      content: annotationForm.content.trim(),
      customerName: annotationForm.customerName.trim() || undefined,
    })
    reviewData.value?.annotations.unshift(created)
    annotationForm.content = ''
    selectedPoint.value = null
    ElMessage.success('修改意见已提交')
  } catch (error: any) {
    ElMessage.error(error?.message || '提交修改意见失败')
  } finally {
    submittingAnnotation.value = false
  }
}

async function submitConfirmation() {
  if (!confirmationForm.customerName.trim()) {
    ElMessage.warning('请填写确认人')
    return
  }
  confirming.value = true
  try {
    const confirmation = await confirmationApi.createPublicConfirmation(token.value, {
      customerName: confirmationForm.customerName.trim(),
      customerContact: confirmationForm.customerContact.trim() || undefined,
    })
    if (reviewData.value) {
      reviewData.value.confirmation = confirmation
    }
    ElMessage.success('已确认定稿')
  } catch (error: any) {
    ElMessage.error(error?.message || '确认定稿失败')
  } finally {
    confirming.value = false
  }
}

onMounted(() => {
  void loadReview()
})
</script>

<template>
  <section class="review-page">
    <div v-if="loading" class="review-shell">
      <el-skeleton :rows="8" animated />
    </div>

    <el-alert v-else-if="errorMessage" :title="errorMessage" type="error" show-icon :closable="false" />

    <template v-else-if="reviewData">
      <header class="review-header">
        <div>
          <p class="eyebrow">客户审稿</p>
          <h1>{{ reviewData.project.name }}</h1>
          <p>{{ reviewData.project.customerName || '客户' }} · {{ activeVersion?.versionName || '当前版本' }}</p>
        </div>
        <el-tag v-if="hasConfirmation" type="success" size="large">已确认定稿</el-tag>
      </header>

      <main class="review-layout">
        <section class="review-main">
          <div class="preview-panel">
            <div v-if="activeVersion && isImageVersion" class="preview-stage" @click="handlePreviewClick">
              <img :src="activeVersion.previewUrl" :alt="activeVersion.originalFilename" />
              <span
                v-for="(annotation, index) in reviewData.annotations"
                :key="annotation.id"
                class="annotation-marker"
                :class="annotation.status"
                :style="markerStyle(annotation)"
              >
                {{ index + 1 }}
              </span>
              <span v-if="selectedPoint" class="annotation-marker drafting" :style="markerStyle(selectedPoint)">
                +
              </span>
            </div>
            <div v-else-if="activeVersion" class="pdf-state">
              <el-icon :size="52"><Message /></el-icon>
              <p>{{ activeVersion.originalFilename }}</p>
              <el-link :href="activeVersion.previewUrl" target="_blank" type="primary">打开 PDF 文件</el-link>
            </div>
            <div v-else class="pdf-state">
              <el-empty description="暂无可审稿版本" />
            </div>
          </div>
        </section>

        <aside class="review-side">
          <div class="panel">
            <div class="panel-title">
              <el-icon><Pointer /></el-icon>
              <span>提交修改意见</span>
            </div>
            <el-alert
              v-if="hasConfirmation"
              title="该项目已确认定稿，不能继续提交修改意见。"
              type="success"
              :closable="false"
              show-icon
            />
            <template v-else>
              <p class="hint">点击左侧设计稿定位问题，再填写意见。</p>
              <el-form label-position="top">
                <el-form-item label="客户名称">
                  <el-input v-model="annotationForm.customerName" placeholder="用于设计师识别反馈人" />
                </el-form-item>
                <el-form-item label="修改意见" required>
                  <el-input v-model="annotationForm.content" type="textarea" :rows="4" placeholder="请输入需要调整的内容" />
                </el-form-item>
                <el-button type="primary" :loading="submittingAnnotation" @click="submitAnnotation">提交意见</el-button>
              </el-form>
            </template>
          </div>

          <div class="panel">
            <div class="panel-title">
              <el-icon><CircleCheck /></el-icon>
              <span>确认定稿</span>
            </div>
            <div v-if="reviewData.confirmation" class="confirmation-state">
              <strong>{{ reviewData.confirmation.customerName }}</strong>
              <span>{{ formatTime(reviewData.confirmation.confirmedAt) }} 已确认</span>
            </div>
            <el-form v-else label-position="top">
              <el-form-item label="确认人" required>
                <el-input v-model="confirmationForm.customerName" />
              </el-form-item>
              <el-form-item label="联系方式">
                <el-input v-model="confirmationForm.customerContact" />
              </el-form-item>
              <el-button type="success" :loading="confirming" @click="submitConfirmation">确认当前版本</el-button>
            </el-form>
          </div>

          <div class="panel">
            <div class="panel-title">
              <el-icon><Message /></el-icon>
              <span>修改意见</span>
            </div>
            <el-empty v-if="reviewData.annotations.length === 0" description="暂无修改意见" />
            <ol v-else class="annotation-list">
              <li v-for="(annotation, index) in reviewData.annotations" :key="annotation.id">
                <div class="annotation-head">
                  <span>#{{ index + 1 }} {{ annotation.customerName || '客户' }}</span>
                  <el-tag :type="statusType(annotation.status)" size="small">{{ statusText(annotation.status) }}</el-tag>
                </div>
                <p>{{ annotation.content }}</p>
                <small>{{ formatTime(annotation.createdAt) }}</small>
              </li>
            </ol>
          </div>
        </aside>
      </main>
    </template>
  </section>
</template>

<style scoped>
.review-page {
  min-height: 100vh;
  padding: 28px;
  background: #f4f6f8;
  color: #1f2933;
}

.review-shell,
.preview-panel,
.panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.review-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.review-header h1 {
  margin: 4px 0;
  font-size: 28px;
  font-weight: 700;
}

.review-header p {
  margin: 0;
  color: #667085;
}

.eyebrow {
  font-size: 13px;
  font-weight: 600;
  color: #2563eb;
}

.review-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 20px;
  align-items: start;
}

.preview-panel {
  min-height: 620px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.preview-stage {
  position: relative;
  max-width: 100%;
  cursor: crosshair;
}

.preview-stage img {
  display: block;
  max-width: 100%;
  max-height: 760px;
  border-radius: 4px;
}

.annotation-marker {
  position: absolute;
  width: 28px;
  height: 28px;
  border: 2px solid #fff;
  border-radius: 50%;
  background: #f59e0b;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  line-height: 24px;
  text-align: center;
  transform: translate(-50%, -50%);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.2);
}

.annotation-marker.resolved {
  background: #16a34a;
}

.annotation-marker.ignored {
  background: #64748b;
}

.annotation-marker.drafting {
  background: #2563eb;
}

.pdf-state {
  text-align: center;
  color: #667085;
}

.pdf-state p {
  margin: 12px 0;
  font-weight: 600;
}

.review-side {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel {
  padding: 18px;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  font-size: 16px;
  font-weight: 700;
}

.hint {
  margin: 0 0 14px;
  color: #667085;
  font-size: 13px;
}

.confirmation-state {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #15803d;
}

.confirmation-state span {
  color: #667085;
  font-size: 13px;
}

.annotation-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 0;
  margin: 0;
  list-style: none;
}

.annotation-list li {
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.annotation-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-weight: 600;
}

.annotation-list p {
  margin: 8px 0;
  color: #344054;
  line-height: 1.6;
}

.annotation-list small {
  color: #98a2b3;
}

@media (max-width: 960px) {
  .review-page {
    padding: 16px;
  }

  .review-layout {
    grid-template-columns: 1fr;
  }

  .preview-panel {
    min-height: 360px;
  }
}
</style>
