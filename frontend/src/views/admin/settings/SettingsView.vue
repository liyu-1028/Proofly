<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getCurrentStore, updateCurrentStore, type StoreResponse } from '@/api/admin'
import { useSessionStore } from '@/stores/session'

const session = useSessionStore()
const loading = ref(false)
const saving = ref(false)
const store = ref<StoreResponse | null>(null)

const form = ref({
  name: '',
  contactName: '',
  contactPhone: ''
})

const fetchStore = async () => {
  loading.value = true
  try {
    const data = await getCurrentStore()
    store.value = data
    form.value = {
      name: data.name,
      contactName: data.contactName || '',
      contactPhone: data.contactPhone || ''
    }
  } catch (error: any) {
    ElMessage.error(error.message || '获取门店信息失败')
  } finally {
    loading.value = false
  }
}

const handleSave = async () => {
  if (!form.value.name) {
    ElMessage.warning('门店名称不能为空')
    return
  }
  
  saving.value = true
  try {
    const data = await updateCurrentStore({
      name: form.value.name,
      contactName: form.value.contactName || undefined,
      contactPhone: form.value.contactPhone || undefined
    })
    store.value = data
    ElMessage.success('保存成功')
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
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
        <h1 class="page-title">系统设置</h1>
        <p class="page-subtitle">配置门店资料、文件上传规则和审稿链接策略。</p>
      </div>
    </header>

    <div class="panel">
      <div class="section-header">
        <h2>门店资料</h2>
      </div>

      <el-form :model="form" label-width="100px" style="max-width: 500px">
        <el-form-item label="门店 ID">
          <el-input :model-value="store?.id" disabled />
        </el-form-item>
        <el-form-item label="门店名称" required>
          <el-input v-model="form.name" placeholder="请输入门店名称" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contactName" placeholder="请输入联系人姓名" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="状态">
          <el-tag :type="store?.status === 'active' ? 'success' : 'danger'">
            {{ store?.status === 'active' ? '正常' : '禁用' }}
          </el-tag>
        </el-form-item>
        <el-form-item label="部署模式">
          <el-tag type="info">{{ store?.deploymentMode }}</el-tag>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存修改</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="panel" style="margin-top: 24px">
      <div class="section-header">
        <h2>当前账号</h2>
      </div>
      <div class="info-grid">
        <div class="info-item">
          <span class="label">用户名</span>
          <span class="value">{{ session.user?.username }}</span>
        </div>
        <div class="info-item">
          <span class="label">昵称</span>
          <span class="value">{{ session.user?.nickname }}</span>
        </div>
        <div class="info-item">
          <span class="label">角色</span>
          <span class="value">
            <el-tag v-for="role in session.user?.roles" :key="role" size="small" style="margin-right: 4px">
              {{ role }}
            </el-tag>
          </span>
        </div>
      </div>
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
.panel {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.05);
}
.section-header {
  margin-bottom: 24px;
  border-bottom: 1px solid #eee;
  padding-bottom: 12px;
}
.section-header h2 {
  font-size: 18px;
  font-weight: 600;
}
.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 24px;
}
.info-item {
  display: flex;
  flex-direction: column;
}
.info-item .label {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}
.info-item .value {
  font-size: 16px;
  font-weight: 500;
}
</style>
