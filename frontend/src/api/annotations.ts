import { request } from './http'

export type AnnotationType = 'point' | 'rect'
export type AnnotationStatus = 'open' | 'resolved' | 'ignored'

export interface AnnotationResponse {
  id: string
  type: AnnotationType
  xRatio: number
  yRatio: number
  widthRatio?: number | null
  heightRatio?: number | null
  content: string
  mediaUrl?: string | null
  mediaDuration?: number | null
  customerName?: string | null
  status: AnnotationStatus
  createdAt: string
  resolvedByNickname?: string | null
  resolvedAt?: string | null
}

export interface PublicAnnotationPayload {
  type: AnnotationType
  xRatio: number
  yRatio: number
  widthRatio?: number
  heightRatio?: number
  content: string
  mediaUrl?: string
  mediaDuration?: number
  customerName?: string
  customerContact?: string
}

export function createPublicAnnotation(token: string, payload: PublicAnnotationPayload) {
  return request<AnnotationResponse>(`/public/reviews/${token}/annotations`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function listProjectVersionAnnotations(projectId: string, versionId: string) {
  return request<AnnotationResponse[]>(`/admin/projects/${projectId}/versions/${versionId}/annotations`)
}

export function updateAnnotationStatus(
  projectId: string,
  versionId: string,
  annotationId: string,
  status: Exclude<AnnotationStatus, 'open'>,
) {
  return request<void>(
    `/admin/projects/${projectId}/versions/${versionId}/annotations/${annotationId}/status?status=${status}`,
    {
      method: 'PATCH',
    },
  )
}
