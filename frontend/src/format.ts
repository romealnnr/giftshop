const euros = new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' })

/** 25 -> "25,00 €" */
export function formatPrice(amount: number): string {
  return euros.format(amount)
}

const dateFormatter = new Intl.DateTimeFormat('fr-FR', { dateStyle: 'long', timeStyle: 'short' })

/** Formats an ISO 8601 date from the API, in the user's time zone: "27 novembre 2026 à 14:57". */
export function formatDate(iso: string): string {
  return dateFormatter.format(new Date(iso))
}
