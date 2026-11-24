import type { GiftCard } from '../types'
import { formatPrice } from '../format'

interface Props {
  giftCard: GiftCard
}

/** One gift card of the catalog. */
export function GiftCardItem({ giftCard }: Props) {
  return (
    <article className="gift-card">
      <span className="gift-card__category">{giftCard.category}</span>
      <h2 className="gift-card__name">{giftCard.name}</h2>
      <p className="gift-card__amount">{formatPrice(giftCard.amount)}</p>
    </article>
  )
}
