<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { ApiError } from '@/api/http'
import { useSessionStore } from '@/stores/session'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()

const form = reactive({
  account: '',
  password: '',
})

const submitting = ref(false)
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
    await session.login({
      account: form.account.trim(),
      password: form.password,
    })
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/admin/dashboard'
    await router.replace(redirect)
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : '登录失败，请稍后重试'
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
          <input v-model="form.password" autocomplete="current-password" placeholder="请输入密码" type="password" />
        </label>

        <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>

        <button class="primary-button" type="submit" :disabled="!canSubmit">
          {{ submitting ? '登录中' : '登录' }}
        </button>
      </form>
    </section>
  </main>
</template>
