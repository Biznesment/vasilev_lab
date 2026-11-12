/**
 * Единый сервис геолокации.
 *
 * watchPosition запускается/останавливается одной кнопкой и продолжает работу
 * при переходах между экранами приложения (состояние хранится в модуле,
 * а не в React-компоненте).
 */

import { DEFAULT_GEO_OPTIONS } from './settings';

const MAX_LOG_ENTRIES = 50;

const listeners = new Set();
let logSeq = 0;

let state = {
  watching: false,
  watchId: null,
  source: null,
  appliedOptions: null,
  startedAt: null,
  updates: 0,
  lastPosition: null,
  lastError: null,
  log: [],
};

function emit(patch = {}) {
  state = { ...state, ...patch };
  listeners.forEach((listener) => {
    try {
      listener(state);
    } catch (error) {
      // ошибка подписчика не должна ломать watchPosition
      console.error(error);
    }
  });
}

function formatCoord(value) {
  return Number.isFinite(value) ? Number(value).toFixed(6) : '—';
}

function addLog(entry) {
  const item = { id: (logSeq += 1), at: Date.now(), ...entry };
  emit({ log: [item, ...state.log].slice(0, MAX_LOG_ENTRIES) });
}

export function getGeoSourceName() {
  return typeof window !== 'undefined' && window.cordova
    ? 'cordova-plugin-geolocation'
    : 'navigator.geolocation (браузер)';
}

export function getGeoState() {
  return state;
}

export function subscribeGeo(listener) {
  listeners.add(listener);
  listener(state);
  return () => listeners.delete(listener);
}

export function clearGeoLog() {
  emit({ log: [] });
}

export function startWatch(options) {
  if (state.watching) {
    return { ok: false, reason: 'already-watching' };
  }

  if (typeof navigator === 'undefined' || !navigator.geolocation) {
    const message = 'navigator.geolocation недоступен на этом устройстве';
    emit({ lastError: { code: null, message, at: Date.now() } });
    addLog({ kind: 'error', text: message });
    return { ok: false, reason: 'unsupported' };
  }

  const appliedOptions = {
    enableHighAccuracy:
      typeof options?.enableHighAccuracy === 'boolean'
        ? options.enableHighAccuracy
        : DEFAULT_GEO_OPTIONS.enableHighAccuracy,
    timeout: Number(options?.timeout ?? DEFAULT_GEO_OPTIONS.timeout),
    maximumAge: Number(options?.maximumAge ?? DEFAULT_GEO_OPTIONS.maximumAge),
  };

  const source = getGeoSourceName();

  emit({
    watching: true,
    source,
    appliedOptions,
    startedAt: Date.now(),
    watchId: null,
    lastError: null,
  });

  addLog({
    kind: 'start',
    text:
      `Старт watchPosition · enableHighAccuracy=${appliedOptions.enableHighAccuracy ? 'true' : 'false'}` +
      ` · timeout=${appliedOptions.timeout} мс · maximumAge=${appliedOptions.maximumAge} мс`,
  });

  let watchId;
  try {
    watchId = navigator.geolocation.watchPosition(
      (position) => {
        if (!state.watching) return;
        const coords = position.coords || {};
        const lastPosition = {
          latitude: coords.latitude,
          longitude: coords.longitude,
          accuracy: coords.accuracy,
          altitude: coords.altitude,
          altitudeAccuracy: coords.altitudeAccuracy,
          heading: coords.heading,
          speed: coords.speed,
          timestamp: position.timestamp || Date.now(),
        };

        emit({
          lastPosition,
          updates: state.updates + 1,
          lastError: null,
        });

        addLog({
          kind: 'position',
          text:
            `Позиция #${state.updates}: ${formatCoord(lastPosition.latitude)}, ` +
            `${formatCoord(lastPosition.longitude)} · точность ±${formatCoord(lastPosition.accuracy)} м`,
        });
      },
      (error) => {
        if (!state.watching) return;
        const lastError = {
          code: typeof error?.code === 'number' ? error.code : null,
          message: error?.message || errorCodeLabel(error?.code),
          at: Date.now(),
        };
        emit({ lastError });
        addLog({
          kind: 'error',
          text: `Ошибка ${lastError.code ?? '—'}: ${lastError.message}`,
        });
      },
      appliedOptions
    );
  } catch (error) {
    emit({
      watching: false,
      startedAt: null,
      watchId: null,
      lastError: { code: null, message: error.message, at: Date.now() },
    });
    addLog({ kind: 'error', text: `Не удалось вызвать watchPosition: ${error.message}` });
    return { ok: false, reason: error.message };
  }

  emit({ watchId });
  return { ok: true, watchId };
}

export function stopWatch() {
  const wasWatching = state.watching;

  if (state.watchId !== null && typeof navigator !== 'undefined' && navigator.geolocation) {
    try {
      navigator.geolocation.clearWatch(state.watchId);
    } catch (error) {
      console.error('clearWatch failed', error);
    }
  }

  emit({ watching: false, watchId: null, startedAt: null });

  if (wasWatching) {
    addLog({ kind: 'stop', text: 'watchPosition остановлен' });
  }
}

export function errorCodeLabel(code) {
  switch (code) {
    case 1:
      return 'PERMISSION_DENIED: доступ к геолокации запрещён';
    case 2:
      return 'POSITION_UNAVAILABLE: позиция недоступна';
    case 3:
      return 'TIMEOUT: истекло время ожидания';
    default:
      return 'неизвестная ошибка';
  }
}
