const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: string
}

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly code?: number,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

export async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const { headers: initHeaders, body, ...requestInit } = init ?? {}
  
  const headers = new Headers(initHeaders)
  
  // 如果 body 不是 FormData 且未提供 Content-Type，则设置为 application/json
  if (!(body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  // 如果存在 token，则注入 Authorization 请求头
  const token = localStorage.getItem('proofly_access_token')
  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...requestInit,
    body,
    headers,
  })

  const payload = (await response.json().catch(() => null)) as ApiResponse<T> | null

  if (!response.ok || !payload || payload.code !== 0) {
    throw new ApiError(payload?.message ?? `Request failed with status ${response.status}`, response.status, payload?.code)
  }

  return payload.data
}
