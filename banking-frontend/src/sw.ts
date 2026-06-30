/// <reference lib="webworker" />
import { precacheAndRoute, createHandlerBoundToURL } from 'workbox-precaching';
import { NavigationRoute, registerRoute } from 'workbox-routing';

declare const self: ServiceWorkerGlobalScope & { __WB_MANIFEST: Array<unknown> };

// Precache des assets generes au build (coquille de l'app, offline).
precacheAndRoute(self.__WB_MANIFEST as never);

// Navigation SPA : sert index.html, sauf pour l'API et l'actuator (toujours reseau).
const handler = createHandlerBoundToURL('index.html');
registerRoute(new NavigationRoute(handler, { denylist: [/^\/api/, /^\/actuator/] }));

// Mise a jour immediate du SW (mode autoUpdate).
self.addEventListener('install', () => {
  self.skipWaiting();
});
self.addEventListener('activate', (event: ExtendableEvent) => {
  event.waitUntil(self.clients.claim());
});

// Reception d'un push -> affichage d'une notification systeme (pop-up).
self.addEventListener('push', (event: PushEvent) => {
  let data: { title?: string; body?: string } = {};
  try {
    data = event.data ? event.data.json() : {};
  } catch {
    data = {};
  }
  const title = data.title || 'Ma Banque';
  const body = data.body || '';
  event.waitUntil(
    self.registration.showNotification(title, {
      body,
      icon: '/pwa-192x192.png',
      badge: '/pwa-192x192.png',
      tag: 'ma-banque',
    }),
  );
});

// Clic sur la notification -> focus de l'app (ou ouverture).
self.addEventListener('notificationclick', (event: NotificationEvent) => {
  event.notification.close();
  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clients) => {
      for (const client of clients) {
        if ('focus' in client) {
          return client.focus();
        }
      }
      return self.clients.openWindow('/');
    }),
  );
});
