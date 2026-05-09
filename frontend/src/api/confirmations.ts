import { request } from './http'

export interface ConfirmationRecordResponse {
  id: string
  versionId: string
  customerName: string
  confirmedAt: string
}

export interface PublicConfirmationPayload {
  customerName: string
  customerContact?: string
}

export function createPublicConfirmation(token: string, payload: PublicConfirmationPayload) {
  return request<ConfirmationRecordResponse>(`/public/reviews/${token}/confirmations`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function getProjectConfirmation(projectId: string) {
  return request<ConfirmationRecordResponse | null>(`/admin/projects/${projectId}/confirmation`)
}
