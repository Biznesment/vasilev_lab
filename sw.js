'use strict';
const VERSION = 'v2';
const APP_CACHE = `mesto-app-${VERSION}`;
const API_CACHE = `mesto-api-${VERSION}`;
const IMAGE_CACHE = `mesto-emoji-images-${VERSION}`;
const APP_ASSETS = [
  './', 'index.html', 'styles.css', 'app.js', 'manifest.webmanifest', 'log.html', 'log.js',
  'assets/icons/icon.svg', 'assets/icons/icon-192.png', 'assets/icons/icon-512.png', 'assets/icons/maskable-512.png'
];
const DB_NAME = 'mesto-sw-log';
function openLog() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, 1);
    request.onupgradeneeded = () => request.result.createObjectStore('events', { autoIncrement: true });
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}
async function log(type, detail) {
  try {
    const db = await openLog();
    await new Promise((resolve, reject) => {
      const tx = db.transaction('events', 'readwrite');
      const store = tx.objectStore('events');
      store.add({ time: new Date().toISOString(), type, detail });
      // Limit persistent storage to the latest 500 messages.
      const count = store.count();
      count.onsuccess = () => {
        let excess = count.result - 500;
        if (excess > 0) store.openCursor().onsuccess = (event) => { const cursor = event.target.result; if (cursor && excess-- > 0) { cursor.delete(); cursor.continue(); } };
      };
      tx.oncomplete = resolve; tx.onerror = () => reject(tx.error);
    });
    db.close();
  } catch (error) { console.warn('Не удалось записать журнал SW:', error); }
}
self.addEventListener('install', (event) => {
  event.waitUntil((async () => {
    try { const cache = await caches.open(APP_CACHE); await cache.addAll(APP_ASSETS); await log('install', 'Ресурсы приложения сохранены в кэш'); await self.skipWaiting(); }
    catch (error) { await log('install', `Ошибка кэширования: ${error.message}`); throw error; }
  })());
});
self.addEventListener('activate', (event) => {
  event.waitUntil((async () => {
    const keep = [APP_CACHE, API_CACHE, IMAGE_CACHE];
    const keys = await caches.keys();
    await Promise.all(keys.filter((key) => key.startsWith('mesto-') && !keep.includes(key)).map((key) => caches.delete(key)));
    await self.clients.claim(); await log('activate', 'Service Worker активирован');
  })());
});
self.addEventListener('fetch', (event) => {
  const request = event.request;
  if (request.method !== 'GET') return;
  const url = new URL(request.url);
  const isApi = url.origin === 'https://api.github.com' && url.pathname === '/emojis';
  const isEmoji = url.hostname === 'github.githubassets.com' && url.pathname.includes('/images/icons/emoji/');
  const isLocal = url.origin === self.location.origin;
  if (!isApi && !isEmoji && !isLocal) return;
  event.respondWith((async () => {
    await log('fetch', `${request.method} ${url.href}`);
    if (isApi) {
      const cache = await caches.open(API_CACHE);
      try {
        const response = await fetch(request);
        if (response.ok) { await cache.put(request, response.clone()); await log('cache', 'Обновлён список GitHub Emoji'); }
        else { const cached = await cache.match(request); if (cached) return cached; }
        return response;
      } catch (error) { const cached = await cache.match(request); if (cached) return cached; throw error; }
    }
    if (isEmoji) {
      const cache = await caches.open(IMAGE_CACHE);
      const cached = await cache.match(request);
      if (cached) return cached;
      const response = await fetch(request);
      if (response.ok || response.type === 'opaque') {
        try { await cache.put(request, response.clone()); await log('cache', `Сохранён emoji: ${url.href}`); }
        catch (error) { await log('cache', `Не удалось сохранить emoji: ${error.message}`); }
      }
      return response;
    }
    const cache = await caches.open(APP_CACHE);
    const cached = await cache.match(request, { ignoreSearch: true });
    if (cached) return cached;
    try {
      const response = await fetch(request);
      if (response.ok && url.href.startsWith(self.registration.scope)) {
        try { await cache.put(request, response.clone()); await log('cache', `Сохранён ресурс: ${url.href}`); }
        catch (error) { await log('cache', `Не удалось сохранить ресурс: ${error.message}`); }
      }
      return response;
    } catch (error) {
      if (request.mode === 'navigate') { const fallback = await cache.match('index.html'); if (fallback) return fallback; }
      throw error;
    }
  })());
});
