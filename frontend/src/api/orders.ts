import type { Order } from '../types'
import { apiFetch } from './client'

/** Buys a gift card: the backend generates its unique code. */
export function createOrder(giftCardId: number): Promise<Order> {
  return apiFetch<Order>('/api/orders', {
    method: 'POST',
    body: JSON.stringify({ giftCardId }),
  })
}

/** The gift cards bought by the logged-in user (the backend filters by owner). */
export function fetchMyOrders(signal?: AbortSignal): Promise<Order[]> {
  return apiFetch<Order[]>('/api/orders', { signal })
}
