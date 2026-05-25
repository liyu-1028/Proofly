<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getCurrentStore } from '@/api/admin'
import { CopyDocument, Share } from '@element-plus/icons-vue'

const loading = ref(false)
const store = ref<any>(null)

const referralUrl = computed(() => {
  if (!store.value?.inviteCode) return ''
  const origin = window.location.origin
  return `${origin}/register?code=${store.value.inviteCode}`
})

async function fetchStore() {
  loading.value = true
  try {
    store.value = await getCurrentStore()
  } catch (err: any) {
    ElMessage.error(err.message || '获取信息失败')
  } finally {
    loading.value = false
  }
}

async function copyToClipboard(text: string) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch (err) {
    ElMessage.error('复制失败，请手动选择复制')
  }
}

onMounted(() => {
  fetchStore()
})
</script>

<template>
  <section class="page" v-loading="loading">
    <header class="page-header">
      <div>
        <h1 class="page-title">推荐奖励</h1>
        <p class="page-subtitle">邀请同行好友使用审稿宝，双方均可获得高级版套餐奖励。</p>
      </div>
    </header>

    <div class="referral-hero">
      <div class="hero-content">
        <h2>每成功邀请一位好友，双方各领 30 天 Pro 版</h2>
        <p>好友通过您的链接注册并完成首次设计稿确认后，系统将自动为您和好友延长套餐有效期。</p>
        
        <div class="invite-card">
          <div class="invite-label">您的专属邀请链接</div>
          <div class="invite-link-box">
            <span class="url">{{ referralUrl }}</span>
            <el-button type="primary" :icon="CopyDocument" @click="copyToClipboard(referralUrl)">复制链接</el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card">
        <div class="label">累计邀请成功</div>
        <div class="value">0</div>
      </div>
      <div class="stat-card">
        <div class="label">累计奖励时长</div>
        <div class="value">0 天</div>
      </div>
    </div>

    <div class="panel" style="margin-top: 24px">
      <div class="section-header">
        <h2>奖励说明</h2>
      </div>
      <ul class="rules">
        <li><strong>如何参与：</strong>复制您的专属链接发送给同行朋友。</li>
        <li><strong>奖励触发：</strong>受邀好友完成注册，并为其客户完成首次“项目确认”操作。</li>
        <li><strong>奖励内容：</strong>您和好友将分别获得 30 天 Pro 套餐奖励，奖励次数不设上限。</li>
        <li><strong>注意事项：</strong>同一受邀方仅能被推荐一次，且需为首次注册。</li>
      </ul>
    </div>
  </section>
</template>

<style scoped>
.page {
  padding: 24px;
}
.page-header {
  margin-bottom: 24px;
}
.page-title {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 8px;
}
.page-subtitle {
  color: #666;
  font-size: 14px;
}

.referral-hero {
  background: linear-gradient(135deg, #2a9d8f 0%, #264653 100%);
  border-radius: 12px;
  padding: 48px;
  color: #fff;
  margin-bottom: 24px;
}

.hero-content h2 {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 16px;
}

.hero-content p {
  font-size: 16px;
  opacity: 0.9;
  margin-bottom: 32px;
  max-width: 600px;
}

.invite-card {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  padding: 20px;
  max-width: 600px;
}

.invite-label {
  font-size: 13px;
  margin-bottom: 12px;
  opacity: 0.8;
}

.invite-link-box {
  display: flex;
  gap: 12px;
  background: #fff;
  border-radius: 4px;
  padding: 4px 4px 4px 12px;
  align-items: center;
}

.invite-link-box .url {
  color: #264653;
  font-size: 14px;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.stats-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.stat-card {
  background: #fff;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.05);
  text-align: center;
}

.stat-card .label {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.stat-card .value {
  font-size: 32px;
  font-weight: 700;
  color: #2a9d8f;
}

.panel {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.05);
}

.section-header {
  margin-bottom: 16px;
  border-bottom: 1px solid #eee;
  padding-bottom: 12px;
}

.rules {
  padding-left: 20px;
  line-height: 2;
  color: #444;
}
</style>
