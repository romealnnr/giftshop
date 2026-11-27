import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { useAuth } from '../auth/useAuth'

export function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await register(email, password)
      navigate('/')
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Unexpected error')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form className="form" onSubmit={handleSubmit}>
      <h1>Créer un compte</h1>

      <label className="form__field">
        Adresse e-mail
        <input
          type="email"
          autoComplete="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
      </label>

      <label className="form__field">
        Mot de passe
        <input
          type="password"
          autoComplete="new-password"
          required
          minLength={12}
          maxLength={72}
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        <span className="form__hint">
          Au moins 12 caractères. Une phrase de plusieurs mots est à la fois sûre et facile à retenir.
        </span>
      </label>

      {error && (
        <p className="form__error" role="alert">
          {error}
        </p>
      )}

      <button type="submit" className="button" disabled={submitting}>
        {submitting ? 'Création…' : 'Créer mon compte'}
      </button>

      <p className="form__footer">
        Déjà un compte ? <Link to="/login">Se connecter</Link>
      </p>
    </form>
  )
}
