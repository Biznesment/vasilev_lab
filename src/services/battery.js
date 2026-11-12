/**
 * Статус батареи (cordova-plugin-battery-status).
 *
 * События слушаются один раз на уровне приложения, поэтому история и счётчики
 * не теряются при переходе между экранами.
 */

const EVENT_TYPES = ['batterystatus', 'batterylow', 'batterycritical'];

const listeners = new Set();

let seq = 0;
let initialized = false;
let batteryManager = null;
let browserLevelHandler = null;
let browserChargingHandler = null;

let state = {
  level: null,
  plugged: null,
  events: [],
  counts: {
    batterystatus: 0,
    batterylow: 0,
    batterycritical: 0,
  },
  last: null,
  source: null,
};

function emit(patch = {}) {
  state = { ...state, ...patch };
  listeners.forEach((listener) => {
    try {
      listener(state);
    } catch (error) {
      console.error(error);
    }
  });
}

function eventLevel(event) {
  const value = typeof event?.level === 'number' ? event.level : event?.detail?.level;
  return typeof value === 'number' ? value : null;
}

function eventPlugged(event) {
  const value =
    typeof event?.isPlugged === 'boolean' ? event.isPlugged : event?.detail?.isPlugged;
  return typeof value === 'boolean' ? value : null;
}

function push(type, level, plugged, origin) {
  const numericLevel = Number(level);
  const normalizedLevel = Number.isFinite(numericLevel)
    ? Math.max(0, Math.min(100, Math.round(numericLevel)))
    : null;

  const entry = {
    id: (seq += 1),
    type,
    level: normalizedLevel,
    plugged: typeof plugged === 'boolean' ? plugged : null,
    at: Date.now(),
    origin,
  };

  emit({
    level: entry.level ?? state.level,
    plugged: entry.plugged ?? state.plugged,
    events: [entry, ...state.events].slice(0, 40),
    counts: {
      ...state.counts,
      [type]: (state.counts[type] || 0) + 1,
    },
    last: entry,
    source: origin,
  });
}

export function getBatteryState() {
  return state;
}

export function subscribeBattery(listener) {
  listeners.add(listener);
  listener(state);
  return () => listeners.delete(listener);
}

export function initBattery() {
  if (initialized) return;
  initialized = true;

  EVENT_TYPES.forEach((type) => {
    window.addEventListener(
      type,
      (event) => push(type, eventLevel(event), eventPlugged(event), 'cordova-plugin-battery-status'),
      false
    );
  });

  const hasCordova = Boolean(window.cordova);

  if (navigator.getBattery) {
    navigator
      .getBattery()
      .then((manager) => {
        batteryManager = manager;

        // Начальное значение — для отображения, пока не пришло первое событие.
        emit({
          level: state.level ?? Math.round(manager.level * 100),
          plugged: state.plugged ?? manager.charging,
          source:
            state.source ||
            (hasCordova ? 'cordova-plugin-battery-status' : 'Battery Status API (браузер)'),
        });

        if (!hasCordova) {
          browserLevelHandler = () =>
            push(
              'batterystatus',
              Math.round(manager.level * 100),
              manager.charging,
              'Battery Status API (браузер)'
            );
          browserChargingHandler = browserLevelHandler;
          manager.addEventListener('levelchange', browserLevelHandler);
          manager.addEventListener('chargingchange', browserChargingHandler);
        }
      })
      .catch(() => {
        /* Battery API недоступен — ждём события плагина */
      });
  }
}

/**
 * Демонстрация событий в браузере (в APK не используется).
 */
export function simulateBatteryEvent(type, level, plugged) {
  const event = new Event(type);
  event.level = level;
  event.isPlugged = plugged;
  window.dispatchEvent(event);
}
