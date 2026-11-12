import React, { useEffect, useRef, useState } from 'react';
import {
  List,
  ListItem,
  Page,
  Splitter,
  SplitterContent,
  SplitterSide,
  Toolbar,
} from 'react-onsenui';
import ons from 'onsenui';

import Icon from './components/Icons';
import BatteryPage from './pages/BatteryPage';
import DeviceInfoPage from './pages/DeviceInfoPage';
import GeolocationPage from './pages/GeolocationPage';
import MessagesPage from './pages/MessagesPage';
import { initBattery } from './services/battery';
import { networkLabel, readNetworkState, subscribeNetwork } from './services/network';

const MENU_ITEMS = [
  { id: 'messages', label: 'Сообщения', icon: 'chat' },
  { id: 'device', label: 'Device Info', icon: 'device' },
  { id: 'geolocation', label: 'Геолокация', icon: 'location' },
  { id: 'battery', label: 'Статус батареи', icon: 'battery' },
];

const TITLES = {
  messages: 'Сообщения',
  device: 'Device Info',
  geolocation: 'Геолокация',
  battery: 'Статус батареи',
};

export function Menu({ current, onSelect, onClose, network }) {
  return (
    <div className="menu">
      <div className="menu__header">
        <div className="menu__logo">VL</div>
        <div className="menu__titles">
          <div className="menu__title">Vasilev Lab</div>
          <div className="menu__subtitle">Cordova · React · Onsen UI</div>
        </div>
        <button
          type="button"
          className="menu__close"
          aria-label="Закрыть меню"
          onClick={onClose}
        >
          <Icon name="close" size={20} />
        </button>
      </div>

      <div className={`net-pill ${network.online ? 'net-pill--online' : 'net-pill--offline'}`}>
        <Icon name={network.online ? 'wifi' : 'wifiOff'} size={15} />
        <span>
          {network.online ? `Онлайн · ${networkLabel(network.type)}` : 'Офлайн · нет сети'}
        </span>
      </div>

      <List className="menu__list">
        {MENU_ITEMS.map((item) => (
          <ListItem
            key={item.id}
            tappable
            className={`menu-item ${current === item.id ? 'menu-item--active' : ''}`}
            onClick={() => onSelect(item.id)}
          >
            <div className="menu-item__inner">
              <Icon name={item.icon} size={20} />
              <span>{item.label}</span>
            </div>
          </ListItem>
        ))}
      </List>

      <div className="menu__footer">
        <div>версия 1.0.0</div>
        <div className="menu__footer-plugins">
          device · geolocation · battery-status · network-information
        </div>
      </div>
    </div>
  );
}

export default function App() {
  const [menuOpen, setMenuOpen] = useState(false);
  const [page, setPage] = useState('messages');
  const [network, setNetwork] = useState(readNetworkState);
  const onlineRef = useRef(null);

  useEffect(() => {
    initBattery();
  }, []);

  // Аппаратная кнопка «Назад»: закрыть меню или вернуться к сообщениям.
  useEffect(() => {
    const onBackButton = (event) => {
      if (menuOpen) {
        event.preventDefault();
        setMenuOpen(false);
        return;
      }
      if (page !== 'messages') {
        event.preventDefault();
        setPage('messages');
      }
    };

    document.addEventListener('backbutton', onBackButton, false);
    return () => document.removeEventListener('backbutton', onBackButton, false);
  }, [menuOpen, page]);

  useEffect(() => {
    const unsubscribe = subscribeNetwork((state) => {
      const wasOnline = onlineRef.current;
      onlineRef.current = state.online;
      setNetwork(state);

      if (wasOnline === true && state.online === false) {
        ons.notification.toast('Соединение потеряно — приложение перешло в offline', {
          timeout: 4000,
          animation: 'fallback',
        });
      }
    });

    return unsubscribe;
  }, []);

  const renderPage = () => {
    switch (page) {
      case 'device':
        return <DeviceInfoPage />;
      case 'geolocation':
        return <GeolocationPage />;
      case 'battery':
        return <BatteryPage />;
      case 'messages':
      default:
        return <MessagesPage online={network.online} />;
    }
  };

  return (
    <Splitter>
      <SplitterSide
        side="left"
        width="272px"
        isOpen={menuOpen}
        collapse=""
        swipeable={false}
        onPostClose={() => setMenuOpen(false)}
        className="app-menu"
      >
        <Menu
          current={page}
          network={network}
          onClose={() => setMenuOpen(false)}
          onSelect={(nextPage) => {
            setPage(nextPage);
            setMenuOpen(false);
          }}
        />
      </SplitterSide>

      <SplitterContent>
        <Page
          renderToolbar={() => (
            <Toolbar>
              <div className="left">
                <button
                  type="button"
                  className="toolbar-menu-btn"
                  aria-label="Открыть меню"
                  onClick={() => setMenuOpen(true)}
                >
                  <Icon name="menu" size={24} />
                </button>
              </div>

              <div className="center">{TITLES[page]}</div>

              <div className="right">
                {!network.online && (
                  <span className="toolbar-offline">
                    <Icon name="wifiOff" size={15} />
                    offline
                  </span>
                )}
              </div>
            </Toolbar>
          )}
        >
          {renderPage()}
        </Page>
      </SplitterContent>
    </Splitter>
  );
}
