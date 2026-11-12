import React from 'react';
import { ListItem, Button, Icon } from 'react-onsenui';

// Строка в режиме редактирования: кнопки «изменить» и «удалить».
function EditDelItem({ itemId, title, onEdit, onDel }) {
  return (
    <ListItem modifier="longdivider" onTap={null}>
      <div className="left">
        <Button key="edit" modifier="cta" onTap={(_) => onEdit(itemId)}>
          <Icon icon="md-edit" />
        </Button>
        &nbsp;
        <Button
          key="del"
          modifier="cta"
          style={{ color: 'red' }}
          onTap={(_) => onDel(itemId)}
        >
          <Icon icon="md-delete" />
        </Button>
      </div>
      <div className="center">{title}</div>
    </ListItem>
  );
}

export default EditDelItem;
