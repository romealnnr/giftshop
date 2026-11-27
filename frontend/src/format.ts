const euros = new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' })

/** 25 -> "25,00 €" */
export function formatPrice(amount: number): string {
  return euros.format(amount)
}
