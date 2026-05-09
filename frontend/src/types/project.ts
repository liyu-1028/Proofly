export type ProjectStatus =
  | 'draft'
  | 'waiting_feedback'
  | 'change_requested'
  | 'waiting_confirm'
  | 'confirmed'
  | 'archived'

export interface ProjectResponse {
  id: number
  storeId: number
  name: string
  customerName: string | null
  customerContact: string | null
  ownerUserId: number
  ownerNickname: string | null
  status: ProjectStatus
  currentVersionId: number | null
  confirmedVersionId: number | null
  remark: string | null
  archivedAt: string | null
  createdAt: string
  createdByNickname: string | null
  updatedAt: string
}

export interface ProjectPayload {
  name: string
  customerName?: string
  customerContact?: string
  ownerUserId: number
  remark?: string
}

export interface ProjectListQuery {
  keyword?: string
  status?: ProjectStatus | ''
  ownerUserId?: number | ''
}
