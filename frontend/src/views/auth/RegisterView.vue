<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/auth'
import prooflyLogo from '@/assets/proofly-logo.svg'

const router = useRouter()
const loading = ref(false)

const form = reactive({
  phone: '',
  password: '',
  nickname: '',
  storeName: '',
  inviteCode: ''
})

const handleRegister = async () => {
  if (!form.phone || !form.password || !form.nickname || !form.storeName) {
    ElMessage.warning('请填写完整注册信息')
    return
  }

  loading.value = true
  try {
    await register({
      phone: form.phone,
      password: form.password,
      nickname: form.nickname,
      storeName: form.storeName,
      inviteCode: form.inviteCode || undefined
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (error: any) {
    ElMessage.error(error.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="register-page">
    <div class="register-card">
      <header class="register-header">
        <img :src="prooflyLogo" alt="Proofly Logo" class="logo" />
        <h1>加入审稿宝</h1>
        <p>让您的设计沟通更高效</p>
      </header>

      <el-form :model="form" label-position="top" size="large" @keyup.enter="handleRegister">
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="设置密码">
          <el-input v-model="form.password" type="password" show-password placeholder="6-20位密码" />
        </el-form-item>
        <el-form-item label="您的姓名">
          <el-input v-model="form.nickname" placeholder="例如：设计师张三" />
        </el-form-item>
        <el-form-item label="门店名称">
          <el-input v-model="form.storeName" placeholder="例如：极速打印店" />
        </el-form-item>
        <el-form-item label="邀请码 (可选)">
          <el-input v-model="form.inviteCode" placeholder="如果有邀请码请填写" />
        </el-form-item>

        <div class="actions">
          <el-button type="primary" :loading="loading" class="submit-btn" @click="handleRegister">
            立即开启免费版
          </el-button>
        </div>

        <div class="footer-links">
          已有账号？<router-link to="/login">立即登录</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f7fa;
  padding: 20px;
}
.register-card {
  width: 100%;
  max-width: 440px;
  background: #fff;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.05);
}
.register-header {
  text-align: center;
  margin-bottom: 32px;
}
.logo {
  width: 64px;
  height: 64px;
  margin-bottom: 16px;
}
.register-header h1 {
  font-size: 24px;
  font-weight: 600;
  color: #14213d;
  margin-bottom: 8px;
}
.register-header p {
  color: #666;
  font-size: 14px;
}
.submit-btn {
  width: 100%;
  margin-top: 12px;
  height: 48px;
  font-size: 16px;
  background-color: #2a9d8f;
  border-color: #2a9d8f;
}
.submit-btn:hover {
  background-color: #21867a;
  border-color: #21867a;
}
.footer-links {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: #666;
}
.footer-links a {
  color: #2a9d8f;
  text-decoration: none;
  font-weight: 500;
}
</style>
