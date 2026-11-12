/**
 * Хранение настроек геолокации.
 *
 * Основное хранилище — IndexedDB (переживает перезапуск приложения).
 * Если IndexedDB недоступен, используется localStorage, затем — память.
 */

const DB_NAME = 'vasilev-lab';
const DB_VERSION = 1;
const STORE = 'settings';
const GEO_KEY = 'geolocation-options';
const LS_KEY = 'vasilev-lab:geolocation-options';

export const DEFAULT_GEO_OPTIONS = {
  enableHighAccuracy: true,
  timeout: 15000,
  maximumAge: 5000,
};

function clampInt(value, min, max, fallback) {
  const n = Number(value);
  if (!Number.isFinite(n)) return fallback;
  return Math.min(max, Math.max(min, Math.round(n)));
}

export function normalizeGeoOptions(raw) {
  const source = raw && typeof raw === 'object' ? raw : {};
  return {
    enableHighAccuracy:
      typeof source.enableHighAccuracy === 'boolean'
        ? source.enableHighAccuracy
        : DEFAULT_GEO_OPTIONS.enableHighAccuracy,
    timeout: clampInt(source.timeout, 1000, 120000, DEFAULT_GEO_OPTIONS.timeout),
    maximumAge: clampInt(source.maximumAge, 0, 3600000, DEFAULT_GEO_OPTIONS.maximumAge),
    savedAt: typeof source.savedAt === 'number' ? source.savedAt : null,
  };
}

function openDb() {
  return new Promise((resolve, reject) => {
    if (typeof indexedDB === 'undefined') {
      reject(new Error('IndexedDB недоступен'));
      return;
    }

    let request;
    try {
      request = indexedDB.open(DB_NAME, DB_VERSION);
    } catch (error) {
      reject(error);
      return;
    }

    request.onupgradeneeded = () => {
      const db = request.result;
      if (!db.objectStoreNames.contains(STORE)) {
        db.createObjectStore(STORE);
      }
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error || new Error('Не удалось открыть IndexedDB'));
    request.onblocked = () => reject(new Error('IndexedDB заблокирован другим окном'));
  });
}

function readFromIndexedDb() {
  return openDb().then(
    (db) =>
      new Promise((resolve, reject) => {
        const tx = db.transaction(STORE, 'readonly');
        const request = tx.objectStore(STORE).get(GEO_KEY);
        request.onsuccess = () => {
          db.close();
          resolve(request.result || null);
        };
        request.onerror = () => {
          db.close();
          reject(request.error || new Error('Ошибка чтения IndexedDB'));
        };
        tx.onabort = () => reject(tx.error || new Error('Транзакция IndexedDB прервана'));
      })
  );
}

function writeToIndexedDb(value) {
  return openDb().then(
    (db) =>
      new Promise((resolve, reject) => {
        const tx = db.transaction(STORE, 'readwrite');
        tx.objectStore(STORE).put(value, GEO_KEY);
        tx.oncomplete = () => {
          db.close();
          resolve();
        };
        tx.onerror = () => {
          db.close();
          reject(tx.error || new Error('Ошибка записи IndexedDB'));
        };
        tx.onabort = () => {
          db.close();
          reject(tx.error || new Error('Транзакция IndexedDB прервана'));
        };
      })
  );
}

export async function loadGeoOptions() {
  try {
    const stored = await readFromIndexedDb();
    return {
      options: normalizeGeoOptions(stored),
      storage: 'IndexedDB',
      error: null,
    };
  } catch (dbError) {
    try {
      const raw = window.localStorage.getItem(LS_KEY);
      return {
        options: normalizeGeoOptions(raw ? JSON.parse(raw) : null),
        storage: 'localStorage',
        error: dbError.message,
      };
    } catch (lsError) {
      return {
        options: normalizeGeoOptions(null),
        storage: 'память (не сохраняется)',
        error: `${dbError.message}; ${lsError.message}`,
      };
    }
  }
}

export async function saveGeoOptions(options) {
  const value = {
    ...normalizeGeoOptions(options),
    savedAt: Date.now(),
  };

  try {
    await writeToIndexedDb(value);
    return { storage: 'IndexedDB', savedAt: value.savedAt };
  } catch (dbError) {
    try {
      window.localStorage.setItem(LS_KEY, JSON.stringify(value));
      return { storage: 'localStorage', savedAt: value.savedAt };
    } catch {
      return { storage: 'память (не сохраняется)', savedAt: value.savedAt };
    }
  }
}

export function formatTime(timestamp) {
  if (!timestamp) return '—';
  try {
    return new Date(timestamp).toLocaleTimeString('ru-RU', { hour12: false });
  } catch {
    return new Date(timestamp).toTimeString().slice(0, 8);
  }
}

export function formatDateTime(timestamp) {
  if (!timestamp) return '—';
  try {
    return new Date(timestamp).toLocaleString('ru-RU', { hour12: false });
  } catch {
    return new Date(timestamp).toString();
  }
}
