import React, { useEffect, useState } from 'react';

import { CableIcon } from '../components/Icons';
import {
  getBatteryState,
  initBattery,
  simulateBatteryEvent,
  subscribeBattery,
} from '../services/battery';
import { formatTime } from '../services/settings';

const EVENT_LABELS = {
  batterystatus: 'batterystatus',
  batterylow: 'batterylow',
  batterycritical: 'batterycritical',
};

function pluggedText(plugged) {
  if (plugged === null || plugged === undefined) return 'неизвестно';
  return plugged ? 'подключено' : 'отключено';
}

export default function BatteryPage() {
  const [battery, setBattery] = useState(getBatteryState);
  const hasCordova = Boolean(window.cordova);

  useEffect(() => {
    initBattery();
    return subscribeBattery(setBattery);
  }, []);

  const level = battery.level;
  const plugged = battery.plugged;
  const levelText = typeof level === 'number' ? `${level}%` : '—%';

  const source = hasCordova
    ? 'cordova-plugin-battery-status'
    : 'Battery Status API (браузер)';

  return (
    <div className="page-scroll">
      <div className="battery-card">
        <div className={`cable ${plugged ? 'cable--on' : plugged === false ? 'cable--off' : ''}`}>
          <CableIcon connected={plugged === true} size={44} />
        </div>

        <div className="battery-card__info">
          <div className="battery-card__level">{levelText}</div>
          <div className={`battery-card__state ${plugged ? 'on' : plugged === false ? 'off' : ''}`}>
            {plugged === null || plugged === undefined
              ? 'Состояние питания неизвестно'
              : plugged
                ? 'Питание подключено'
                : 'Питание отключено'}
          </div>
          <div className="muted small">
            {plugged === null || plugged === undefined
              ? 'Ждём первое событие batterystatus'
              : `Заряд: ${levelText} · batterystatus`}
          </div>
        </div>
      </div>

      <div className="battery-visual" aria-hidden="true">
        <div
          className={`battery-visual__fill ${
            typeof level === 'number' && level <= 15 ? 'battery-visual__fill--low' : ''
          }`}
          style={{ width: `${typeof level === 'number' ? Math.max(2, level) : 0}%` }}
        />
        <div className="battery-visual__cap" />
      </div>

      <div className="note">
        <strong>События:</strong> batterystatus приходит при изменении уровня заряда минимум на 1%
        и при подключении/отключении питания, batterylow — при низком заряде, batterycritical — при
        критическом. Приложение не фильтрует события: каждое попадает в журнал.
      </div>

      <div className="counters">
        {Object.keys(EVENT_LABELS).map((type) => (
          <div key={type} className={`counter counter--${type}`}>
            <div className="counter__value">{battery.counts[type] || 0}</div>
            <div className="counter__label">{EVENT_LABELS[type]}</div>
          </div>
        ))}
      </div>

      <div className="card">
        <div className="card__title">Текущее состояние</div>
        <div className="info-row">
          <div className="info-row__side">
            <div className="info-row__label">Уровень заряда</div>
            <code className="info-row__field">event.level</code>
          </div>
          <div className="info-row__value mono">{levelText}</div>
        </div>
        <div className="info-row">
          <div className="info-row__side">
            <div className="info-row__label">Питание</div>
            <code className="info-row__field">event.isPlugged</code>
          </div>
          <div className="info-row__value">
            {pluggedText(plugged)}
            {typeof level === 'number' ? ` · ${levelText}` : ''}
          </div>
        </div>
        <div className="info-row">
          <div className="info-row__side">
            <div className="info-row__label">Источник</div>
          </div>
          <div className="info-row__value">{battery.source || source}</div>
        </div>
        <div className="info-row">
          <div className="info-row__side">
            <div className="info-row__label">Последнее событие</div>
          </div>
          <div className="info-row__value">
            {battery.last
              ? `${battery.last.type} · ${battery.last.level ?? '—'}% · ${
                  battery.last.plugged ? 'подключено' : 'отключено'
                } · ${formatTime(battery.last.at)}`
              : '—'}
          </div>
        </div>
      </div>

      <div className="card">
        <div className="card__head">
          <div className="card__title">Журнал событий</div>
          <span className="muted small">новые сверху</span>
        </div>

        {battery.events.length === 0 ? (
          <div className="empty">
            Событий пока нет. В APK они появятся при изменении заряда или подключении питания.
          </div>
        ) : (
          <div className="log">
            {battery.events.map((entry) => (
              <div className="log-row log-row--battery" key={entry.id}>
                <span className={`badge badge--${entry.type}`}>{entry.type}</span>
                <span className="log-row__text">
                  {entry.level !== null ? `${entry.level}%` : '—'} ·{' '}
                  {entry.plugged ? 'питание подключено' : 'питание отключено'}
                </span>
                <span className="log-row__time">{formatTime(entry.at)}</span>
              </div>
            ))}
          </div>
        )}
      </div>

      {!hasCordova && (
        <div className="card">
          <div className="card__title">Демонстрация событий</div>
          <div className="muted small">
            Доступно только в браузере: в APK события приходят от плагина.
          </div>
          <div className="demo-buttons">
            <button
              type="button"
              className="demo-btn"
              onClick={() => simulateBatteryEvent('batterystatus', 57, true)}
            >
              batterystatus 57%, подключено
            </button>
            <button
              type="button"
              className="demo-btn"
              onClick={() => simulateBatteryEvent('batterylow', 14, false)}
            >
              batterylow 14%
            </button>
            <button
              type="button"
              className="demo-btn demo-btn--danger"
              onClick={() => simulateBatteryEvent('batterycritical', 4, false)}
            >
              batterycritical 4%
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
