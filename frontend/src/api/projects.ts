import { request } from './http'

import type { ProjectListQuery, ProjectPayload, ProjectResponse } from '@/types/project'

function authHeaders(accessToken: string) {
  return {
    Authorization: `Bearer ${accessToken}`,
  }
}

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

export function listProjects(accessToken: string, query: ProjectListQuery = {}) {
  return request<ProjectResponse[]>(`/admin/projects${toQueryString(query)}`, {
    headers: authHeaders(accessToken),
  })
}

export function createProject(accessToken: string, payload: ProjectPayload) {
  return request<ProjectResponse>('/admin/projects', {
    method: 'POST',
    headers: authHeaders(accessToken),
    body: JSON.stringify(payload),
  })
}

export function getProject(accessToken: string, projectId: number) {
  return request<ProjectResponse>(`/admin/projects/${projectId}`, {
    headers: authHeaders(accessToken),
  })
}

export function updateProject(accessToken: string, projectId: number, payload: ProjectPayload) {
  return request<ProjectResponse>(`/admin/projects/${projectId}`, {
    method: 'PUT',
    headers: authHeaders(accessToken),
    body: JSON.stringify(payload),
  })
}

export function archiveProject(accessToken: string, projectId: number) {
  return request<ProjectResponse>(`/admin/projects/${projectId}/archive`, {
    method: 'PATCH',
    headers: authHeaders(accessToken),
  })
}

export function restoreProject(accessToken: string, projectId: number) {
  return request<ProjectResponse>(`/admin/projects/${projectId}/restore`, {
    method: 'PATCH',
    headers: authHeaders(accessToken),
  })
}
