<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { View, Hide } from '@element-plus/icons-vue'
import JSEncrypt from 'jsencrypt'

import { ApiError } from '@/api/http'
import { useSessionStore } from '@/stores/session'
import { getRsaPublicKey } from '@/api/configs'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()

const form = reactive({
  account: '',
  password: '',
})

const submitting = ref(false)
const passwordVisible = ref(false)
const errorMessage = ref('')

const canSubmit = computed(() => form.account.trim().length > 0 && form.password.trim().length > 0 && !submitting.value)

async function handleSubmit() {
  if (!canSubmit.value) {
    errorMessage.value = '请输入账号和密码'
    return
  }

  submitting.value = true
  errorMessage.value = ''

  try {
    // 1. 获取 RSA 公钥
    const publicKey = await getRsaPublicKey()
    
    // 2. 加密密码
    const encrypt = new JSEncrypt()
    encrypt.setPublicKey(publicKey)
    const encryptedPassword = encrypt.encrypt(form.password)
    
    if (!encryptedPassword) {
      throw new Error('加密失败')
    }

    // 3. 提交登录
    await session.login({
      account: form.account.trim(),
      password: encryptedPassword,
    })
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/admin/dashboard'
    await router.replace(redirect)
  } catch (error: any) {
    errorMessage.value = error instanceof ApiError ? error.message : (error.message || '登录失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-panel">
      <div class="auth-brand">
        <span class="brand-mark">审</span>
        <div>
          <strong>审稿宝</strong>
          <span>Proofly</span>
        </div>
      </div>

      <div>
        <h1 class="auth-title">门店后台登录</h1>
        <p class="auth-subtitle">登录后管理审稿项目、员工账号和门店配置。</p>
      </div>

      <form class="auth-form" @submit.prevent="handleSubmit">
        <label class="field">
          <span>账号</span>
          <input v-model="form.account" autocomplete="username" placeholder="请输入账号" />
        </label>

        <label class="field">
          <span>密码</span>
          <div class="password-input-wrapper">
            <input 
              v-model="form.password" 
              autocomplete="current-password" 
              placeholder="请输入密码" 
              :type="passwordVisible ? 'text' : 'password'" 
            />
            <button 
              type="button" 
              class="visibility-toggle" 
              @click="passwordVisible = !passwordVisible"
              tabindex="-1"
            >
              <el-icon><component :is="passwordVisible ? Hide : View" /></el-icon>
            </button>
          </div>
        </label>

        <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>

        <button class="primary-button" type="submit" :disabled="!canSubmit">
          {{ submitting ? '登录中' : '登录' }}
        </button>

        <div class="auth-footer">
          还没有账号？<router-link to="/register">立即免费注册</router-link>
        </div>
      </form>
    </section>
  </main>
</template>

<style scoped>
.auth-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: #666;
}
.auth-footer a {
  color: #2a9d8f;
  text-decoration: none;
  font-weight: 500;
}

.password-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.password-input-wrapper input {
  flex: 1;
  padding-right: 40px !important;
}

.visibility-toggle {
  position: absolute;
  right: 12px;
  background: none;
  border: none;
  color: #999;
  cursor: pointer;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.visibility-toggle:hover {
  color: #666;
}
</style>
