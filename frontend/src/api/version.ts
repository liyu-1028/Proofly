import { request } from './http'

export interface ProjectVersionResponse {
  id: string
  storeId: string
  projectId: string
  versionNo: number
  versionName: string
  fileId: string
  originalFilename: string
  fileExt: string
  fileSize: number
  previewUrl: string
  description: string
  isCurrent: boolean
  isConfirmed: boolean
  confirmedAt: string | null
  createdAt: string
  uploadedBy: string
  uploaderNickname: string
  annotationCount?: number
  hasVoice?: boolean
}

/**
 * List all versions for a project.
 */
export function listVersions(projectId: string) {
  return request<ProjectVersionResponse[]>(`/admin/projects/${projectId}/versions`)
}

/**
 * Upload a new version for a project.
 */
export function uploadVersion(projectId: string, file: File, description?: string) {
  const formData = new FormData()
  formData.append('file', file)
  if (description) {
    formData.append('description', description)
  }

  return request<ProjectVersionResponse>(`/admin/projects/${projectId}/versions`, {
    method: 'POST',
    body: formData,
    // Note: Fetch handles boundary automatically when body is FormData
    // but our http.ts helper might try to set JSON content-type.
    // We need to check http.ts.
  })
}
