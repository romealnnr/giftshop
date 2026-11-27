import type { GiftCard } from '../types'
import { apiFetch } from './client'

/** Loads the gift card catalog (public, no authentication needed). */
export function fetchGiftCards(signal?: AbortSignal): Promise<GiftCard[]> {
  return apiFetch<GiftCard[]>('/api/giftcards', { signal })
}
