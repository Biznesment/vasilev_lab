/**
 * Информация об устройстве (cordova-plugin-device).
 * В обычном браузере возвращается приблизительная информация из User-Agent,
 * чтобы интерфейс можно было проверить без сборки APK.
 */

const ANDROID_SDK = {
  16: 36,
  15: 35,
  14: 34,
  13: 33,
  '12.1': 32,
  12: 31,
  11: 30,
  10: 29,
  9: 28,
  '8.1': 27,
  '8.0': 26,
  8: 26,
  '7.1': 25,
  '7.0': 24,
  7: 24,
  '6.0': 23,
  6: 23,
  '5.1': 22,
  '5.0': 21,
  5: 21,
};

export function androidSdkFromVersion(version) {
  if (!version) return null;
  const match = String(version).trim().match(/^(\d+)(?:\.(\d+))?/);
  if (!match) return null;

  const major = match[1];
  const minor = match[2];
  const keys = [];

  if (minor !== undefined) {
    keys.push(`${major}.${minor}`);
    // для "8.1.0" сначала "8.1", затем "8.0" не нужен
  }
  keys.push(major);
  keys.push(`${major}.0`);

  for (const key of keys) {
    if (Object.prototype.hasOwnProperty.call(ANDROID_SDK, key)) {
      return ANDROID_SDK[key];
    }
  }
  return null;
}

function browserAndroidVersion(ua) {
  const match = ua.match(/Android\s+([\d.]+)/i);
  return match ? match[1] : null;
}

function browserModel(ua) {
  const android = ua.match(/Android\s+[\d.]+;\s*([^;)]+?)(?:\s+Build\/[^;)]*)?\)/i);
  if (android) return android[1].trim();

  if (/iPhone/i.test(ua)) return 'iPhone';
  if (/iPad/i.test(ua)) return 'iPad';
  if (/Macintosh/i.test(ua)) return 'Mac';
  if (/Windows/i.test(ua)) return 'Windows PC';
  if (/Linux/i.test(ua)) return 'Linux PC';

  return 'браузер';
}

export function readDeviceInfo() {
  const device = typeof window !== 'undefined' ? window.device : null;
  const ua = typeof navigator !== 'undefined' ? navigator.userAgent : '';
  const hasCordovaDevice = Boolean(window.cordova && device);

  const platform = hasCordovaDevice ? device.platform || '—' : 'browser';
  const version = hasCordovaDevice ? device.version || '—' : browserAndroidVersion(ua) || '—';
  const androidVersion = String(platform).toLowerCase() === 'android' ? version : '—';
  const pluginSdk =
    hasCordovaDevice && device.sdkVersion ? Number(device.sdkVersion) : null;
  const derivedSdk = androidSdkFromVersion(androidVersion);
  const sdk = Number.isFinite(pluginSdk) ? pluginSdk : derivedSdk;
  const sdkSource = Number.isFinite(pluginSdk)
    ? 'device.sdkVersion'
    : derivedSdk
      ? 'вычислено по версии Android'
      : null;

  return {
    source: hasCordovaDevice ? 'cordova-plugin-device' : 'браузер (fallback, без Cordova)',
    isCordova: hasCordovaDevice,
    platform,
    androidVersion,
    sdk,
    sdkSource,
    cordova: hasCordovaDevice ? device.cordova || '—' : '—',
    model: hasCordovaDevice ? device.model || '—' : browserModel(ua),
    uuid: hasCordovaDevice ? device.uuid || '—' : '—',
    version,
    manufacturer: hasCordovaDevice ? device.manufacturer || '—' : '—',
    serial:
      hasCordovaDevice && device.serial && device.serial !== 'unknown' ? device.serial : '',
    isVirtual:
      hasCordovaDevice && typeof device.isVirtual === 'boolean' ? device.isVirtual : null,
    raw: hasCordovaDevice
      ? {
          cordova: device.cordova,
          model: device.model,
          platform: device.platform,
          uuid: device.uuid,
          version: device.version,
          manufacturer: device.manufacturer,
          serial: device.serial,
          isVirtual: device.isVirtual,
          sdkVersion: device.sdkVersion,
        }
      : { userAgent: ua },
  };
}

export function formatBoolean(value) {
  if (value === null || value === undefined) return '—';
  return value ? 'Да' : 'Нет';
}
