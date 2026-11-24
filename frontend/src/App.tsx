import { GiftCardList } from './components/GiftCardList'
import './App.css'

function App() {
  return (
    <div className="app">
      <header className="app__header">
        <h1>Giftshop</h1>
        <p>Get a gift card in just a few seconds.</p>
      </header>
      <main>
        <GiftCardList />
      </main>
    </div>
  )
}

export default App
