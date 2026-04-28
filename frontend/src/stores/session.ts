import { defineStore } from 'pinia'

export interface SessionUser {
  id: string
  name: string
  storeId: string
}

export const useSessionStore = defineStore('session', {
  state: () => ({
    user: null as SessionUser | null,
  }),
  actions: {
    setUser(user: SessionUser | null) {
      this.user = user
    },
  },
})
