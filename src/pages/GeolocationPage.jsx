import React, { useEffect, useRef, useState } from 'react';
import { Button, Segment, Switch } from 'react-onsenui';
import ons from 'onsenui';

import InfoRow from '../components/InfoRow';
import Icon from '../components/Icons';
import {
  clearGeoLog,
  getGeoSourceName,
  getGeoState,
  startWatch,
  stopWatch,
  subscribeGeo,
} from '../services/geolocation';
import {
  DEFAULT_GEO_OPTIONS,
  formatDateTime,
  formatTime,
  loadGeoOptions,
  normalizeGeoOptions,
  saveGeoOptions,
} from '../services/settings';

function clampInt(value, min, max, fallback) {
  const n = Number(value);
  if (!Number.isFinite(n)) return fallback;
  return Math.min(max, Math.max(min, Math.round(n)));
}

function fmtNumber(value, digits = 6) {
  return Number.isFinite(value) ? Number(value).toFixed(digits) : '—';
}

function formatElapsed(ms) {
  const total = Math.max(0, Math.floor(ms / 1000));
  const minutes = Math.floor(total / 60);
  const seconds = total % 60;
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
}

function logKindLabel(kind) {
  switch (kind) {
    case 'start':
      return 'старт';
    case 'stop':
      return 'стоп';
    case 'position':
      return 'позиция';
    case 'error':
      return 'ошибка';
    default:
      return kind;
  }
}

function SaveIndicator({ saveState, storage, savedAt, storageError }) {
  const isError = saveState === 'error' || String(storage).includes('память');
  const saving = saveState === 'saving';

  let text;
  if (saving) text = 'Сохранение…';
  else if (isError) text = `Хранилище: ${storage}`;
  else text = `Сохранено в ${storage}`;

  return (
    <div className={`save-indicator ${isError ? 'save-indicator--warn' : ''}`}>
      <span className={`dot ${saving ? 'dot--amber' : isError ? 'dot--red' : 'dot--green'}`} />
      <span>
        {text}
        {savedAt ? ` · ${formatTime(savedAt)}` : ''}
      </span>
      {storageError && <span className="save-indicator__error">({storageError})</span>}
    </div>
  );
}

export default function GeolocationPage() {
  const [tab, setTab] = useState('options');
  const [options, setOptions] = useState(null);
  const [timeoutText, setTimeoutText] = useState('');
  const [maxAgeText, setMaxAgeText] = useState('');
  const [storage, setStorage] = useState('—');
  const [savedAt, setSavedAt] = useState(null);
  const [saveState, setSaveState] = useState('idle');
  const [storageError, setStorageError] = useState(null);
  const [geo, setGeo] = useState(getGeoState);
  const [, setTick] = useState(0);

  const loadedRef = useRef(false);

  // загрузка сохранённых опций
  useEffect(() => {
    let alive = true;

    loadGeoOptions().then((result) => {
      if (!alive) return;
      loadedRef.current = true;
      setOptions(result.options);
      setTimeoutText(String(result.options.timeout));
      setMaxAgeText(String(result.options.maximumAge));
      setStorage(result.storage);
      setSavedAt(result.options.savedAt);
      setStorageError(result.error);
      setSaveState(result.error ? 'idle' : 'saved');
    });

    return () => {
      alive = false;
    };
  }, []);

  // автосохранение при изменении опций
  useEffect(() => {
    if (!loadedRef.current || !options) return undefined;

    setSaveState('saving');
    const timer = window.setTimeout(() => {
      saveGeoOptions(options)
        .then((result) => {
          setStorage(result.storage);
          setSavedAt(result.savedAt);
          setSaveState('saved');
          setStorageError(null);
        })
        .catch((error) => {
          setSaveState('error');
          setStorageError(error.message);
        });
    }, 350);

    return () => window.clearTimeout(timer);
  }, [options]);

  // состояние сервиса watchPosition
  useEffect(() => subscribeGeo(setGeo), []);

  // обновляем «время работы» раз в секунду
  useEffect(() => {
    if (!geo.watching) return undefined;
    const timer = window.setInterval(() => setTick((value) => value + 1), 1000);
    return () => window.clearInterval(timer);
  }, [geo.watching]);

  const updateOption = (patch) => {
    setOptions((prev) => (prev ? { ...prev, ...patch, savedAt: null } : prev));
  };

  const handleTimeoutInput = (raw) => {
    setTimeoutText(raw);
    const value = Number(raw);
    if (Number.isFinite(value) && value >= 1000 && value <= 120000) {
      updateOption({ timeout: Math.round(value) });
    }
  };

  const handleTimeoutBlur = () => {
    const value = clampInt(timeoutText, 1000, 120000, options?.timeout ?? DEFAULT_GEO_OPTIONS.timeout);
    updateOption({ timeout: value });
    setTimeoutText(String(value));
  };

  const handleMaxAgeInput = (raw) => {
    setMaxAgeText(raw);
    const value = Number(raw);
    if (Number.isFinite(value) && value >= 0 && value <= 3600000) {
      updateOption({ maximumAge: Math.round(value) });
    }
  };

  const handleMaxAgeBlur = () => {
    const value = clampInt(
      maxAgeText,
      0,
      3600000,
      options?.maximumAge ?? DEFAULT_GEO_OPTIONS.maximumAge
    );
    updateOption({ maximumAge: value });
    setMaxAgeText(String(value));
  };

  const resetOptions = () => {
    const defaults = normalizeGeoOptions(DEFAULT_GEO_OPTIONS);
    setOptions(defaults);
    setTimeoutText(String(defaults.timeout));
    setMaxAgeText(String(defaults.maximumAge));
    ons.notification.toast('Настройки сброшены к значениям по умолчанию', { timeout: 1800 });
  };

  const toggleWatch = () => {
    if (geo.watching) {
      stopWatch();
      return;
    }

    const result = startWatch(options || DEFAULT_GEO_OPTIONS);
    if (!result.ok) {
      ons.notification.toast('Не удалось запустить watchPosition', { timeout: 2500 });
    }
  };

  const elapsed = geo.watching && geo.startedAt ? Date.now() - geo.startedAt : 0;
  const position = geo.lastPosition;

  return (
    <div className="page-scroll">
      <div className="segment-wrap">
        <Segment
          activeIndex={tab === 'options' ? 0 : 1}
          onPostChange={(event) => setTab(event.activeIndex === 0 ? 'options' : 'diagnostics')}
        >
          <button type="button" onClick={() => setTab('options')}>
            Опции
          </button>
          <button type="button" onClick={() => setTab('diagnostics')}>
            Диагностика
          </button>
        </Segment>
      </div>

      {tab === 'options' && (
        <>
          <div className="card">
            <div className="card__title">Опции watchPosition</div>

            {!options ? (
              <div className="empty">Загрузка настроек…</div>
            ) : (
              <>
                <div className="setting">
                  <div className="setting__text">
                    <div className="setting__label">enableHighAccuracy</div>
                    <div className="setting__hint">
                      Приоритет точности (GPS). Точнее, но расходует больше энергии.
                    </div>
                  </div>
                  <Switch
                    checked={options.enableHighAccuracy}
                    onChange={(event) =>
                      updateOption({ enableHighAccuracy: Boolean(event.target.checked) })
                    }
                  />
                </div>

                <div className="setting setting--column">
                  <label className="setting__label" htmlFor="geo-timeout">
                    timeout, мс
                  </label>
                  <input
                    id="geo-timeout"
                    className="num-input"
                    type="number"
                    inputMode="numeric"
                    min="1000"
                    max="120000"
                    step="1000"
                    value={timeoutText}
                    onInput={(event) => handleTimeoutInput(event.target.value)}
                    onBlur={handleTimeoutBlur}
                  />
                  <div className="setting__hint">
                    Максимальное время ожидания позиции: 1 000 – 120 000 мс.
                  </div>
                </div>

                <div className="setting setting--column">
                  <label className="setting__label" htmlFor="geo-maxage">
                    maximumAge, мс
                  </label>
                  <input
                    id="geo-maxage"
                    className="num-input"
                    type="number"
                    inputMode="numeric"
                    min="0"
                    max="3600000"
                    step="1000"
                    value={maxAgeText}
                    onInput={(event) => handleMaxAgeInput(event.target.value)}
                    onBlur={handleMaxAgeBlur}
                  />
                  <div className="setting__hint">
                    Сколько можно использовать закэшированную позицию: 0 – 3 600 000 мс.
                  </div>
                </div>

                <SaveIndicator
                  saveState={saveState}
                  storage={storage}
                  savedAt={savedAt}
                  storageError={storageError}
                />
              </>
            )}
          </div>

          <div className="note">
            Опции сохраняются в IndexedDB и автоматически загружаются после перезапуска
            приложения. Изменения применяются при следующем запуске watchPosition.
          </div>

          <div className="actions">
            <Button className="btn-secondary" onClick={resetOptions} disabled={!options}>
              Сбросить настройки
            </Button>
          </div>
        </>
      )}

      {tab === 'diagnostics' && (
        <>
          <div className="card">
            <div className="status-line">
              <span className={`dot ${geo.watching ? 'dot--green dot--pulse' : 'dot--gray'}`} />
              <strong>{geo.watching ? 'watchPosition активен' : 'watchPosition остановлен'}</strong>
            </div>
            <div className="muted small">
              Источник: {geo.source || getGeoSourceName()}
              {geo.watching ? ` · работает ${formatElapsed(elapsed)}` : ''}
              {geo.updates > 0 ? ` · обновлений: ${geo.updates}` : ''}
            </div>

            <button
              type="button"
              className={`watch-btn ${geo.watching ? 'watch-btn--stop' : ''}`}
              onClick={toggleWatch}
              disabled={!options}
            >
              <Icon name={geo.watching ? 'stop' : 'play'} size={20} />
              {geo.watching ? 'Остановить watchPosition' : 'Запустить watchPosition'}
            </button>

            <div className="muted small watch-btn__hint">
              Кнопка работает как переключатель. Наблюдение продолжается при переходе на другие
              экраны приложения.
            </div>
          </div>

          <div className="card">
            <div className="card__title">Последняя позиция</div>
            {position ? (
              <>
                <InfoRow label="Широта" value={fmtNumber(position.latitude)} mono />
                <InfoRow label="Долгота" value={fmtNumber(position.longitude)} mono />
                <InfoRow
                  label="Точность"
                  value={`± ${fmtNumber(position.accuracy, 1)} м`}
                  mono
                />
                <InfoRow label="Высота" value={`${fmtNumber(position.altitude, 1)} м`} mono />
                <InfoRow
                  label="Скорость"
                  value={`${fmtNumber(position.speed, 2)} м/с`}
                  mono
                />
                <InfoRow
                  label="Направление"
                  value={`${fmtNumber(position.heading, 1)}°`}
                  mono
                />
                <InfoRow label="Время позиции" value={formatDateTime(position.timestamp)} />
              </>
            ) : (
              <div className="empty">Позиция ещё не получена</div>
            )}
          </div>

          {geo.lastError && (
            <div className="card card--error">
              <div className="card__title">Ошибка геолокации</div>
              <InfoRow label="Код" value={geo.lastError.code ?? '—'} mono />
              <InfoRow label="Сообщение" value={geo.lastError.message} />
              <InfoRow label="Время" value={formatDateTime(geo.lastError.at)} />
            </div>
          )}

          <div className="card">
            <div className="card__title">Диагностика</div>
            <InfoRow
              label="Опции текущего запуска"
              value={
                geo.appliedOptions
                  ? `highAccuracy=${String(geo.appliedOptions.enableHighAccuracy)}, timeout=${
                      geo.appliedOptions.timeout
                    } мс, maximumAge=${geo.appliedOptions.maximumAge} мс`
                  : '—'
              }
            />
            <InfoRow label="watchId" value={geo.watchId ?? '—'} mono />
            <InfoRow label="Старт" value={formatDateTime(geo.startedAt)} />
            <InfoRow label="Обновлений" value={geo.updates} mono />
            <InfoRow label="Хранилище настроек" value={storage} />
            <InfoRow label="Настройки сохранены" value={formatDateTime(savedAt)} />
            <InfoRow label="API" value="navigator.geolocation.watchPosition" mono />
          </div>

          <div className="card">
            <div className="card__head">
              <div className="card__title">Журнал событий</div>
              <button type="button" className="link-btn" onClick={clearGeoLog}>
                очистить
              </button>
            </div>

            {geo.log.length === 0 ? (
              <div className="empty">Событий пока нет</div>
            ) : (
              <div className="log">
                {geo.log.map((item) => (
                  <div className="log-row" key={item.id}>
                    <span className={`badge badge--${item.kind}`}>{logKindLabel(item.kind)}</span>
                    <span className="log-row__text">{item.text}</span>
                    <span className="log-row__time">{formatTime(item.at)}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}
