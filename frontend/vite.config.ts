import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // In development, the browser calls /api on the Vite server (same origin),
    // and Vite forwards the request to the Spring Boot backend: no CORS needed.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
