'use strict';
if ('serviceWorker' in navigator) navigator.serviceWorker.register('sw.js').catch(console.error);
function openLog() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open('mesto-sw-log', 1);
    request.onupgradeneeded = () => request.result.createObjectStore('events', { autoIncrement: true });
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}
async function refresh() {
  const status = document.querySelector('#log-status');
  try {
    const db = await openLog();
    const entries = await new Promise((resolve, reject) => {
      const tx = db.transaction('events', 'readonly'); const request = tx.objectStore('events').getAll();
      request.onsuccess = () => resolve(request.result); request.onerror = () => reject(request.error);
    });
    db.close();
    const list = document.querySelector('#log-list'); list.replaceChildren();
    entries.reverse().forEach((entry) => {
      const row = document.createElement('div'); row.className = 'log-entry';
      const time = document.createElement('time'); time.dateTime = entry.time; time.textContent = new Date(entry.time).toLocaleString('ru-RU');
      const type = document.createElement('strong'); type.textContent = entry.type;
      const detail = document.createElement('span'); detail.textContent = entry.detail;
      row.append(time, type, detail); list.append(row);
    });
    status.textContent = entries.length ? `Записей: ${entries.length}` : 'Пока нет записей. Service Worker создаст их после установки и запросов.';
  } catch (error) { status.textContent = 'Не удалось прочитать журнал. Проверьте доступность IndexedDB.'; console.error(error); }
}
async function clearLog() {
  try {
    const db = await openLog();
    await new Promise((resolve, reject) => { const tx = db.transaction('events', 'readwrite'); tx.objectStore('events').clear(); tx.oncomplete = resolve; tx.onerror = () => reject(tx.error); });
    db.close(); await refresh();
  } catch (error) { document.querySelector('#log-status').textContent = 'Не удалось очистить журнал.'; console.error(error); }
}
document.querySelector('#refresh').addEventListener('click', refresh);
document.querySelector('#clear').addEventListener('click', clearLog);
refresh();
