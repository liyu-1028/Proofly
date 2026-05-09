import { request } from './http'

export interface AuditLogResponse {
  id: string
  storeId: string
  action: string
  targetType: string
  targetId: string
  operatorType: 'user' | 'customer' | 'system' | string
  operatorId?: string | null
  operatorName?: string | null
  summary: string
  extraJson?: string | null
  createdAt: string
}

export function getProjectTimeline(projectId: string) {
  return request<AuditLogResponse[]>(`/admin/projects/${projectId}/timeline`)
}
