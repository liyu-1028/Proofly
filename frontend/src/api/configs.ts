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

/**
 * 获取公开品牌配置（Logo, 品牌色等）
 */
export function getBrandConfig(storeId: string) {
  return request<Record<string, string>>(`/public/configs/brand/${storeId}`)
}

/**
 * 获取上传限制配置
 */
export function getUploadLimits() {
  return request<{ maxFileSize: string }>('/public/configs/upload-limits')
}

/**
 * 获取 RSA 公钥用于加密传输敏感信息
 */
export function getRsaPublicKey() {
  return request<string>('/public/configs/rsa-public-key')
}
