import { request } from './http'

export interface SessionUser {
  userId: string
  storeId: string
  username: string
  nickname: string
  phone: string
  status: string
  roles: string[]
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  accessTokenExpiresAt: string
  refreshTokenExpiresAt: string
  user: SessionUser
}

export interface LoginPayload {
  account: string
  password: string
}

export interface RegisterRequest {
  phone: string
  password: string
  nickname: string
  storeName: string
  inviteCode?: string
}

export function login(payload: LoginPayload) {
  return request<AuthResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function register(payload: RegisterRequest) {
  return request<void>('/auth/register', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function refresh(refreshToken: string) {
  return request<AuthResponse>('/auth/refresh', {
    method: 'POST',
    body: JSON.stringify({ refreshToken }),
  })
}

export function logout(accessToken: string) {
  return request<null>('/auth/logout', {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  })
}

export function me(accessToken: string) {
  return request<SessionUser>('/auth/me', {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  })
}
