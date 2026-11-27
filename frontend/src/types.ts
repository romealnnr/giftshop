/** A gift card of the catalog, as returned by GET /api/giftcards. */
export interface GiftCard {
  id: number
  name: string
  category: string
  amount: number
}

/** The authenticated user, as returned by GET /api/me (never contains the password). */
export interface User {
  id: string
  email: string
  role: 'USER' | 'ADMIN'
  enabled: boolean
  createdAt: string
}

/** Returned by /api/auth/login and /api/auth/refresh. */
export interface TokenResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
}

/** A purchased gift card, as returned by /api/orders. */
export interface Order {
  id: string
  userId: string
  giftCardId: number
  giftCardName: string
  amount: number
  balance: number
  code: string
  createdAt: string
}
