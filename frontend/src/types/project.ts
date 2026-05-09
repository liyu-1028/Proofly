export type ProjectStatus =
  | 'draft'
  | 'waiting_feedback'
  | 'change_requested'
  | 'waiting_confirm'
  | 'confirmed'
  | 'archived'

export interface ProjectResponse {
  id: string
  storeId: string
  name: string
  customerName: string | null
  customerContact: string | null
  ownerUserId: string
  ownerNickname: string | null
  status: ProjectStatus
  currentVersionId: string | null
  confirmedVersionId: string | null
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
  ownerUserId: string
  remark?: string
}

export interface ProjectListQuery {
  keyword?: string
  status?: ProjectStatus | ''
  ownerUserId?: string
}
