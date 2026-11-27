import { Link, NavLink, useNavigate } from 'react-router'
import { useAuth } from '../auth/useAuth'

export function Header() {
  const { user, status, logout } = useAuth()
  const navigate = useNavigate()

  async function handleLogout() {
    await logout()
    navigate('/')
  }

  return (
    <header className="header">
      <Link to="/" className="header__brand">
        Giftshop
      </Link>
      <nav className="header__nav">
        <NavLink to="/" end>
          Catalogue
        </NavLink>
        {status === 'authenticated' && user && (
          <>
            <NavLink to="/my-cards">Mes cartes</NavLink>
            <span className="header__user">{user.email}</span>
            <button type="button" className="button button--secondary" onClick={handleLogout}>
              Se déconnecter
            </button>
          </>
        )}
        {status === 'anonymous' && (
          <>
            <NavLink to="/login">Se connecter</NavLink>
            <NavLink to="/register">Créer un compte</NavLink>
          </>
        )}
      </nav>
    </header>
  )
}
