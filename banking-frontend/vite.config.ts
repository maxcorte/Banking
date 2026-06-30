import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'

// Le proxy redirige les appels /api du frontend (port 5173) vers le
// serveur Spring Boot (port 8080). Cote navigateur, tout part de la meme
// origine : pas de souci de CORS.
export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['favicon-32x32.png', 'apple-touch-icon.png'],
      manifest: {
        name: 'Ma Banque',
        short_name: 'Ma Banque',
        description: 'Application bancaire de démonstration',
        lang: 'fr',
        theme_color: '#2563eb',
        background_color: '#0f172a',
        display: 'standalone',
        start_url: '/',
        scope: '/',
        icons: [
          { src: 'pwa-192x192.png', sizes: '192x192', type: 'image/png' },
          { src: 'pwa-512x512.png', sizes: '512x512', type: 'image/png' },
          {
            src: 'pwa-maskable-512x512.png',
            sizes: '512x512',
            type: 'image/png',
            purpose: 'maskable',
          },
        ],
      },
      workbox: {
        // Le service worker ne doit jamais intercepter l'API ni l'actuator :
        // ces requetes doivent toujours partir au reseau.
        navigateFallback: '/index.html',
        navigateFallbackDenylist: [/^\/api/, /^\/actuator/],
        globPatterns: ['**/*.{js,css,html,ico,png,svg,webmanifest}'],
      },
    }),
  ],
  server: {
    host: true, // ecoute sur toutes les interfaces : accessible depuis le reseau local
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
