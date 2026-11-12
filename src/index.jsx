import React from 'react';
import { createRoot } from 'react-dom/client';
import ons from 'onsenui';
import 'onsenui/css/onsenui.css';
import 'onsenui/css/onsen-css-components.css';
import data from './data.js';
import Choicer from './components/Choicer.jsx';

ons.ready(() => {
  const root = createRoot(document.getElementById('root'));
  root.render(<Choicer data={data} />);
});
