import { request } from './http'

export interface UserResponse {
  userId: string
  storeId: string
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

export interface UserCreatePayload {
  username: string
  nickname: string
  phone?: string
  email?: string
  password: string
  roleCodes: string[]
}

function authHeaders(accessToken: string) {
  return {
    Authorization: `Bearer ${accessToken}`,
  }
}

export function createUser(accessToken: string, payload: UserCreatePayload) {
  return request<UserResponse>('/admin/users', {
    method: 'POST',
    headers: authHeaders(accessToken),
    body: JSON.stringify(payload),
  })
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
