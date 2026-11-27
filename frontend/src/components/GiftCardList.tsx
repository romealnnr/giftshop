import { useEffect, useState } from 'react'
import { fetchGiftCards } from '../api/giftCards'
import type { GiftCard } from '../types'
import { GiftCardItem } from './GiftCardItem'

/** Loads and shows the gift card catalog. */
export function GiftCardList() {
  const [giftCards, setGiftCards] = useState<GiftCard[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()

    fetchGiftCards(controller.signal)
      .then(setGiftCards)
      .catch((e: unknown) => {
        if (controller.signal.aborted) return
        setError(e instanceof Error ? e.message : 'Unexpected error')
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false)
      })

    // Cancels the request if the component disappears before the answer arrives
    return () => controller.abort()
  }, [])

  if (loading) return <p className="status">Chargement du catalogue…</p>
  if (error) return <p className="status status--error">{error}</p>

  return (
    <section className="gift-card-list">
      {giftCards.map((giftCard) => (
        <GiftCardItem key={giftCard.id} giftCard={giftCard} />
      ))}
    </section>
  )
}
