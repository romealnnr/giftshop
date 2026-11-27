import { BrowserRouter, Route, Routes } from 'react-router'
import { AuthProvider } from './auth/AuthProvider'
import { RequireAuth } from './auth/RequireAuth'
import { Header } from './components/Header'
import { CatalogPage } from './pages/CatalogPage'
import { LoginPage } from './pages/LoginPage'
import { MyCardsPage } from './pages/MyCardsPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { RedeemPage } from './pages/RedeemPage'
import { RegisterPage } from './pages/RegisterPage'
import './App.css'

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Header />
        <main className="app">
          <Routes>
            <Route path="/" element={<CatalogPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route
              path="/my-cards"
              element={
                <RequireAuth>
                  <MyCardsPage />
                </RequireAuth>
              }
            />
            <Route
              path="/redeem"
              element={
                <RequireAuth>
                  <RedeemPage />
                </RequireAuth>
              }
            />
            <Route path="*" element={<NotFoundPage />} />
          </Routes>
        </main>
      </AuthProvider>
    </BrowserRouter>
  )
}

export default App
