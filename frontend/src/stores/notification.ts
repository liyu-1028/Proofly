import { defineStore } from 'pinia'
import { ref } from 'vue'
import SockJS from 'sockjs-client'
import Stomp from 'stompjs'
import { getUnreadCount, getNotifications, markAsRead, markAllAsRead } from '@/api/notifications'
import type { NotificationResponse } from '@/api/notifications'
import { useSessionStore } from './session'

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)
  const notifications = ref<NotificationResponse[]>([])
  const loading = ref(false)
  const stompClient = ref<Stomp.Client | null>(null)

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
      await fetchUnreadCount()
    } catch (e) {
      console.error('Failed to fetch notifications', e)
    } finally {
      loading.value = false
    }
  }

  const connectWebSocket = () => {
    const sessionStore = useSessionStore()
    if (!sessionStore.user?.userId || stompClient.value?.connected) return

    // 在本地开发中，后端通常运行在 8080 端口，或与前端代理相同的端口
    // 假设已配置 Vite 代理或使用相对路径
    const socket = new SockJS('/ws-notifications')
    stompClient.value = Stomp.over(socket)
    
    // 禁用调试日志以使控制台更整洁
    stompClient.value.debug = () => {}

    stompClient.value.connect({}, () => {
      console.log('WebSocket Connected')
      
      // 订阅个人通知队列
      stompClient.value?.subscribe(`/user/${sessionStore.user?.userId}/queue/notifications`, (message) => {
        const notification: NotificationResponse = JSON.parse(message.body)
        // 添加到列表并增加计数
        notifications.value.unshift(notification)
        unreadCount.value++
        
        // 可选：触发浏览器通知或消息提示
      })
    }, (error) => {
      console.error('WebSocket Error:', error)
      // 5 秒后尝试重新连接
      setTimeout(connectWebSocket, 5000)
    })
  }

  const disconnectWebSocket = () => {
    if (stompClient.value) {
      stompClient.value.disconnect(() => {
        console.log('WebSocket Disconnected')
      })
      stompClient.value = null
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
    connectWebSocket,
    disconnectWebSocket,
    readOne,
    readAll
  }
})
