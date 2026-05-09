import { request } from './http'

import type { ProjectListQuery, ProjectPayload, ProjectResponse } from '@/types/project'

function toQueryString(query: ProjectListQuery) {
  const params = new URLSearchParams()
  if (query.keyword?.trim()) {
    params.set('keyword', query.keyword.trim())
  }
  if (query.status) {
    params.set('status', query.status)
  }
  if (query.ownerUserId) {
    params.set('ownerUserId', String(query.ownerUserId))
  }
  const value = params.toString()
  return value ? `?${value}` : ''
}

export function listProjects(query: ProjectListQuery = {}) {
  return request<ProjectResponse[]>(`/admin/projects${toQueryString(query)}`)
}

export function createProject(payload: ProjectPayload) {
  return request<ProjectResponse>('/admin/projects', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function getProject(projectId: string) {
  return request<ProjectResponse>(`/admin/projects/${projectId}`)
}

export function updateProject(projectId: string, payload: ProjectPayload) {
  return request<ProjectResponse>(`/admin/projects/${projectId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function archiveProject(projectId: string) {
  return request<ProjectResponse>(`/admin/projects/${projectId}/archive`, {
    method: 'PATCH',
  })
}

export function restoreProject(projectId: string) {
  return request<ProjectResponse>(`/admin/projects/${projectId}/restore`, {
    method: 'PATCH',
  })
}
