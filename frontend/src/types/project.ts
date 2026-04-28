export type ProjectStatus =
  | 'draft'
  | 'waiting_feedback'
  | 'change_requested'
  | 'waiting_confirm'
  | 'confirmed'
  | 'archived'

export interface ProjectSummary {
  id: string
  name: string
  customerName: string
  status: ProjectStatus
  ownerName: string
  updatedAt: string
}
