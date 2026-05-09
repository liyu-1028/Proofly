<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getDashboardStats, type DashboardStatsResponse } from '@/api/dashboard'
import type { ProjectStatus } from '@/types/project'

const statusLabels: Record<ProjectStatus, string> = {
  draft: '草稿',
  waiting_feedback: '待反馈',
  change_requested: '需修改',
  waiting_confirm: '待确认',
  confirmed: '已确认',
  archived: '已归档',
}

const loading = ref(false)
const stats = ref<DashboardStatsResponse | null>(null)

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

onMounted(() => {
  loadStats()
})
</script>

<template>
  <section class="page" v-loading="loading">
    <header class="page-header">
      <div>
        <h1 class="page-title">工作台</h1>
        <p class="page-subtitle">集中查看审稿项目状态、客户反馈和确认进度。</p>
      </div>
    </header>

    <div class="metric-grid">
      <div class="metric">
        <span>全部项目</span>
        <strong>{{ stats?.totalProjects ?? 0 }}</strong>
      </div>
      <div class="metric">
        <span>待客户反馈</span>
        <strong>{{ stats?.statusCounts?.waiting_feedback ?? 0 }}</strong>
      </div>
      <div class="metric">
        <span>需修改</span>
        <strong>{{ stats?.statusCounts?.change_requested ?? 0 }}</strong>
      </div>
      <div class="metric">
        <span>待确认</span>
        <strong>{{ stats?.statusCounts?.waiting_confirm ?? 0 }}</strong>
      </div>
      <div class="metric">
        <span>已确认</span>
        <strong>{{ stats?.statusCounts?.confirmed ?? 0 }}</strong>
      </div>
    </div>

    <div class="dashboard-grid">
      <div class="panel">
        <div class="section-header">
          <h2>最近项目</h2>
          <RouterLink to="/admin/projects" class="text-button">查看全部</RouterLink>
        </div>
        <div class="panel-body">
          <ul v-if="stats?.recentProjects.length" class="activity-list">
            <li v-for="project in stats.recentProjects" :key="project.id">
              <div class="activity-info">
                <RouterLink :to="`/admin/projects/${project.id}`" class="activity-title">{{ project.name }}</RouterLink>
                <span class="activity-time">{{ project.customerName || '匿名客户' }}</span>
              </div>
              <span class="status-pill" :class="`status-${project.status}`">
                {{ statusLabels[project.status as ProjectStatus] }}
              </span>
            </li>
          </ul>
          <p v-else class="empty-text">暂无项目</p>
        </div>
      </div>

      <div class="panel">
        <div class="section-header">
          <h2>近期动态</h2>
        </div>
        <div class="panel-body">
          <ul v-if="stats?.recentActivities.length" class="activity-list">
            <li v-for="log in stats.recentActivities" :key="log.id">
              <div class="activity-info">
                <span class="activity-title">{{ log.summary }}</span>
                <span class="activity-time">{{ formatTime(log.createdAt) }} · {{ log.operatorName }}</span>
              </div>
            </li>
          </ul>
          <p v-else class="empty-text">暂无动态</p>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.dashboard-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-top: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-header h2 {
  font-size: 18px;
  font-weight: 600;
}

.activity-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.activity-list li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #eee;
}

.activity-list li:last-child {
  border-bottom: none;
}

.activity-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.activity-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  text-decoration: none;
}

.activity-title:hover {
  color: var(--el-color-primary);
}

.activity-time {
  font-size: 12px;
  color: #999;
}

.empty-text {
  text-align: center;
  color: #999;
  padding: 24px 0;
}

@media (max-width: 768px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}
</style>
