import { request } from './http'
import type { ProjectResponse } from '@/types/project'

export interface AuditLogResponse {
  id: string
  action: string
  targetType: string
  targetId: string
  operatorType: string
  operatorName: string
  summary: string
  createdAt: string
}

export interface DashboardStatsResponse {
  statusCounts: Record<string, number>
  totalProjects: number
  recentProjects: ProjectResponse[]
  recentActivities: AuditLogResponse[]
}

/**
 * 获取工作台统计数据
 */
export function getDashboardStats() {
  return request<DashboardStatsResponse>('/admin/dashboard/stats')
}
