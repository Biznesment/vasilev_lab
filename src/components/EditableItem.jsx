import React, { useRef, useEffect } from 'react';
import { ListItem, Input, Button, Icon } from 'react-onsenui';

// Строка в режиме редактирования: поле ввода + кнопка подтверждения.
function EditableItem({ itemId, initVal, onSubmitChange }) {
  const wrapRef = useRef(null);

  // Внутренний <input> создаётся самим web-компонентом <ons-input>,
  // поэтому работаем с ним через DOM-обёртку строки.
  const getInput = () => {
    const wrap = wrapRef.current;
    if (!wrap || !wrap.querySelector) return null;
    return wrap.querySelector('input');
  };

  useEffect(() => {
    let cancelled = false;
    const prepare = () => {
      if (cancelled) return;
      const input = getInput();
      if (!input) return false;
      input.value = initVal;
      if (input.focus) input.focus();
      if (input.select) input.select();
      return true;
    };
    if (prepare()) return undefined;
    // <ons-input> достраивает внутренний <input> асинхронно — подождём немного
    const timer = setInterval(() => {
      if (prepare()) clearInterval(timer);
    }, 30);
    return () => {
      cancelled = true;
      clearInterval(timer);
    };
  }, [initVal]);

  const submit = () => {
    const input = getInput();
    const value = input ? input.value : initVal;
    if (value && value.trim()) onSubmitChange(itemId, value);
  };

  return (
    <ListItem modifier="longdivider">
      <div className="center" ref={wrapRef}>
        <Input
          modifier="underbar"
          size="100"
          name="itmVal"
          onKeyDown={(e) => {
            if (e.key === 'Enter') submit();
          }}
        />
      </div>
      <div className="right">
        <Button modifier="cta" onTap={submit}>
          <Icon icon="md-check" />
        </Button>
      </div>
    </ListItem>
  );
}

export default EditableItem;
