import React, { useEffect, useRef, useState } from 'react';
import { Carousel, CarouselItem } from 'react-onsenui';

import messages from '../data/messages';
import Icon from '../components/Icons';

function MessageCard({ message, index, total }) {
  return (
    <article className="msg-card">
      <header className="msg-card__head">
        <div className="avatar" style={{ backgroundColor: message.color }}>
          {message.initials}
        </div>
        <div className="msg-card__meta">
          <div className="msg-card__author">{message.author}</div>
          <div className="msg-card__time">сегодня, {message.time}</div>
        </div>
        <div className="msg-card__index">
          {index + 1} / {total}
        </div>
      </header>

      <p className="msg-card__text">{message.text}</p>

      <footer className="msg-card__foot">
        <span>текстовое сообщение</span>
        <span className="msg-card__hint">свайп ← →</span>
      </footer>
    </article>
  );
}

export default function MessagesPage({ online }) {
  const carouselRef = useRef(null);
  const [index, setIndex] = useState(0);

  const goTo = (nextIndex) => {
    const target = Math.max(0, Math.min(messages.length - 1, nextIndex));
    if (carouselRef.current) {
      carouselRef.current.setActiveIndex(target, { animation: true });
    }
    setIndex(target);
  };

  useEffect(() => {
    const onKeyDown = (event) => {
      if (event.key === 'ArrowRight') goTo(index + 1);
      if (event.key === 'ArrowLeft') goTo(index - 1);
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [index]);

  return (
    <div className="messages-page">
      {!online && (
        <div className="offline-banner" role="status">
          <Icon name="wifiOff" size={18} />
          <span>
            <strong>Вы offline.</strong> Нет подключения к сети — показаны сохранённые сообщения.
          </span>
        </div>
      )}

      <div className="carousel-wrap">
        <Carousel
          ref={carouselRef}
          fullscreen
          swipeable
          overscrollable
          initialIndex={0}
          onPostChange={(event) => setIndex(event.activeIndex)}
        >
          {messages.map((message, i) => (
            <CarouselItem key={message.id}>
              <div className="msg-slide">
                <MessageCard message={message} index={i} total={messages.length} />
              </div>
            </CarouselItem>
          ))}
        </Carousel>
      </div>

      <footer className="carousel-footer">
        <button
          type="button"
          className="nav-btn"
          aria-label="Предыдущее сообщение"
          disabled={index === 0}
          onClick={() => goTo(index - 1)}
        >
          <Icon name="left" size={20} />
        </button>

        <div className="dots" role="tablist" aria-label="Навигация по сообщениям">
          {messages.map((message, i) => (
            <button
              key={message.id}
              type="button"
              role="tab"
              aria-label={`Сообщение ${i + 1}`}
              aria-selected={i === index}
              className={`dot-btn ${i === index ? 'dot-btn--active' : ''}`}
              onClick={() => goTo(i)}
            />
          ))}
        </div>

        <button
          type="button"
          className="nav-btn"
          aria-label="Следующее сообщение"
          disabled={index === messages.length - 1}
          onClick={() => goTo(index + 1)}
        >
          <Icon name="right" size={20} />
        </button>
      </footer>
    </div>
  );
}
