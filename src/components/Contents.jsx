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

// Третий экран: главы (содержимое) выбранной подтемы.
// В обычном режиме главу можно выделить и переместить вверх/вниз.
function Contents({ data, subThemeId, nav, delItem, editItem, upAndDown }) {
  const choicedContents = Loader.sorted(
    data.contents.filter((track) => track.subthemeId === subThemeId)
  );
  const subThemeFound = data.subthemes.find((subTheme) => subTheme.id == subThemeId);
  const ellipsisLen = 20;
  const subThemeTitle = subThemeFound ? subThemeFound.title : '';
  const subThemeFoundEllipsis =
    subThemeTitle.length > ellipsisLen ? subThemeTitle.slice(0, ellipsisLen) + '…' : subThemeTitle;

  const [edit, setEdit] = useState({ editMode: false, editableItemId: 0 });
  const [isOpen, setIsOpen] = useState(false);
  const [selectedItemId, setSelectedItemId] = useState(0);

  const curIdNdx = choicedContents.findIndex((track) => track.id == selectedItemId);
  let ndxAfter = -1;
  let ndxBefore = -1;
  if (curIdNdx > -1) ndxAfter = curIdNdx + 1;
  if (curIdNdx > 0) ndxBefore = curIdNdx - 1;

  const moveUp = () => {
    if (curIdNdx <= 0) return;
    const aux = choicedContents[curIdNdx].ord;
    choicedContents[curIdNdx].ord = choicedContents[ndxBefore].ord;
    choicedContents[ndxBefore].ord = aux;
    upAndDown();
  };

  const moveDown = () => {
    if (curIdNdx < 0 || curIdNdx == choicedContents.length - 1) return;
    const aux = choicedContents[curIdNdx].ord;
    choicedContents[curIdNdx].ord = choicedContents[ndxAfter].ord;
    choicedContents[ndxAfter].ord = aux;
    upAndDown();
  };

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
        modifier="longdivider"
        onTap={() => {
          if (selectedItemId != itm.id) setSelectedItemId(itm.id);
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
      <div className="center">{subThemeFoundEllipsis}</div>
      <div className="right" style={{ flex: 2 }}>
        <ToolbarButton onTap={moveUp} disabled={curIdNdx <= 0}>
          <Icon icon="md-chevron-up" />
        </ToolbarButton>
        <ToolbarButton
          onTap={moveDown}
          disabled={curIdNdx < 0 || curIdNdx == choicedContents.length - 1}
        >
          <Icon icon="md-chevron-down" />
        </ToolbarButton>
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
      <List dataSource={choicedContents} renderRow={renderRow} />
    </Page>
  );
}

export default Contents;
