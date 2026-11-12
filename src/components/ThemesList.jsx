import React, { useState } from 'react';
import ons from 'onsenui';
import {
  Page,
  List,
  ListItem,
  Toolbar,
  ToolbarButton,
  Icon,
} from 'react-onsenui';
import Loader from '../Loader';
import EditableItem from './EditableItem.jsx';
import EditDelItem from './EditDelItem.jsx';
import BusyModal from './BusyModal.jsx';

// Первый экран: список тем.
function ThemesList({ data, nav, clickHndlr, addItem, delItem, editItem }) {
  const [edit, setEdit] = useState({ editMode: false, editableItemId: 0 });
  const [selectedItemId, setSelectedItemId] = useState(0);
  const [isOpen, setIsOpen] = useState(false);

  const renderRow = (itm, ndx) => {
    if (edit.editMode) {
      return itm.id == edit.editableItemId ? (
        <EditableItem
          key={ndx}
          itemId={itm.id}
          initVal={itm.title}
          onSubmitChange={(id, newVal) => {
            editItem(id, newVal);
            setEdit({ editMode: false, editableItemId: 0 });
          }}
        />
      ) : (
        <EditDelItem
          key={ndx}
          itemId={itm.id}
          title={itm.title}
          onEdit={(id) => setEdit({ ...edit, editableItemId: id })}
          onDel={(id) => {
            setIsOpen(true);
            delItem(id).then(() => setIsOpen(false));
          }}
        />
      );
    }

    const divstyle = selectedItemId == itm.id ? { backgroundColor: '#d9d9d9' } : null;
    return (
      <ListItem
        key={ndx}
        modifier="longdivider chevron"
        onTap={(e) => {
          if (selectedItemId != itm.id) setSelectedItemId(itm.id);
          clickHndlr(e, nav, itm.id);
        }}
        style={{ paddingLeft: '0px' }}
      >
        <div className="left" style={divstyle}>
          &nbsp;
        </div>
        <div className="center" style={divstyle}>
          {itm.title}
        </div>
        <div className="right" style={divstyle} />
      </ListItem>
    );
  };

  const renderToolbar = () => (
    <Toolbar>
      <div className="center">Тема</div>
      <div className="right">
        <ToolbarButton
          onTap={() => {
            ons.notification
              .prompt('Тема!', { title: '', cancelable: true })
              .then((newTheme) => {
                if (newTheme) {
                  setIsOpen(true);
                  addItem(newTheme).then(() => setIsOpen(false));
                }
              });
          }}
        >
          <Icon icon="md-plus" />
        </ToolbarButton>
        <ToolbarButton
          onTap={() => setEdit({ ...edit, editMode: !edit.editMode })}
        >
          {edit.editMode ? <Icon icon="md-close" /> : <Icon icon="md-edit" />}
        </ToolbarButton>
      </div>
    </Toolbar>
  );

  return (
    <Page
      renderToolbar={renderToolbar}
      renderModal={() => <BusyModal isOpen={isOpen} text="Сохранение…" />}
    >
      <List dataSource={Loader.sorted(data.themes)} renderRow={renderRow} />
    </Page>
  );
}

export default ThemesList;
