import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Le proxy redirige les appels /api du frontend (port 5173) vers le
// serveur Spring Boot (port 8080). Cote navigateur, tout part de la meme
// origine : pas de souci de CORS.
export default defineConfig({
  plugins: [react()],
  server: {
    host: true, // ecoute sur toutes les interfaces : accessible depuis le reseau local
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
