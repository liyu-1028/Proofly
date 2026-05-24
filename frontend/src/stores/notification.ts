import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUnreadCount, getNotifications, markAsRead, markAllAsRead } from '@/api/notifications'
import type { NotificationResponse } from '@/api/notifications'

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)
  const notifications = ref<NotificationResponse[]>([])
  const loading = ref(false)

  const fetchUnreadCount = async () => {
    try {
      const count = await getUnreadCount()
      unreadCount.value = count
    } catch (e) {
      console.error('Failed to fetch unread count', e)
    }
  }

  const fetchNotifications = async () => {
    loading.value = true
    try {
      const list = await getNotifications()
      notifications.value = list
      // After listing, we might want to refresh unread count too
      await fetchUnreadCount()
    } catch (e) {
      console.error('Failed to fetch notifications', e)
    } finally {
      loading.value = false
    }
  }

  const readOne = async (id: string) => {
    try {
      await markAsRead(id)
      const found = notifications.value.find(n => n.id === id)
      if (found && !found.readAt) {
        found.readAt = new Date().toISOString()
        if (unreadCount.value > 0) unreadCount.value--
      }
    } catch (e) {
      console.error('Failed to mark notification as read', e)
    }
  }

  const readAll = async () => {
    try {
      await markAllAsRead()
      notifications.value.forEach(n => {
        if (!n.readAt) n.readAt = new Date().toISOString()
      })
      unreadCount.value = 0
    } catch (e) {
      console.error('Failed to mark all as read', e)
    }
  }

  return {
    unreadCount,
    notifications,
    loading,
    fetchUnreadCount,
    fetchNotifications,
    readOne,
    readAll
  }
})
