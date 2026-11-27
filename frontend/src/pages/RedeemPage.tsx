import { useEffect, useState, type FormEvent } from 'react'
import { ApiError } from '../api/client'
import { checkBalance, redeem } from '../api/giftCodes'
import { formatPrice } from '../format'
import type { GiftCodeBalance, RedemptionResult } from '../types'

/** Same rules as the backend: at least 0.01, up to 8 digits and 2 decimals. */
const AMOUNT_PATTERN = /^\d{1,8}([.,]\d{1,2})?$/

/** Accepts "12,50" (French keyboard) as well as "12.50". */
function parseAmount(input: string): number | null {
  const trimmed = input.trim()
  if (!AMOUNT_PATTERN.test(trimmed)) {
    return null
  }
  const value = Number(trimmed.replace(',', '.'))
  return value >= 0.01 ? value : null
}

export function RedeemPage() {
  const [code, setCode] = useState('')
  const [card, setCard] = useState<GiftCodeBalance | null>(null)
  const [amount, setAmount] = useState('')
  const [result, setResult] = useState<RedemptionResult | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  // Rate limiting (429): attempts are blocked until this time (ms since epoch)
  const [blockedUntil, setBlockedUntil] = useState<number | null>(null)
  const [now, setNow] = useState(0)

  useEffect(() => {
    if (blockedUntil === null) return
    const timer = setInterval(() => {
      const current = Date.now()
      setNow(current)
      if (current >= blockedUntil) {
        setBlockedUntil(null)
      }
    }, 1000)
    return () => clearInterval(timer)
  }, [blockedUntil])

  const secondsLeft = blockedUntil === null ? 0 : Math.max(0, Math.ceil((blockedUntil - now) / 1000))
  const blocked = secondsLeft > 0

  function handleError(e: unknown) {
    if (!(e instanceof ApiError)) {
      setError('Erreur inattendue. Réessaie plus tard.')
      return
    }
    switch (e.status) {
      case 429: {
        const current = Date.now()
        setNow(current)
        setBlockedUntil(current + (e.retryAfterSeconds ?? 60) * 1000)
        setError("Trop d'essais. Par sécurité, patiente avant de réessayer.")
        return
      }
      case 404:
        setError('Ce code est invalide.')
        return
      case 409:
        setError('Le solde de cette carte est insuffisant.')
        return
      default:
        setError(e.message)
    }
  }

  async function handleCheck(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setCard(null)
    setResult(null)
    setBusy(true)
    try {
      setCard(await checkBalance(code.trim()))
    } catch (e) {
      handleError(e)
    } finally {
      setBusy(false)
    }
  }

  async function handleRedeem(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!card) return

    const value = parseAmount(amount)
    if (value === null) {
      setError('Montant invalide : par exemple 12,50.')
      return
    }

    setError(null)
    setResult(null)
    setBusy(true)
    try {
      const redemption = await redeem(code.trim(), value)
      setResult(redemption)
      setCard({ ...card, balance: redemption.remainingBalance })
      setAmount('')
    } catch (e) {
      handleError(e)
    } finally {
      setBusy(false)
    }
  }

  function handleCodeChange(value: string) {
    // A new code: forget everything about the previous one
    setCode(value)
    setCard(null)
    setResult(null)
    setError(null)
  }

  const submitLabel = (label: string) => (blocked ? `Réessayer dans ${secondsLeft} s` : label)

  return (
    <div className="form">
      <h1>Utiliser un code</h1>

      <form className="form" onSubmit={handleCheck}>
        <label className="form__field">
          Code de la carte
          <input
            type="text"
            autoComplete="off"
            spellCheck={false}
            required
            maxLength={30}
            value={code}
            onChange={(e) => handleCodeChange(e.target.value)}
          />
        </label>
        <button type="submit" className="button" disabled={busy || blocked}>
          {submitLabel(busy && !card ? 'Vérification…' : 'Vérifier le solde')}
        </button>
      </form>

      {card && (
        <div className="balance-summary">
          <p className="balance-summary__name">{card.giftCardName}</p>
          <p className="balance-summary__balance">
            {formatPrice(card.balance)}
            <span> restants sur {formatPrice(card.amount)}</span>
          </p>
        </div>
      )}

      {result && (
        <p className="notice" role="status">
          {formatPrice(result.redeemedAmount)} débités. Nouveau solde :{' '}
          {formatPrice(result.remainingBalance)}.
        </p>
      )}

      {card && card.balance > 0 && (
        <form className="form" onSubmit={handleRedeem}>
          <label className="form__field">
            Montant à dépenser (€)
            <input
              type="text"
              inputMode="decimal"
              autoComplete="off"
              required
              placeholder="12,50"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
            />
          </label>
          <button type="submit" className="button" disabled={busy || blocked}>
            {submitLabel(busy ? 'Paiement…' : 'Dépenser')}
          </button>
        </form>
      )}

      {error && (
        <p className="form__error" role="alert">
          {error}
        </p>
      )}
    </div>
  )
}
