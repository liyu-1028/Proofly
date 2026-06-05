<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getCurrentStore, updateCurrentStore, type StoreResponse } from '@/api/admin'
import { getConfigs, updateConfig, type SystemConfigResponse } from '@/api/configs'
import { useSessionStore } from '@/stores/session'
import { createOrder, getOrderStatus, getOrders, type OrderResponse } from '@/api/billing'

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

const payDialogVisible = ref(false)
const selectedMonths = ref(1)
const currentAmount = ref(29)
const paymentMethod = ref('wechat')
const creatingOrder = ref(false)
const currentOrder = ref<OrderResponse | null>(null)
const billingHistory = ref<OrderResponse[]>([])
let statusTimer: any = null

const handleMonthsChange = (val: any) => {
  const months = Number(val)
  if (months === 1) {
    currentAmount.value = 29
  } else if (months === 6) {
    currentAmount.value = 149
  } else if (months === 12) {
    currentAmount.value = 259
  }
}

const openPayDialog = () => {
  payDialogVisible.value = true
  selectedMonths.value = 1
  currentAmount.value = 29
  paymentMethod.value = 'wechat'
  currentOrder.value = null
  if (statusTimer) {
    clearInterval(statusTimer)
    statusTimer = null
  }
}

const handleCreateOrder = async () => {
  creatingOrder.value = true
  try {
    const order = await createOrder({
      durationMonths: selectedMonths.value,
      paymentMethod: paymentMethod.value,
    })
    currentOrder.value = order
    startPolling(order.orderNo)
  } catch (error: any) {
    ElMessage.error(error.message || '订单创建失败')
  } finally {
    creatingOrder.value = false
  }
}

const startPolling = (orderNo: string) => {
  if (statusTimer) clearInterval(statusTimer)
  statusTimer = setInterval(async () => {
    try {
      const statusRes = await getOrderStatus(orderNo)
      if (statusRes.isPaid) {
        clearInterval(statusTimer)
        statusTimer = null
        ElMessage.success('支付成功，已为您开通/续费 Pro 套餐！')
        payDialogVisible.value = false
        await fetchStoreAndConfigs()
        await fetchBillingHistory()
      }
    } catch (error) {
      console.error('查询订单状态失败', error)
    }
  }, 3000)
}

const goToPayPage = () => {
  if (currentOrder.value && currentOrder.value.payUrl) {
    window.open(currentOrder.value.payUrl)
  }
}

const handleClosePayDialog = (done: () => void) => {
  if (statusTimer) {
    clearInterval(statusTimer)
    statusTimer = null
  }
  done()
}

const fetchBillingHistory = async () => {
  try {
    const orders = await getOrders()
    billingHistory.value = orders
  } catch (error: any) {
    console.error('获取账单历史失败', error)
  }
}

onMounted(() => {
  fetchStoreAndConfigs()
  fetchBillingHistory()
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
          <el-button type="warning" link style="margin-left: 12px" @click="openPayDialog">
            {{ store?.planType === 'free' ? '升级高级版' : '立即续费' }}
          </el-button>
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

    <!-- 账单记录 Panel -->
    <div class="panel" style="margin-top: 24px">
      <div class="section-header">
        <h2>账单记录</h2>
        <p>查看您门店的历史升级及续费记录。</p>
      </div>

      <el-table :data="billingHistory" style="width: 100%">
        <el-table-column prop="orderNo" label="订单号" width="220" />
        <el-table-column prop="planType" label="套餐类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" type="warning">高级版 (Pro)</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="支付金额" width="100">
          <template #default="{ row }">
            ¥{{ row.amount.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="durationMonths" label="购买时长" width="100">
          <template #default="{ row }">
            {{ row.durationMonths }} 个月
          </template>
        </el-table-column>
        <el-table-column prop="paymentMethod" label="支付方式" width="120">
          <template #default="{ row }">
            {{ row.paymentMethod === 'wechat' ? '微信支付' : '支付宝' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="支付状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 'paid' ? 'success' : row.status === 'pending' ? 'info' : 'danger'">
              {{ row.status === 'paid' ? '已支付' : row.status === 'pending' ? '待支付' : '已失效' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="订单创建时间">
          <template #default="{ row }">
            {{ new Date(row.createdAt).toLocaleString() }}
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 支付弹窗 -->
    <el-dialog
      v-model="payDialogVisible"
      title="升级/续费高级套餐"
      width="480px"
      :before-close="handleClosePayDialog"
    >
      <div class="pay-dialog-content">
        <p class="pay-intro">升级高级版可享无限项目数、添加协作员工、自定义品牌 Logo 与主色调等高级专属功能。</p>
        
        <el-form label-width="80px">
          <el-form-item label="选择时长">
            <el-radio-group v-model="selectedMonths" @change="handleMonthsChange" :disabled="creatingOrder">
              <el-radio-button :value="1">1 个月</el-radio-button>
              <el-radio-button :value="6">6 个月 (9折)</el-radio-button>
              <el-radio-button :value="12">12 个月 (75折)</el-radio-button>
            </el-radio-group>
          </el-form-item>
          
          <el-form-item label="支付金额">
            <span class="pay-price">¥{{ currentAmount }}</span>
          </el-form-item>
          
          <el-form-item label="支付方式">
            <el-radio-group v-model="paymentMethod" :disabled="creatingOrder">
              <el-radio value="wechat">微信支付 (模拟)</el-radio>
              <el-radio value="alipay">支付宝 (模拟)</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>

        <div v-if="currentOrder" class="qr-box">
          <p class="qr-tip">订单已创建，请点击下方按钮前往模拟收银台支付</p>
          <el-button type="primary" size="large" @click="goToPayPage" style="width: 100%">
            前往模拟收银台付款
          </el-button>
          <p class="qr-loading"><el-icon class="is-loading"><Loading /></el-icon> 等待支付中，请勿关闭弹窗...</p>
        </div>
        
        <div v-else class="pay-action-row">
          <el-button type="primary" :loading="creatingOrder" @click="handleCreateOrder" style="width: 100%" size="large">
            立即下单
          </el-button>
        </div>
      </div>
    </el-dialog>
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

.pay-intro {
  font-size: 13px;
  color: #666;
  margin-bottom: 20px;
  line-height: 1.5;
}
.pay-price {
  font-size: 24px;
  font-weight: bold;
  color: #e76f51;
}
.qr-box {
  margin-top: 20px;
  padding: 16px;
  background: #f7f9fa;
  border-radius: 6px;
  text-align: center;
}
.qr-tip {
  font-size: 12px;
  color: #666;
  margin-bottom: 12px;
}
.qr-loading {
  font-size: 12px;
  color: #999;
  margin-top: 12px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 4px;
}
</style>
