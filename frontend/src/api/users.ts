import { request } from './http'

export interface UserResponse {
  userId: number
  storeId: number
  username: string
  nickname: string
  phone: string | null
  email: string | null
  status: string
  roles: string[]
  lastLoginAt: string | null
  createdAt: string
  updatedAt: string
}

function authHeaders(accessToken: string) {
  return {
    Authorization: `Bearer ${accessToken}`,
  }
}

export function listUsers(accessToken: string, keyword = '', status = 'active') {
  const params = new URLSearchParams()
  if (keyword.trim()) {
    params.set('keyword', keyword.trim())
  }
  if (status) {
    params.set('status', status)
  }
  const query = params.toString()
  return request<UserResponse[]>(`/admin/users${query ? `?${query}` : ''}`, {
    headers: authHeaders(accessToken),
  })
}
