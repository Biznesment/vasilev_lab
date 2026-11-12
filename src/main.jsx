import React from 'react';
import ReactDOM from 'react-dom/client';

import 'onsenui/css/onsenui.css';
import 'onsenui/css/onsen-css-components.css';
import './styles.css';

import App from './App.jsx';

const container = document.getElementById('root');
const root = ReactDOM.createRoot(container);

let mounted = false;

function mount() {
  if (mounted) return;
  mounted = true;
  root.render(<App />);
}

/*
 * В Cordova ждём deviceready, чтобы window.device и плагины были готовы.
 * В браузере (npm run dev) cordova.js нет — запускаемся сразу.
 */
if (window.cordova) {
  document.addEventListener('deviceready', mount, { once: true });
  // страховка, если deviceready не придёт
  window.setTimeout(mount, 3000);
} else {
  mount();
}
