import { Link } from 'react-router'

export function NotFoundPage() {
  return (
    <div className="page-intro">
      <h1>Page introuvable</h1>
      <p>
        <Link to="/">Retour au catalogue</Link>
      </p>
    </div>
  )
}
