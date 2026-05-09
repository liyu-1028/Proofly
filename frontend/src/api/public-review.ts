import { request } from './http'
import type { AnnotationResponse } from './annotations'
import type { ConfirmationRecordResponse } from './confirmations'
import type { ProjectVersionResponse } from './version'
import type { ProjectResponse } from '@/types/project'

export interface PublicProjectReviewResponse {
  project: ProjectResponse
  versions: ProjectVersionResponse[]
  activeVersionId: string
  annotations: AnnotationResponse[]
  confirmation?: ConfirmationRecordResponse | null
}

export function getPublicReview(token: string) {
  return request<PublicProjectReviewResponse>(`/public/reviews/${token}`)
}
