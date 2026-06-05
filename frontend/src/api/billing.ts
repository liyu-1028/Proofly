import { request } from './http'

export interface OrderCreateRequest {
  durationMonths: number
  paymentMethod?: string
}

export interface OrderResponse {
  id: string
  storeId: string
  orderNo: string
  planType: string
  amount: number
  durationMonths: number
  status: 'pending' | 'paid' | 'failed' | 'expired'
  paymentMethod: string
  payUrl: string | null
  paidAt: string | null
  createdAt: string
}

export interface OrderStatusResponse {
  orderNo: string
  status: string
  isPaid: boolean
}

/**
 * 创建套餐订单
 */
export function createOrder(payload: OrderCreateRequest) {
  return request<OrderResponse>('/admin/billing/orders', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

/**
 * 查询订单支付状态
 */
export function getOrderStatus(orderNo: string) {
  return request<OrderStatusResponse>(`/admin/billing/orders/${orderNo}/status`)
}

/**
 * 查询账单/订单历史列表
 */
export function getOrders() {
  return request<OrderResponse[]>('/admin/billing/orders')
}
