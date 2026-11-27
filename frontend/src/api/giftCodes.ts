import type { GiftCodeBalance, RedemptionResult } from '../types'
import { apiFetch } from './client'

// The code is a bearer secret: it travels in the request body, never in the URL,
// so it does not end up in the browser history or in server access logs.

export function checkBalance(code: string): Promise<GiftCodeBalance> {
  return apiFetch<GiftCodeBalance>('/api/gift-codes/balance', {
    method: 'POST',
    body: JSON.stringify({ code }),
  })
}

export function redeem(code: string, amount: number): Promise<RedemptionResult> {
  return apiFetch<RedemptionResult>('/api/gift-codes/redeem', {
    method: 'POST',
    body: JSON.stringify({ code, amount }),
  })
}
