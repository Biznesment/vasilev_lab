'use strict';
const $ = (selector) => document.querySelector(selector);
const installButton = $('#install');
const installHelp = $('#install-help');
const installHelpText = $('#install-help-text');
let installPrompt = null;
let installed = false;
const standalone = () => window.matchMedia('(display-mode: standalone)').matches || window.navigator.standalone === true;
function closeInstallHelp() {
  installHelp.hidden = true;
  installButton.setAttribute('aria-expanded', 'false');
}
function showInstallHelp() {
  installHelpText.textContent = window.isSecureContext
    ? 'Автоматическая установка пока недоступна. В Chrome откройте меню ⋮ и выберите «Установить приложение» или «Добавить на главный экран». Если этого пункта нет, проверьте поддержку установки и попробуйте позже.'
    : 'Для установки откройте приложение по HTTPS или на localhost. На телефоне адрес компьютера в локальной сети должен использовать HTTPS с доверенным сертификатом.';
  installHelp.hidden = false;
  installButton.setAttribute('aria-expanded', 'true');
}
window.addEventListener('beforeinstallprompt', (event) => {
  event.preventDefault();
  installPrompt = event;
  closeInstallHelp();
});
installButton.addEventListener('click', async () => {
  if (!installPrompt) { showInstallHelp(); return; }
  const prompt = installPrompt;
  installPrompt = null;
  closeInstallHelp();
  try {
    await prompt.prompt();
    await prompt.userChoice;
  } catch (error) {
    console.warn('Installation prompt:', error);
    showInstallHelp();
  }
});
$('#install-help-close').addEventListener('click', closeInstallHelp);
window.addEventListener('appinstalled', () => {
  installed = true;
  installPrompt = null;
  installButton.hidden = true;
  closeInstallHelp();
  showToast('Приложение установлено');
});
const displayMode = window.matchMedia('(display-mode: standalone)');
function updateInstallButton() {
  installButton.hidden = installed || standalone();
  if (installButton.hidden) closeInstallHelp();
}
if (displayMode.addEventListener) displayMode.addEventListener('change', updateInstallButton);
updateInstallButton();

if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('sw.js').catch((error) => console.error('Service Worker:', error));
}

const drawer = $('#drawer');
const menuOpen = $('#menu-open');
const scrim = $('#scrim');
function setMenu(open) {
  drawer.classList.toggle('open', open);
  drawer.setAttribute('aria-hidden', String(!open));
  drawer.inert = !open;
  menuOpen.setAttribute('aria-expanded', String(open));
  scrim.hidden = !open;
  document.body.style.overflow = open ? 'hidden' : '';
  (open ? $('#menu-close') : menuOpen).focus();
}
menuOpen.addEventListener('click', () => setMenu(true));
$('#menu-close').addEventListener('click', () => setMenu(false));
scrim.addEventListener('click', () => setMenu(false));
document.addEventListener('keydown', (event) => {
  if (event.key === 'Escape' && drawer.classList.contains('open')) setMenu(false);
  if (event.key === 'Tab' && drawer.classList.contains('open')) {
    const items = [...drawer.querySelectorAll('a, button')];
    const first = items[0], last = items[items.length - 1];
    if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
    else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
  }
});
drawer.querySelectorAll('a').forEach((link) => link.addEventListener('click', () => setMenu(false)));
let emojisRequested = false;
function route() {
  const hash = location.hash.slice(1);
  const page = ['emojis', 'contacts'].includes(hash) ? hash : 'home';
  document.querySelectorAll('.page').forEach((section) => { section.hidden = section.id !== page; });
  document.querySelectorAll('[data-page]').forEach((link) => {
    const active = link.dataset.page === page;
    link.classList.toggle('active', active);
    if (active) link.setAttribute('aria-current', 'page'); else link.removeAttribute('aria-current');
  });
  if (page === 'emojis' && !emojisRequested) loadEmojis();
  window.scrollTo(0, 0);
}
window.addEventListener('hashchange', route);

// The neutral background is shown first; actual images are requested near the viewport.
const imageObserver = 'IntersectionObserver' in window ? new IntersectionObserver((entries) => {
  entries.forEach((entry) => { if (entry.isIntersecting) { revealImage(entry.target); imageObserver.unobserve(entry.target); } });
}, { rootMargin: '250px 0px' }) : null;
function revealImage(img) {
  if (!img.dataset.src) return;
  img.addEventListener('load', () => img.classList.add('loaded'), { once: true });
  img.addEventListener('error', () => { img.alt = 'Изображение недоступно'; }, { once: true });
  img.src = img.dataset.src;
  delete img.dataset.src;
}
function lazyImage(img, src) {
  img.dataset.src = src;
  img.decoding = 'async';
  if (imageObserver) imageObserver.observe(img); else revealImage(img);
}
let allEmojis = [], filtered = [], shown = 0;
const PAGE_SIZE = 72;
async function loadEmojis() {
  emojisRequested = true;
  const status = $('#emoji-status'); status.hidden = false; status.textContent = 'Загружаем эмодзи…';
  try {
    const response = await fetch('https://api.github.com/emojis');
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const data = await response.json();
    if (!data || typeof data !== 'object' || Array.isArray(data)) throw new Error('Неверный ответ API');
    allEmojis = Object.entries(data).filter(([name, url]) => name && typeof url === 'string' && /^https:\/\//.test(url));
    filterEmojis();
  } catch (error) {
    status.textContent = 'Не удалось загрузить эмодзи. Проверьте интернет и попробуйте снова.';
    const retry = document.createElement('button'); retry.textContent = 'Повторить'; retry.className = 'more-button';
    retry.addEventListener('click', loadEmojis, { once: true }); status.append(' ', retry);
    console.warn('GitHub Emoji:', error);
  }
}
function filterEmojis() {
  const query = $('#emoji-search').value.trim().toLowerCase();
  filtered = allEmojis.filter(([name]) => name.toLowerCase().includes(query));
  shown = 0;
  if (imageObserver) $('#emoji-grid').querySelectorAll('img').forEach((img) => imageObserver.unobserve(img));
  $('#emoji-grid').replaceChildren();
  $('#emoji-count').textContent = `НАЙДЕНО: ${filtered.length}`;
  $('#emoji-status').hidden = filtered.length > 0;
  if (!filtered.length) $('#emoji-status').textContent = 'Ничего не найдено. Попробуйте другой запрос.';
  renderMore();
}
function renderMore() {
  const grid = $('#emoji-grid');
  const fragment = document.createDocumentFragment();
  filtered.slice(shown, shown + PAGE_SIZE).forEach(([name, url]) => {
    const button = document.createElement('button'); button.className = 'emoji-item';
    button.type = 'button'; button.title = `Скопировать :${name}:`; button.setAttribute('aria-label', `Скопировать :${name}:`);
    const img = document.createElement('img'); img.alt = ''; img.width = 44; img.height = 44; lazyImage(img, url);
    const label = document.createElement('span'); label.textContent = `:${name}:`;
    button.addEventListener('click', () => copyName(name)); button.append(img, label); fragment.append(button);
  });
  shown = Math.min(shown + PAGE_SIZE, filtered.length); grid.append(fragment);
  $('#load-more').hidden = shown >= filtered.length;
}
async function copyName(name) {
  try { await navigator.clipboard.writeText(`:${name}:`); showToast(`Скопировано :${name}:`); }
  catch { showToast(`Название: :${name}:`); }
}
let toastTimer;
function showToast(message) { const toast = $('#toast'); toast.textContent = message; toast.classList.add('show'); clearTimeout(toastTimer); toastTimer = setTimeout(() => toast.classList.remove('show'), 2500); }
$('#emoji-search').addEventListener('input', () => { if (allEmojis.length) filterEmojis(); });
$('#load-more').addEventListener('click', renderMore);
route();
