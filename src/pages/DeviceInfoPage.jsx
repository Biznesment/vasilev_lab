import React, { useCallback, useMemo, useState } from 'react';
import { Button } from 'react-onsenui';
import ons from 'onsenui';

import InfoRow from '../components/InfoRow';
import { formatBoolean, readDeviceInfo } from '../services/deviceInfo';

function copyText(text) {
  if (navigator.clipboard && navigator.clipboard.writeText) {
    return navigator.clipboard.writeText(text);
  }

  return new Promise((resolve, reject) => {
    try {
      const area = document.createElement('textarea');
      area.value = text;
      area.style.position = 'fixed';
      area.style.opacity = '0';
      document.body.appendChild(area);
      area.select();
      document.execCommand('copy');
      document.body.removeChild(area);
      resolve();
    } catch (error) {
      reject(error);
    }
  });
}

export default function DeviceInfoPage() {
  const [info, setInfo] = useState(() => readDeviceInfo());

  const sdkValue = useMemo(() => {
    if (!info.sdk) return '—';
    return `API ${info.sdk}`;
  }, [info.sdk]);

  const serialValue = info.serial ? info.serial : 'недоступен';

  const refresh = () => {
    setInfo(readDeviceInfo());
    ons.notification.toast('Данные об устройстве обновлены', { timeout: 1500 });
  };

  const copyReport = useCallback(async () => {
    const report = [
      `source: ${info.source}`,
      `platform: ${info.platform}`,
      `androidVersion: ${info.androidVersion}`,
      `cordova: ${info.cordova}`,
      `model: ${info.model}`,
      `uuid: ${info.uuid}`,
      `device.version: ${info.version}`,
      `manufacturer: ${info.manufacturer}`,
      `serial: ${info.serial || '—'}`,
      `isVirtual: ${formatBoolean(info.isVirtual)}`,
      `sdkVersion: ${info.sdk ?? '—'}`,
      '',
      JSON.stringify(info.raw, null, 2),
    ].join('\n');

    try {
      await copyText(report);
      ons.notification.toast('Отчёт скопирован в буфер обмена', { timeout: 1800 });
    } catch (error) {
      ons.notification.toast(`Не удалось скопировать: ${error.message}`, { timeout: 2500 });
    }
  }, [info]);

  return (
    <div className="page-scroll">
      {!info.isCordova && (
        <div className="note note--info">
          Cordova-плагины недоступны: приложение открыто в браузере. Значения ниже приблизительные —
          в собранном APK будут реальные данные устройства.
        </div>
      )}

      <div className="card">
        <div className="card__title">Устройство</div>
        <InfoRow label="Модель" field="device.model" value={info.model} />
        <InfoRow label="Производитель" field="device.manufacturer" value={info.manufacturer} />
        <InfoRow label="Платформа" field="device.platform" value={info.platform} />
        <InfoRow label="Версия Android" field="device.version" value={info.androidVersion} />
        <InfoRow
          label="Device version"
          field="device.version"
          value={info.version}
          mono
        />
        <InfoRow
          label="SDK version"
          field={info.sdkSource === 'device.sdkVersion' ? 'device.sdkVersion' : undefined}
          value={sdkValue}
          mono
        />
        <InfoRow
          label="Симулятор"
          field="device.isVirtual"
          value={formatBoolean(info.isVirtual)}
        />
      </div>

      {info.sdkSource && info.sdkSource !== 'device.sdkVersion' && (
        <div className="note">
          SDK version вычислен по версии Android: {info.androidVersion} → API {info.sdk}.
          В APK плагин обычно отдаёт значение напрямую.
        </div>
      )}

      <div className="card">
        <div className="card__title">Идентификаторы</div>
        <InfoRow label="UUID" field="device.uuid" value={info.uuid} mono />
        <InfoRow label="Серийный номер" field="device.serial" value={serialValue} mono />
        <div className="card__note">
          На Android 10+ серийный номер недоступен обычным приложениям даже с разрешением
          READ_PHONE_STATE. Пустое значение — ожидаемое поведение.
        </div>
      </div>

      <div className="card">
        <div className="card__title">Cordova</div>
        <InfoRow label="Версия Cordova" field="device.cordova" value={info.cordova} mono />
        <InfoRow label="Источник данных" value={info.source} />
      </div>

      <div className="actions">
        <Button className="btn-secondary" onClick={refresh}>
          Обновить
        </Button>
        <Button className="btn-secondary" onClick={copyReport}>
          Копировать отчёт
        </Button>
      </div>

      <details className="raw">
        <summary>Сырые данные плагина</summary>
        <pre>{JSON.stringify(info.raw, null, 2)}</pre>
      </details>
    </div>
  );
}
