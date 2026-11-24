import type { GiftCard } from '../types'

/** Loads the gift card catalog (public, no authentication needed). */
export async function fetchGiftCards(signal?: AbortSignal): Promise<GiftCard[]> {
  const response = await fetch('/api/giftcards', { signal })
  if (!response.ok) {
    throw new Error(`Could not load the catalog (HTTP ${response.status})`)
  }
  return response.json()
}
