import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router'
import { createOrder } from '../api/orders'
import { useAuth } from '../auth/useAuth'

export function BuyButton({ giftCardId }: { giftCardId: number }) {
  const { status } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [buying, setBuying] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleClick() {
    // Anonymous visitors log in first, then come back to the catalog
    if (status !== 'authenticated') {
      navigate('/login', { state: { from: location.pathname } })
      return
    }

    setError(null)
    setBuying(true)
    try {
      const order = await createOrder(giftCardId)
      navigate('/my-cards', { state: { purchasedOrderId: order.id } })
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Unexpected error')
    } finally {
      setBuying(false)
    }
  }

  return (
    <>
      <button
        type="button"
        className="button"
        onClick={handleClick}
        disabled={buying || status === 'loading'}
      >
        {buying ? 'Achat…' : 'Acheter'}
      </button>
      {error && (
        <p className="form__error" role="alert">
          {error}
        </p>
      )}
    </>
  )
}
