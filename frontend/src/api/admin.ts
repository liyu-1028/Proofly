import { request } from './http'

export interface StoreResponse {
  id: string
  name: string
  contactName: string
  contactPhone: string
  status: 'active' | 'disabled'
  deploymentMode: string
  planType: 'free' | 'pro'
  planExpiresAt?: string
  inviteCode?: string
  createdAt: string
  updatedAt: string
}

export interface StoreUpdateRequest {
  name: string
  contactName?: string
  contactPhone?: string
}

export interface UserResponse {
  userId: string
  storeId: string
  username: string
  nickname: string
  phone: string
  email: string
  status: 'active' | 'disabled' | 'locked'
  roles: string[]
  lastLoginAt: string
  createdAt: string
  updatedAt: string
}

export interface UserCreateRequest {
  username: string
  nickname: string
  phone?: string
  email?: string
  password?: string
  roleCodes?: string[]
}

export interface UserUpdateRequest {
  nickname: string
  phone?: string
  email?: string
  roleCodes?: string[]
}

export interface UserStatusUpdateRequest {
  status: 'active' | 'disabled' | 'locked'
}

export interface UserResetPasswordRequest {
  password: string
}

export interface UserQuery {
  keyword?: string
  status?: string
}

/**
 * 获取当前门店信息
 */
export function getCurrentStore() {
  return request<StoreResponse>('/admin/stores/current')
}

/**
 * 更新当前门店信息
 */
export function updateCurrentStore(payload: StoreUpdateRequest) {
  return request<StoreResponse>('/admin/stores/current', {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

/**
 * 获取员工列表
 */
export function getUsers(query?: UserQuery) {
  const params = new URLSearchParams()
  if (query?.keyword) params.append('keyword', query.keyword)
  if (query?.status) params.append('status', query.status)
  
  const queryString = params.toString()
  const path = `/admin/users${queryString ? `?${queryString}` : ''}`
  
  return request<UserResponse[]>(path)
}

/**
 * 创建员工
 */
export function createUser(payload: UserCreateRequest) {
  return request<UserResponse>('/admin/users', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

/**
 * 获取员工详情
 */
export function getUser(userId: string) {
  return request<UserResponse>(`/admin/users/${userId}`)
}

/**
 * 更新员工信息
 */
export function updateUser(userId: string, payload: UserUpdateRequest) {
  return request<UserResponse>(`/admin/users/${userId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

/**
 * 更新员工状态
 */
export function updateUserStatus(userId: string, status: 'active' | 'disabled' | 'locked') {
  return request<UserResponse>(`/admin/users/${userId}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  })
}

/**
 * 重置员工密码
 */
export function resetUserPassword(userId: string, payload: UserResetPasswordRequest) {
  return request<null>(`/admin/users/${userId}/reset-password`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}
