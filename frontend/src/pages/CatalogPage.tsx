import { GiftCardList } from '../components/GiftCardList'

export function CatalogPage() {
  return (
    <>
      <div className="page-intro">
        <h1>Offrez une carte cadeau</h1>
        <p>Choisissez une carte, et recevez son code en quelques secondes.</p>
      </div>
      <GiftCardList />
    </>
  )
}
