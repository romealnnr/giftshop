import { useState } from 'react'
import { formatDate, formatPrice } from '../format'
import type { Order } from '../types'

export function MyCardItem({ order, isNew }: { order: Order; isNew: boolean }) {
  // The code is a bearer secret: hidden by default, except for the card just bought
  const [revealed, setRevealed] = useState(isNew)

  return (
    <li className={isNew ? 'my-card my-card--new' : 'my-card'}>
      <div className="my-card__header">
        <h2>{order.giftCardName}</h2>
        {isNew && <span className="my-card__badge">Nouvelle</span>}
      </div>

      <p className="my-card__balance">
        {formatPrice(order.balance)}
        <span> restants sur {formatPrice(order.amount)}</span>
      </p>

      <div className="my-card__code">
        <code>{revealed ? order.code : '•••• •••• ••••'}</code>
        <button
          type="button"
          className="button button--secondary"
          aria-pressed={revealed}
          onClick={() => setRevealed((r) => !r)}
        >
          {revealed ? 'Masquer' : 'Afficher le code'}
        </button>
      </div>

      <p className="my-card__date">Achetée le {formatDate(order.createdAt)}</p>
    </li>
  )
}
