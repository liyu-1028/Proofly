import { request } from './http'

export interface NotificationResponse {
  id: string
  receiverUserId: string
  projectId: string | null
  type: string
  title: string
  content: string
  readAt: string | null
  createdAt: string
}

/**
 * 获取通知列表
 */
export function getNotifications() {
  return request<NotificationResponse[]>('/admin/notifications')
}

/**
 * 获取未读数量
 */
export function getUnreadCount() {
  return request<number>('/admin/notifications/unread-count')
}

/**
 * 标记为已读
 */
export function markAsRead(id: string) {
  return request<void>(`/admin/notifications/${id}/read`, {
    method: 'PATCH'
  })
}

/**
 * 全部标记为已读
 */
export function markAllAsRead() {
  return request<void>('/admin/notifications/mark-all-read', {
    method: 'POST'
  })
}
