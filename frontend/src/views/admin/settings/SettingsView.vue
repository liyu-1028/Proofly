<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getCurrentStore, updateCurrentStore, type StoreResponse } from '@/api/admin'
import { getConfigs, updateConfig, type SystemConfigResponse } from '@/api/configs'
import { useSessionStore } from '@/stores/session'

const session = useSessionStore()
const loading = ref(false)
const saving = ref(false)
const configSaving = ref<Record<string, boolean>>({})
const store = ref<StoreResponse | null>(null)
const configs = ref<SystemConfigResponse[]>([])

const form = ref({
  name: '',
  contactName: '',
  contactPhone: ''
})

const brandForm = ref({
  logoUrl: '',
  primaryColor: '#2a9d8f'
})

const fetchStoreAndConfigs = async () => {
  loading.value = true
  try {
    const [storeData, configData] = await Promise.all([
      getCurrentStore(),
      getConfigs()
    ])
    store.value = storeData
    form.value = {
      name: storeData.name,
      contactName: storeData.contactName || '',
      contactPhone: storeData.contactPhone || ''
    }
    configs.value = configData
    
    // 提取品牌信息
    brandForm.value.logoUrl = configData.find(c => c.configKey === 'brand.logo_url')?.configValue || ''
    brandForm.value.primaryColor = configData.find(c => c.configKey === 'brand.primary_color')?.configValue || '#2a9d8f'
  } catch (error: any) {
    ElMessage.error(error.message || '获取信息失败')
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

const handleSaveConfig = async (cfg: SystemConfigResponse) => {
  configSaving.value[cfg.configKey] = true
  try {
    await updateConfig(cfg.configKey, {
      configValue: cfg.configValue,
      description: cfg.description
    })
    ElMessage.success(`配置 [${cfg.configKey}] 已更新`)
  } catch (error: any) {
    ElMessage.error(error.message || '配置更新失败')
  } finally {
    configSaving.value[cfg.configKey] = false
  }
}

const handleSaveBrandConfig = async (key: string, value: string) => {
  if (store.value?.planType !== 'pro') {
    ElMessage.warning('自定义品牌功能仅限高级版使用')
    return
  }
  
  try {
    await updateConfig(key, { configValue: value })
    ElMessage.success('品牌配置已更新')
  } catch (error: any) {
    ElMessage.error(error.message || '更新失败')
  }
}

onMounted(() => {
  fetchStoreAndConfigs()
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
        <el-form-item label="当前套餐">
          <el-tag :type="store?.planType === 'pro' ? 'warning' : 'info'">
            {{ store?.planType === 'pro' ? '高级版 (Pro)' : '免费版 (Free)' }}
          </el-tag>
          <span v-if="store?.planExpiresAt" style="margin-left: 12px; font-size: 13px; color: #999">
            有效期至：{{ new Date(store.planExpiresAt).toLocaleDateString() }}
          </span>
          <el-button v-if="store?.planType === 'free'" type="warning" link style="margin-left: 12px">升级高级版</el-button>
        </el-form-item>
        <el-form-item label="门店邀请码">
          <code style="font-weight: bold; color: #2a9d8f">{{ store?.inviteCode }}</code>
          <p style="margin: 4px 0 0; font-size: 12px; color: #999">邀请好友注册，双方均可获得高级版奖励。</p>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存修改</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="panel" style="margin-top: 24px">
      <div class="section-header">
        <h2>品牌设置</h2>
        <p>配置您的专属 Logo 和品牌主色调，提升专业形象（仅限高级版）。</p>
      </div>
      
      <el-form label-width="120px" style="max-width: 600px">
        <el-form-item label="品牌 Logo">
          <div class="logo-setting">
            <el-input v-model="brandForm.logoUrl" placeholder="Logo 图片 URL" style="margin-bottom: 8px">
              <template #append>
                <el-button @click="handleSaveBrandConfig('brand.logo_url', brandForm.logoUrl)">保存</el-button>
              </template>
            </el-input>
            <div v-if="brandForm.logoUrl" class="logo-preview">
              <img :src="brandForm.logoUrl" alt="Logo Preview" />
            </div>
          </div>
        </el-form-item>
        <el-form-item label="品牌色 (Primary)">
          <div style="display: flex; gap: 12px; align-items: center">
            <el-color-picker v-model="brandForm.primaryColor" @change="handleSaveBrandConfig('brand.primary_color', brandForm.primaryColor)" />
            <span>{{ brandForm.primaryColor }}</span>
          </div>
        </el-form-item>
      </el-form>
    </div>

    <div class="panel" style="margin-top: 24px">
      <div class="section-header">
        <h2>业务配置</h2>
        <p>配置上传文件大小、允许类型以及系统业务行为。</p>
      </div>

      <el-table :data="configs" style="width: 100%">
        <el-table-column prop="configKey" label="配置项" width="200">
          <template #default="{ row }">
            <code>{{ row.configKey }}</code>
            <div style="font-size: 12px; color: #999">{{ row.description }}</div>
          </template>
        </el-table-column>
        <el-table-column label="配置值">
          <template #default="{ row }">
            <el-input v-model="row.configValue" placeholder="请输入配置值">
              <template #append v-if="row.valueType === 'number' && row.configKey.includes('size')">MB</template>
            </el-input>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              link 
              :loading="configSaving[row.configKey]"
              @click="handleSaveConfig(row)"
            >
              保存
            </el-button>
          </template>
        </el-table-column>
      </el-table>
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
.logo-preview {
  margin-top: 12px;
  padding: 8px;
  border: 1px dashed #ccc;
  border-radius: 4px;
  max-width: 200px;
}
.logo-preview img {
  max-width: 100%;
  height: auto;
}
</style>
