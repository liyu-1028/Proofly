import { defineStore } from 'pinia'

import * as authApi from '@/api/auth'

const ACCESS_TOKEN_KEY = 'proofly_access_token'
const REFRESH_TOKEN_KEY = 'proofly_refresh_token'
const ACCESS_EXPIRES_KEY = 'proofly_access_token_expires_at'
const REFRESH_EXPIRES_KEY = 'proofly_refresh_token_expires_at'

export const useSessionStore = defineStore('session', {
  state: () => ({
    user: null as authApi.SessionUser | null,
    accessToken: localStorage.getItem(ACCESS_TOKEN_KEY),
    refreshToken: localStorage.getItem(REFRESH_TOKEN_KEY),
    accessTokenExpiresAt: localStorage.getItem(ACCESS_EXPIRES_KEY),
    refreshTokenExpiresAt: localStorage.getItem(REFRESH_EXPIRES_KEY),
    initialized: false,
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.accessToken && state.user),
    displayName: (state) => state.user?.nickname || state.user?.username || '未登录',
  },
  actions: {
    applyAuth(response: authApi.AuthResponse) {
      this.user = response.user
      this.accessToken = response.accessToken
      this.refreshToken = response.refreshToken
      this.accessTokenExpiresAt = response.accessTokenExpiresAt
      this.refreshTokenExpiresAt = response.refreshTokenExpiresAt

      localStorage.setItem(ACCESS_TOKEN_KEY, response.accessToken)
      localStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken)
      localStorage.setItem(ACCESS_EXPIRES_KEY, response.accessTokenExpiresAt)
      localStorage.setItem(REFRESH_EXPIRES_KEY, response.refreshTokenExpiresAt)
    },
    clearSession() {
      this.user = null
      this.accessToken = null
      this.refreshToken = null
      this.accessTokenExpiresAt = null
      this.refreshTokenExpiresAt = null
      localStorage.removeItem(ACCESS_TOKEN_KEY)
      localStorage.removeItem(REFRESH_TOKEN_KEY)
      localStorage.removeItem(ACCESS_EXPIRES_KEY)
      localStorage.removeItem(REFRESH_EXPIRES_KEY)
    },
    async login(payload: authApi.LoginPayload) {
      const response = await authApi.login(payload)
      this.applyAuth(response)
    },
    async fetchCurrentUser() {
      if (!this.accessToken) {
        throw new Error('缺少访问令牌')
      }
      this.user = await authApi.me(this.accessToken)
    },
    async refreshSession() {
      if (!this.refreshToken) {
        throw new Error('缺少刷新令牌')
      }
      const response = await authApi.refresh(this.refreshToken)
      this.applyAuth(response)
    },
    async initialize() {
      if (this.initialized) {
        return
      }

      try {
        if (this.accessToken) {
          await this.fetchCurrentUser()
        } else if (this.refreshToken) {
          await this.refreshSession()
        }
      } catch {
        try {
          await this.refreshSession()
        } catch {
          this.clearSession()
        }
      } finally {
        this.initialized = true
      }
    },
    async logout() {
      const token = this.accessToken
      this.clearSession()
      if (token) {
        await authApi.logout(token).catch(() => undefined)
      }
    },
  },
})
