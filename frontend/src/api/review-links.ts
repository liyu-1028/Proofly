import { request } from './http'

export interface ReviewLinkResponse {
  id: string
  projectId: string
  currentVersionId: string | null
  token?: string | null
  status: 'active' | 'disabled' | 'expired'
  expiresAt: string | null
  maxAccessCount: number | null
  accessCount: number
  lastAccessAt: string | null
  url?: string | null
  createdAt: string
}

export interface ReviewLinkCreatePayload {
  expiresAt?: string
  maxAccessCount?: number
}

export function listReviewLinks(projectId: string) {
  return request<ReviewLinkResponse[]>(`/admin/projects/${projectId}/review-links`)
}

export function createReviewLink(projectId: string, payload: ReviewLinkCreatePayload) {
  return request<ReviewLinkResponse>(`/admin/projects/${projectId}/review-links`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function disableReviewLink(linkId: string) {
  return request<null>(`/admin/review-links/${linkId}/disable`, {
    method: 'PATCH',
  })
}

export function enableReviewLink(linkId: string) {
  return request<null>(`/admin/review-links/${linkId}/enable`, {
    method: 'PATCH',
  })
}

export function deleteReviewLink(linkId: string) {
  return request<null>(`/admin/review-links/${linkId}`, {
    method: 'DELETE',
  })
}
