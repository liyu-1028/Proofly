import { request } from './http'

export interface SystemConfigResponse {
  id: string
  storeId: string | null
  configKey: string
  configValue: string
  valueType: 'string' | 'number' | 'boolean' | 'json'
  description: string
  createdAt: string
  updatedAt: string
}

export interface SystemConfigUpdateRequest {
  configValue: string
  description?: string
}

/**
 * 获取所有配置项（合并门店与全局）
 */
export function getConfigs() {
  return request<SystemConfigResponse[]>('/admin/configs')
}

/**
 * 更新或创建门店覆盖配置
 */
export function updateConfig(key: string, payload: SystemConfigUpdateRequest) {
  return request<SystemConfigResponse>(`/admin/configs/${key}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}
