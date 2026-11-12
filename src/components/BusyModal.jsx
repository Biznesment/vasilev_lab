import React from 'react';
import { Modal, ProgressCircular } from 'react-onsenui';

// Модальное окно-заглушка, которое показывается во время выполнения операции.
function BusyModal({ isOpen, text }) {
  return (
    <Modal isOpen={isOpen} animation="fade">
      <div
        style={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          gap: '12px',
          padding: '24px 32px',
          background: '#fff',
          borderRadius: '8px',
          boxShadow: '0 8px 24px rgba(0,0,0,.18)',
        }}
      >
        <ProgressCircular indeterminate />
        <div style={{ color: '#333' }}>{text || 'Подождите…'}</div>
      </div>
    </Modal>
  );
}

export default BusyModal;
