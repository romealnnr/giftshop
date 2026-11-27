import { useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router'
import { fetchMyOrders } from '../api/orders'
import { MyCardItem } from '../components/MyCardItem'
import type { Order } from '../types'

export function MyCardsPage() {
  const location = useLocation()
  const purchasedOrderId = (location.state as { purchasedOrderId?: string } | null)
    ?.purchasedOrderId

  const [orders, setOrders] = useState<Order[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    fetchMyOrders(controller.signal)
      .then(setOrders)
      .catch((e: unknown) => {
        if (controller.signal.aborted) return
        setError(e instanceof Error ? e.message : 'Unexpected error')
      })
    return () => controller.abort()
  }, [])

  return (
    <>
      <div className="page-intro">
        <h1>Mes cartes</h1>
        {purchasedOrderId && (
          <p className="notice" role="status">
            Achat confirmé ! Le code de ta nouvelle carte est affiché ci-dessous.
          </p>
        )}
      </div>

      {error && (
        <p className="form__error" role="alert">
          {error}
        </p>
      )}

      {!error && orders === null && <p>Chargement…</p>}

      {orders?.length === 0 && (
        <p>
          Tu n'as pas encore de carte. <Link to="/">Découvre le catalogue</Link>
        </p>
      )}

      {orders && orders.length > 0 && (
        <ul className="my-cards">
          {orders.map((order) => (
            <MyCardItem key={order.id} order={order} isNew={order.id === purchasedOrderId} />
          ))}
        </ul>
      )}
    </>
  )
}
