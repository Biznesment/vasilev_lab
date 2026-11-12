import React from 'react';

const PATHS = {
  menu: <path d="M3 6h18M3 12h18M3 18h18" />,
  chat: <path d="M21 11.5a8.5 8.5 0 0 1-8.5 8.5H4l2.2-3.1A8.5 8.5 0 1 1 21 11.5z" />,
  device: (
    <>
      <rect x="6" y="2.5" width="12" height="19" rx="2.5" />
      <path d="M10.5 18.5h3" />
    </>
  ),
  location: (
    <>
      <path d="M12 21.5s7-5.6 7-11.1a7 7 0 1 0-14 0c0 5.5 7 11.1 7 11.1z" />
      <circle cx="12" cy="10.2" r="2.6" />
    </>
  ),
  battery: (
    <>
      <rect x="2.5" y="7" width="17" height="10" rx="2.5" />
      <path d="M22 10.8v2.4" />
    </>
  ),
  left: <path d="M15.5 5 8.5 12l7 7" />,
  close: <path d="M6.5 6.5l11 11M17.5 6.5l-11 11" />,
  right: <path d="M8.5 5l7 7-7 7" />,
  copy: (
    <>
      <rect x="9" y="9" width="11" height="11" rx="2" />
      <path d="M6 15H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v1" />
    </>
  ),
  trash: (
    <>
      <path d="M4 7h16M9 7V4.5h6V7M6.5 7l.8 13h9.4l.8-13" />
      <path d="M10 11v5M14 11v5" />
    </>
  ),
  refresh: <path d="M20 12a8 8 0 1 1-2.6-5.9M20 4v4h-4" />,
  play: <path d="M8 5.5v13l11-6.5z" />,
  stop: <rect x="6.5" y="6.5" width="11" height="11" rx="1.5" />,
  check: <path d="M5 12.5 9.5 17 19 7" />,
  wifi: (
    <>
      <path d="M2.5 8.5a15 15 0 0 1 19 0" />
      <path d="M5.5 12a10.5 10.5 0 0 1 13 0" />
      <path d="M8.5 15.5a6 6 0 0 1 7 0" />
      <circle cx="12" cy="19" r="0.8" fill="currentColor" stroke="none" />
    </>
  ),
  wifiOff: (
    <>
      <path d="M2.5 8.5a15 15 0 0 1 5.2-3M13 5.2a15 15 0 0 1 8.5 3.3" />
      <path d="M5.5 12a10.5 10.5 0 0 1 4-2.1M14 10.1a10.5 10.5 0 0 1 4.5 1.9" />
      <path d="M8.5 15.5a6 6 0 0 1 7 0" />
      <path d="M3 3l18 18" />
    </>
  ),
  info: (
    <>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 11v6M12 7.5v.5" />
    </>
  ),
};

export default function Icon({ name, size = 22, className = '', strokeWidth = 1.8 }) {
  return (
    <svg
      className={`icon ${className}`}
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      {PATHS[name] || null}
    </svg>
  );
}

/**
 * Иконка «шнур питания»: вилка + провод.
 * connected = true  → зелёная, шнур вставлен;
 * connected = false → серая, шнур перечёркнут.
 */
export function CableIcon({ connected, size = 40, className = '' }) {
  return (
    <svg
      className={`icon ${className}`}
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.7}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      {/* штыри вилки */}
      <path d="M9 2.5v4.2M15 2.5v4.2" />
      {/* корпус вилки */}
      <path d="M6.4 6.7h11.2v3.4a5.6 5.6 0 0 1-5.6 5.6 5.6 5.6 0 0 1-5.6-5.6V6.7z" />
      {/* провод */}
      <path d="M12 15.7v2.4a3.6 3.6 0 0 0 3.6 3.6h2.6" />
      {!connected && <path d="M3 3l18 18" />}
    </svg>
  );
}
