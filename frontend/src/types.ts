/** A gift card of the catalog, as returned by GET /api/giftcards. */
export interface GiftCard {
  id: number
  name: string
  category: string
  amount: number
}
