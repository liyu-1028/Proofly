<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CircleCheck, Message, Pointer } from '@element-plus/icons-vue'

import { ApiError } from '@/api/http'
import * as annotationApi from '@/api/annotations'
import * as confirmationApi from '@/api/confirmations'
import * as publicReviewApi from '@/api/public-review'
import * as configApi from '@/api/configs'
import * as pdfjsLib from 'pdfjs-dist'

// 设置 worker 路径（通常需要复制到 public 目录或进行链接）
// 对于本地开发，可能需要特定的 URL。
pdfjsLib.GlobalWorkerOptions.workerSrc = `https://cdnjs.cloudflare.com/ajax/libs/pdf.js/${pdfjsLib.version}/pdf.worker.min.js`

const route = useRoute()

const loading = ref(false)
const brandingLoading = ref(false)
const brandConfig = ref<Record<string, string>>({})
const submittingAnnotation = ref(false)
const confirming = ref(false)
const errorMessage = ref('')
const reviewData = ref<publicReviewApi.PublicProjectReviewResponse | null>(null)
const selectedPoint = ref<{ xRatio: number; yRatio: number } | null>(null)
const pdfCanvasRef = ref<HTMLCanvasElement | null>(null)

const token = computed(() => String(route.params.token ?? ''))

async function renderPdf() {
  if (!activeVersion.value || !isImageVersion.value === false) return
  if (!pdfCanvasRef.value) return
  
  try {
    const loadingTask = pdfjsLib.getDocument(activeVersion.value.previewUrl)
    const pdf = await loadingTask.promise
    const page = await pdf.getPage(1) // MVP 版本仅渲染第 1 页
    
    const viewport = page.getViewport({ scale: 1.5 })
    const canvas = pdfCanvasRef.value
    const context = canvas.getContext('2d')
    if (!context) return
    
    canvas.height = viewport.height
    canvas.width = viewport.width
    
    const renderContext = {
      canvasContext: context,
      viewport: viewport
    }
    await page.render(renderContext).promise
  } catch (err) {
    console.error('PDF rendering failed', err)
  }
}

async function loadBranding(storeId: string) {
  brandingLoading.value = true
  try {
    brandConfig.value = await configApi.getBrandConfig(storeId)
    // 如果存在，应用品牌主色调
    const primaryColor = brandConfig.value['brand.primary_color']
    if (primaryColor) {
      document.documentElement.style.setProperty('--el-color-primary', primaryColor)
    }
  } catch (e) {
    console.error('Failed to load branding', e)
  } finally {
    brandingLoading.value = false
  }
}
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
  mediaUrl: '',
  mediaDuration: 0,
})

const isRecording = ref(false)
const recordingTime = ref(0)
const mediaRecorder = ref<MediaRecorder | null>(null)
const audioChunks = ref<Blob[]>([])
const recordedBlob = ref<Blob | null>(null)
let timer: any = null

async function startRecording() {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    mediaRecorder.value = new MediaRecorder(stream)
    audioChunks.value = []
    
    mediaRecorder.value.ondataavailable = (event) => {
      audioChunks.value.push(event.data)
    }
    
    mediaRecorder.value.onstop = () => {
      const mimeType = mediaRecorder.value?.mimeType || 'audio/webm'
      recordedBlob.value = new Blob(audioChunks.value, { type: mimeType })
      annotationForm.mediaDuration = recordingTime.value
      stream.getTracks().forEach(track => track.stop())
    }
    
    mediaRecorder.value.start()
    isRecording.value = true
    recordingTime.value = 0
    recordedBlob.value = null
    timer = setInterval(() => {
      recordingTime.value++
    }, 1000)
  } catch (err) {
    ElMessage.error('无法启动录音，请检查麦克风权限')
  }
}

function stopRecording() {
  if (mediaRecorder.value && isRecording.value) {
    mediaRecorder.value.stop()
    isRecording.value = false
    if (timer) clearInterval(timer)
  }
}

function clearVoice() {
  recordedBlob.value = null
  annotationForm.mediaDuration = 0
  annotationForm.mediaUrl = ''
}

async function uploadVoice(blob: Blob) {
  const isMp4 = blob.type.includes('mp4')
  const ext = isMp4 ? 'm4a' : 'webm'
  const file = new File([blob], `voice_${Date.now()}.${ext}`, { type: blob.type })
  const formData = new FormData()
  formData.append('file', file)
  formData.append('fileRole', 'attachment')
  
  const response = await fetch(`/api/public/reviews/${token.value}/files/upload`, {
    method: 'POST',
    body: formData
  })
  const result = await response.json()
  if (result.code !== 0) {
    throw new Error(result.message || '语音上传失败')
  }
  return result.data.objectKey
}

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

    // 第 2 阶段：加载品牌配置
    void loadBranding(reviewData.value.project.storeId)
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
  if (hasConfirmation.value) return
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
  if (!annotationForm.content.trim() && !recordedBlob.value) {
    ElMessage.warning('请填写修改意见或录制语音')
    return
  }
  submittingAnnotation.value = true
  try {
    let mediaUrl = annotationForm.mediaUrl
    if (recordedBlob.value) {
      mediaUrl = await uploadVoice(recordedBlob.value)
    }

    const created = await annotationApi.createPublicAnnotation(token.value, {
      type: 'point',
      xRatio: selectedPoint.value.xRatio,
      yRatio: selectedPoint.value.yRatio,
      content: annotationForm.content.trim(),
      mediaUrl: mediaUrl || undefined,
      mediaDuration: annotationForm.mediaDuration || undefined,
      customerName: annotationForm.customerName.trim() || undefined,
    })
    reviewData.value?.annotations.unshift(created)
    annotationForm.content = ''
    annotationForm.mediaUrl = ''
    annotationForm.mediaDuration = 0
    recordedBlob.value = null
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

watch(activeVersion, async (newVal) => {
  if (newVal && newVal.fileExt?.toLowerCase() === 'pdf') {
    await nextTick()
    void renderPdf()
  }
}, { immediate: true })

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
        <div class="header-main">
          <div class="branding">
            <img v-if="brandConfig['brand.logo_url']" :src="brandConfig['brand.logo_url']" alt="Logo" class="brand-logo" />
            <div v-else class="brand-placeholder">
              <span class="eyebrow">客户审稿</span>
            </div>
          </div>
          <div class="project-info">
            <h1>{{ reviewData.project.name }}</h1>
            <p>{{ reviewData.project.customerName || '客户' }} · {{ activeVersion?.versionName || '当前版本' }}</p>
          </div>
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
            <div v-else-if="activeVersion" class="preview-stage pdf-preview" @click="handlePreviewClick">
              <canvas ref="pdfCanvasRef"></canvas>
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
                <el-form-item label="修改意见">
                  <el-input v-model="annotationForm.content" type="textarea" :rows="4" placeholder="请输入需要调整的内容" />
                </el-form-item>
                <div class="voice-actions">
                  <el-button v-if="!isRecording" size="small" @click="startRecording">
                    {{ recordedBlob ? '重新录制语音' : '录制语音意见' }}
                  </el-button>
                  <el-button v-else size="small" type="danger" @click="stopRecording">
                    正在录音 ({{ recordingTime }}s) - 点击停止
                  </el-button>
                  <div v-if="recordedBlob" class="voice-ready">
                    <span>语音已就绪 ({{ annotationForm.mediaDuration }}s)</span>
                    <el-button link type="danger" size="small" @click="clearVoice">清除</el-button>
                  </div>
                </div>
                <el-button type="primary" :loading="submittingAnnotation" @click="submitAnnotation" style="margin-top: 12px;">提交意见</el-button>
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
                <div v-if="annotation.mediaUrl" class="voice-player">
                  <audio :src="annotation.mediaUrl" controls style="height: 32px; width: 100%;"></audio>
                </div>
                <small>{{ formatTime(annotation.createdAt) }}</small>
              </li>
            </ol>
          </div>

          <div class="powered-by">
            <p>由 <strong>审稿宝</strong> 提供支持</p>
            <router-link to="/register" target="_blank" class="plg-link">免费创建您的审稿项目 &rarr;</router-link>
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
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.header-main {
  display: flex;
  align-items: center;
  gap: 16px;
}

.brand-logo {
  height: 48px;
  max-width: 120px;
  object-fit: contain;
}

.review-header h1 {
  margin: 0;
  font-size: 24px;
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

.voice-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}

.voice-ready {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #16a34a;
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

.powered-by {
  margin-top: 8px;
  text-align: center;
  padding: 16px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.powered-by p {
  font-size: 13px;
  color: #667085;
  margin: 0 0 8px;
}

.plg-link {
  display: inline-block;
  font-size: 14px;
  color: #2a9d8f;
  text-decoration: none;
  font-weight: 600;
  transition: all 0.3s;
}

.plg-link:hover {
  color: #21867a;
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
