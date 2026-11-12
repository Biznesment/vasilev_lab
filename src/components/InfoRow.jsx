import React from 'react';

export default function InfoRow({ label, value, field, mono = false, warning = false }) {
  return (
    <div className={`info-row ${warning ? 'info-row--warning' : ''}`}>
      <div className="info-row__side">
        <div className="info-row__label">{label}</div>
        {field && <code className="info-row__field">{field}</code>}
      </div>
      <div className={`info-row__value ${mono ? 'mono' : ''}`}>{value}</div>
    </div>
  );
}
