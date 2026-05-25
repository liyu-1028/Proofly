<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Bell,
  ChatLineRound,
  CircleCheck,
  Clock,
  Files,
  Finished,
  FolderOpened,
  Refresh,
  TrendCharts,
  Warning,
} from '@element-plus/icons-vue'

import { getDashboardStats, type AuditLogResponse, type DashboardStatsResponse } from '@/api/dashboard'
import type { ProjectResponse, ProjectStatus } from '@/types/project'
import { useNotificationStore } from '@/stores/notification'
import NotificationCenter from '@/components/NotificationCenter.vue'

const notificationStore = useNotificationStore()
const showNotifications = ref(false)

const statusLabels: Record<ProjectStatus, string> = {
  draft: '草稿',
  waiting_feedback: '待反馈',
  change_requested: '需修改',
  waiting_confirm: '待确认',
  confirmed: '已确认',
  archived: '已归档',
}

const statusHints: Record<ProjectStatus, string> = {
  draft: '尚未发送给客户',
  waiting_feedback: '客户正在审稿',
  change_requested: '客户已提出修改',
  waiting_confirm: '等待客户定稿',
  confirmed: '已完成闭环',
  archived: '已归档保存',
}

const loading = ref(false)
const stats = ref<DashboardStatsResponse | null>(null)

const totalProjects = computed(() => stats.value?.totalProjects ?? 0)
const waitingFeedback = computed(() => stats.value?.statusCounts?.waiting_feedback ?? 0)
const changeRequested = computed(() => stats.value?.statusCounts?.change_requested ?? 0)
const waitingConfirm = computed(() => stats.value?.statusCounts?.waiting_confirm ?? 0)
const confirmed = computed(() => Number(stats.value?.statusCounts?.confirmed ?? 0))
const draft = computed(() => Number(stats.value?.statusCounts?.draft ?? 0))
const urgentCount = computed(() => changeRequested.value + waitingConfirm.value)
const completionRate = computed(() => {
  const total = totalProjects.value
  const done = confirmed.value
  if (!total || total <= 0) return 0
  return Math.round((done / total) * 100)
})

const metricCards = computed(() => [
  {
    label: '全部项目',
    value: totalProjects.value,
    hint: '当前门店进行中与已完成项目',
    icon: FolderOpened,
    tone: 'blue',
  },
  {
    label: '客户审稿中',
    value: waitingFeedback.value,
    hint: '已发出版本，等待客户反馈',
    icon: ChatLineRound,
    tone: 'cyan',
  },
  {
    label: '需要处理',
    value: changeRequested.value,
    hint: '客户已提交修改意见',
    icon: Warning,
    tone: 'orange',
  },
  {
    label: '等待确认',
    value: waitingConfirm.value,
    hint: '可推动客户确认定稿',
    icon: Bell,
    tone: 'purple',
  },
])

const statusCards = computed(() => [
  { status: 'draft' as ProjectStatus, count: draft.value },
  { status: 'waiting_feedback' as ProjectStatus, count: waitingFeedback.value },
  { status: 'change_requested' as ProjectStatus, count: changeRequested.value },
  { status: 'waiting_confirm' as ProjectStatus, count: waitingConfirm.value },
  { status: 'confirmed' as ProjectStatus, count: confirmed.value },
])

const priorityProjects = computed(() => {
  const list = stats.value?.recentProjects ?? []
  return [...list]
    .sort((a, b) => priorityScore(b) - priorityScore(a))
    .slice(0, 3)
})

async function loadStats() {
  loading.value = true
  try {
    stats.value = await getDashboardStats()
  } catch (error: any) {
    ElMessage.error(error.message || '加载工作台数据失败')
  } finally {
    loading.value = false
  }
}

function formatTime(value: string) {
  return new Date(value).toLocaleString()
}

function formatShortTime(value: string) {
  const date = new Date(value)
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  const hour = `${date.getHours()}`.padStart(2, '0')
  const minute = `${date.getMinutes()}`.padStart(2, '0')
  return `${month}-${day} ${hour}:${minute}`
}

function priorityScore(project: ProjectResponse) {
  const map: Record<ProjectStatus, number> = {
    change_requested: 5,
    waiting_confirm: 4,
    waiting_feedback: 3,
    draft: 2,
    confirmed: 1,
    archived: 0,
  }
  return map[project.status] ?? 0
}

function activityIcon(log: AuditLogResponse) {
  if (log.action?.includes('CONFIRM')) return CircleCheck
  if (log.action?.includes('ANNOTATION')) return ChatLineRound
  if (log.action?.includes('VERSION')) return Files
  return Clock
}

function activityTone(log: AuditLogResponse) {
  if (log.action?.includes('CONFIRM')) return 'success'
  if (log.action?.includes('ANNOTATION')) return 'warning'
  if (log.action?.includes('VERSION')) return 'primary'
  return 'neutral'
}

function operatorName(log: AuditLogResponse) {
  if (log.operatorName) return log.operatorName
  if (log.operatorType === 'customer') return '客户'
  if (log.operatorType === 'system') return '系统'
  return '用户'
}

onMounted(() => {
  void loadStats()
})
</script>

<template>
  <section class="dashboard-page" v-loading="loading">
    <header class="dashboard-hero">
      <div class="hero-copy">
        <p class="eyebrow">Proofly 工作台</p>
        <h1>今天需要推进的审稿，都在这里。</h1>
        <p>集中查看项目状态、客户反馈、确认进度和最新动态，少翻页面，多做判断。</p>
      </div>
      <div class="hero-actions">
        <button class="icon-button notification-trigger" type="button" @click="showNotifications = true">
          <el-badge :value="notificationStore.unreadCount" :max="99" :hidden="notificationStore.unreadCount === 0">
            <el-icon><Bell /></el-icon>
          </el-badge>
        </button>
        <button class="icon-button" type="button" aria-label="刷新工作台" @click="loadStats">
          <el-icon><Refresh /></el-icon>
        </button>
        <RouterLink to="/admin/projects" class="primary-link">
          <el-icon><FolderOpened /></el-icon>
          新建或查看项目
        </RouterLink>
      </div>
    </header>

    <NotificationCenter v-model:visible="showNotifications" />

    <div class="summary-strip">
      <div>
        <span>待处理事项</span>
        <strong>{{ urgentCount }}</strong>
      </div>
      <div>
        <span>确认完成率</span>
        <strong>{{ completionRate }}%</strong>
      </div>
      <div>
        <span>最近动态</span>
        <strong>{{ stats?.recentActivities.length ?? 0 }}</strong>
      </div>
    </div>

    <div class="metric-grid dashboard-metrics">
      <div v-for="metric in metricCards" :key="metric.label" class="metric-card" :class="`tone-${metric.tone}`">
        <div class="metric-icon">
          <el-icon><component :is="metric.icon" /></el-icon>
        </div>
        <div>
          <span>{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
          <small>{{ metric.hint }}</small>
        </div>
      </div>
    </div>

    <div class="dashboard-grid">
      <section class="panel priority-panel">
        <div class="section-header">
          <div>
            <h2>优先处理</h2>
            <p>按项目状态自动提取最近项目中的高优先级事项。</p>
          </div>
          <RouterLink to="/admin/projects" class="text-button">查看全部</RouterLink>
        </div>

        <div v-if="priorityProjects.length" class="priority-list">
          <RouterLink
            v-for="project in priorityProjects"
            :key="project.id"
            :to="`/admin/projects/${project.id}`"
            class="priority-item"
          >
            <span class="project-dot" :class="`status-${project.status}`"></span>
            <div>
              <strong>{{ project.name }}</strong>
              <small>{{ project.customerName || '匿名客户' }} · {{ formatShortTime(project.updatedAt) }}</small>
            </div>
            <span class="status-pill" :class="`status-${project.status}`">
              {{ statusLabels[project.status] }}
            </span>
          </RouterLink>
        </div>
        <div v-else class="empty-card">
          <el-icon><Finished /></el-icon>
          <span>目前没有需要优先处理的项目</span>
        </div>
      </section>

      <section class="panel progress-panel">
        <div class="section-header">
          <div>
            <h2>状态分布</h2>
            <p>快速判断项目集中卡在哪个阶段。</p>
          </div>
          <el-icon class="section-icon"><TrendCharts /></el-icon>
        </div>

        <div class="completion-ring">
          <div>
            <strong>{{ completionRate }}%</strong>
            <span>确认完成率</span>
          </div>
        </div>

        <ul class="status-list">
          <li v-for="item in statusCards" :key="item.status">
            <div>
              <span class="project-dot" :class="`status-${item.status}`"></span>
              <strong>{{ statusLabels[item.status] }}</strong>
              <small>{{ statusHints[item.status] }}</small>
            </div>
            <b>{{ item.count }}</b>
          </li>
        </ul>
      </section>
    </div>

    <div class="dashboard-grid lower-grid">
      <section class="panel">
        <div class="section-header">
          <div>
            <h2>最近项目</h2>
            <p>最近更新的审稿项目。</p>
          </div>
          <RouterLink to="/admin/projects" class="text-button">查看全部</RouterLink>
        </div>
        <div v-if="stats?.recentProjects.length" class="project-table">
          <RouterLink
            v-for="project in stats.recentProjects"
            :key="project.id"
            :to="`/admin/projects/${project.id}`"
            class="project-row"
          >
            <div>
              <strong>{{ project.name }}</strong>
              <small>{{ project.customerName || '匿名客户' }} · 负责人 {{ project.ownerNickname || '-' }}</small>
            </div>
            <span class="status-pill" :class="`status-${project.status}`">
              {{ statusLabels[project.status as ProjectStatus] }}
            </span>
          </RouterLink>
        </div>
        <div v-else class="empty-card">
          <el-icon><FolderOpened /></el-icon>
          <span>暂无项目</span>
        </div>
      </section>

      <section class="panel">
        <div class="section-header">
          <div>
            <h2>近期动态</h2>
            <p>客户反馈、版本上传和定稿动作会在这里出现。</p>
          </div>
        </div>
        <ol v-if="stats?.recentActivities.length" class="timeline">
          <li v-for="log in stats.recentActivities" :key="log.id" :class="`activity-${activityTone(log)}`">
            <span class="timeline-icon">
              <el-icon><component :is="activityIcon(log)" /></el-icon>
            </span>
            <div>
              <strong>{{ log.summary }}</strong>
              <small>{{ formatTime(log.createdAt) }} · {{ operatorName(log) }}</small>
            </div>
          </li>
        </ol>
        <div v-else class="empty-card">
          <el-icon><Clock /></el-icon>
          <span>暂无动态</span>
        </div>
      </section>
    </div>
  </section>
</template>

<style scoped>
.dashboard-page {
  display: grid;
  gap: 20px;
}

.dashboard-hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  min-height: 180px;
  border: 1px solid #d9e4ee;
  border-radius: 8px;
  padding: 28px;
  background:
    linear-gradient(135deg, rgba(20, 121, 109, 0.1), rgba(37, 99, 235, 0.08)),
    #ffffff;
}

.hero-copy {
  max-width: 720px;
}

.eyebrow {
  margin: 0 0 10px;
  color: #14796d;
  font-size: 13px;
  font-weight: 800;
}

.hero-copy h1 {
  margin: 0;
  color: #182026;
  font-size: 34px;
  line-height: 1.2;
}

.hero-copy p:last-child {
  margin: 12px 0 0;
  color: #5d6b78;
  font-size: 16px;
  line-height: 1.7;
}

.hero-actions {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.icon-button,
.primary-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-radius: 8px;
}

.icon-button {
  width: 40px;
  height: 40px;
  border: 1px solid #d9e4ee;
  background: #ffffff;
  color: #14796d;
  cursor: pointer;
  transition: all 0.2s;
}

.icon-button:hover {
  background: #f5f7fa;
  border-color: #14796d;
}

.notification-trigger :deep(.el-badge__content) {
  top: 5px;
  right: 5px;
}

.primary-link {
  min-height: 40px;
  padding: 0 14px;
  background: #14796d;
  color: #ffffff;
  font-weight: 800;
}

.summary-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1px;
  overflow: hidden;
  border: 1px solid #d9e4ee;
  border-radius: 8px;
  background: #d9e4ee;
}

.summary-strip div {
  padding: 16px 18px;
  background: #ffffff;
}

.summary-strip span,
.metric-card span,
.metric-card small,
.status-list small,
.project-row small,
.priority-item small,
.timeline small {
  color: #64727f;
  font-size: 13px;
}

.summary-strip strong {
  display: block;
  margin-top: 6px;
  color: #182026;
  font-size: 24px;
}

.dashboard-metrics {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.metric-card {
  display: flex;
  gap: 14px;
  min-height: 132px;
  border: 1px solid #d9e4ee;
  border-radius: 8px;
  padding: 18px;
  background: #ffffff;
}

.metric-icon {
  display: grid;
  width: 42px;
  height: 42px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 8px;
}

.metric-card strong {
  display: block;
  margin-top: 8px;
  color: #182026;
  font-size: 32px;
  line-height: 1;
}

.metric-card small {
  display: block;
  margin-top: 10px;
  line-height: 1.5;
}

.tone-blue .metric-icon {
  background: #eaf2ff;
  color: #2563eb;
}

.tone-cyan .metric-icon {
  background: #e8f6f3;
  color: #14796d;
}

.tone-orange .metric-icon {
  background: #fff7e6;
  color: #b7791f;
}

.tone-purple .metric-icon {
  background: #f1ecff;
  color: #6d4aff;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(340px, 0.9fr);
  gap: 20px;
  align-items: start;
}

.lower-grid {
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
}

.panel {
  min-width: 0;
  padding: 20px;
}

.section-header {
  margin-bottom: 18px;
}

.section-icon {
  color: #14796d;
  font-size: 22px;
}

.priority-list,
.project-table {
  display: grid;
  gap: 10px;
}

.priority-item,
.project-row {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  border: 1px solid #e4ebf1;
  border-radius: 8px;
  padding: 14px;
  background: #fbfcfd;
}

.project-row {
  grid-template-columns: minmax(0, 1fr) auto;
}

.priority-item:hover,
.project-row:hover {
  border-color: #95d2ca;
  background: #f7fbfa;
}

.priority-item strong,
.project-row strong,
.timeline strong {
  display: block;
  color: #182026;
  line-height: 1.4;
}

.priority-item small,
.project-row small {
  display: block;
  margin-top: 4px;
}

.project-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #94a3b8;
}

.completion-ring {
  display: grid;
  width: 156px;
  height: 156px;
  place-items: center;
  margin: 4px auto 18px;
  border-radius: 50%;
  background:
    radial-gradient(circle at center, #ffffff 0 56%, transparent 57%),
    conic-gradient(#14796d calc(var(--rate, 0) * 1%), #e6edf2 0);
}

.completion-ring {
  --rate: v-bind(completionRate);
}

.completion-ring div {
  display: grid;
  place-items: center;
}

.completion-ring strong {
  color: #182026;
  font-size: 30px;
  line-height: 1;
}

.completion-ring span {
  margin-top: 6px;
  color: #64727f;
  font-size: 12px;
}

.status-list,
.timeline {
  display: grid;
  gap: 10px;
  padding: 0;
  margin: 0;
  list-style: none;
}

.status-list li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid #e4ebf1;
  border-radius: 8px;
  padding: 12px;
}

.status-list li > div {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 2px 10px;
  align-items: center;
}

.status-list small {
  grid-column: 2;
}

.status-list b {
  color: #182026;
  font-size: 20px;
}

.timeline li {
  position: relative;
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 12px;
  padding: 4px 0 14px;
}

.timeline li:not(:last-child)::after {
  position: absolute;
  top: 38px;
  bottom: -6px;
  left: 16px;
  width: 1px;
  background: #e4ebf1;
  content: "";
}

.timeline-icon {
  z-index: 1;
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 50%;
  background: #eef2f5;
  color: #52616f;
}

.activity-success .timeline-icon {
  background: #e8f6f3;
  color: #14796d;
}

.activity-warning .timeline-icon {
  background: #fff7e6;
  color: #b7791f;
}

.activity-primary .timeline-icon {
  background: #eaf2ff;
  color: #2563eb;
}

.timeline small {
  display: block;
  margin-top: 5px;
}

.empty-card {
  display: grid;
  min-height: 128px;
  place-items: center;
  gap: 8px;
  border: 1px dashed #d9e4ee;
  border-radius: 8px;
  background: #fbfcfd;
  color: #64727f;
}

.empty-card .el-icon {
  font-size: 28px;
}

.status-draft.project-dot {
  background: #94a3b8;
}

.status-waiting_feedback.project-dot {
  background: #2563eb;
}

.status-change_requested.project-dot,
.status-waiting_confirm.project-dot {
  background: #b7791f;
}

.status-confirmed.project-dot {
  background: #14796d;
}

@media (max-width: 1180px) {
  .dashboard-metrics,
  .dashboard-grid,
  .lower-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 860px) {
  .dashboard-hero,
  .hero-actions {
    flex-direction: column;
  }

  .summary-strip,
  .dashboard-metrics,
  .dashboard-grid,
  .lower-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 520px) {
  .dashboard-hero {
    padding: 20px;
  }

  .hero-copy h1 {
    font-size: 28px;
  }

  .priority-item,
  .project-row {
    grid-template-columns: 1fr;
  }
}
</style>
