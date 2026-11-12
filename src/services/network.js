/**
 * Состояние сети.
 * В Cordova используется cordova-plugin-network-information (navigator.connection)
 * и события online/offline, в браузере — navigator.onLine.
 */

export function readNetworkState() {
  const conn = navigator.connection || navigator.mozConnection || navigator.webkitConnection || null;
  const type = conn && typeof conn.type === 'string' ? conn.type : null;
  const browserOnline = typeof navigator.onLine === 'boolean' ? navigator.onLine : true;
  const online = browserOnline && type !== 'none';
  return {
    online,
    type: type || (online ? 'unknown' : 'none'),
  };
}

export function networkLabel(type) {
  switch (type) {
    case 'wifi':
      return 'Wi-Fi';
    case 'cellular':
    case '2g':
    case '3g':
    case '4g':
    case '5g':
      return 'мобильная сеть';
    case 'ethernet':
      return 'Ethernet';
    case 'none':
      return 'нет сети';
    default:
      return 'сеть';
  }
}

/**
 * Подписка на изменения сети. Колбэк вызывается сразу при подписке
 * и далее только при реальном изменении состояния.
 */
export function subscribeNetwork(callback) {
  let lastKey = null;

  const emit = () => {
    const state = readNetworkState();
    const key = `${state.online}|${state.type}`;
    if (key === lastKey) return;
    lastKey = key;
    callback(state);
  };

  window.addEventListener('online', emit);
  window.addEventListener('offline', emit);
  document.addEventListener('online', emit);
  document.addEventListener('offline', emit);

  // Страховка: в WebView navigator.connection.type может измениться без события.
  const timer = window.setInterval(emit, 5000);

  emit();

  return () => {
    window.removeEventListener('online', emit);
    window.removeEventListener('offline', emit);
    document.removeEventListener('online', emit);
    document.removeEventListener('offline', emit);
    window.clearInterval(timer);
  };
}
