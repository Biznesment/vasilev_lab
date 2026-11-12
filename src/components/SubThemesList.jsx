import React, { useState } from 'react';
import {
  Page,
  List,
  ListItem,
  Toolbar,
  ToolbarButton,
  BackButton,
  Icon,
} from 'react-onsenui';
import Loader from '../Loader';
import EditableItem from './EditableItem.jsx';
import EditDelItem from './EditDelItem.jsx';
import BusyModal from './BusyModal.jsx';

// Второй экран: подтемы выбранной темы.
function SubThemesList({ data, themeId, nav, clickHndlr, delItem, editItem }) {
  const choicedSubthemes = Loader.sorted(
    data.subthemes.filter((subtheme) => subtheme.themeId === themeId)
  );
  const themeFound = data.themes.find((theme) => theme.id == themeId);
  const ellipsisLen = 20;
  const themeTitle = themeFound ? themeFound.title : '';
  const themeFoundEllipsis =
    themeTitle.length > ellipsisLen ? themeTitle.slice(0, ellipsisLen) + '…' : themeTitle;

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
      <div className="left">
        <BackButton>Back</BackButton>
      </div>
      <div className="center">{themeFoundEllipsis}</div>
      <div className="right">
        <ToolbarButton onTap={() => setEdit({ ...edit, editMode: !edit.editMode })}>
          {edit.editMode ? <Icon icon="md-close" /> : <Icon icon="md-edit" />}
        </ToolbarButton>
      </div>
    </Toolbar>
  );

  return (
    <Page
      renderToolbar={renderToolbar}
      renderModal={() => <BusyModal isOpen={isOpen} text="Удаление…" />}
    >
      <List dataSource={choicedSubthemes} renderRow={renderRow} />
    </Page>
  );
}

export default SubThemesList;
