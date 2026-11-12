import React, { useState, useCallback } from 'react';
import { Navigator } from 'react-onsenui';
import Loader from '../Loader';
import ThemesList from './ThemesList.jsx';
import SubThemesList from './SubThemesList.jsx';
import Contents from './Contents.jsx';

// Корневой компонент: навигация themes -> subthemes -> contents
// и все операции редактирования данных.
function Choicer({ data: initData }) {
  const [data, setData] = useState(initData);

  // Защита от двойного тапа: Navigator отклоняет pushPage, пока идёт анимация.
  const safePush = (nav, route) => {
    const result = nav.pushPage(route);
    if (result && result.catch) result.catch(() => {});
  };

  const onThemeClick = (e, nav, themeId) => {
    safePush(nav, { type: 'subthemes', themeId });
  };

  const onSubThemeClick = (e, nav, subThemeId) => {
    safePush(nav, { type: 'contents', subThemeId });
  };

  const add = (where, newVal) => {
    // копируем и сам список, чтобы не мутировать текущее состояние
    const dataClone = { ...data, [where]: [...data[where]] };
    Loader.add(dataClone, where, newVal);
    setData(dataClone);
    return Promise.resolve(true);
  };

  const del = (where, id) => {
    const dataClone = { ...data };
    Loader.del(dataClone, where, id);
    setData(dataClone);
    return Promise.resolve(true);
  };

  const edit = (where, id, newVal) => {
    const ndx = data[where].findIndex((itm) => id == itm.id);
    if (ndx == -1) return;
    const dataClone = { ...data };
    dataClone[where][ndx] = { ...dataClone[where][ndx], title: newVal };
    setData(dataClone);
  };

  const upAndDown = useCallback(() => {
    setData((prev) => ({ ...prev, contents: [...prev.contents] }));
  }, []);

  const renderPage = (route, nav) => {
    if (route.type == 'themes') {
      return (
        <ThemesList
          key="themes"
          data={data}
          nav={nav}
          clickHndlr={onThemeClick}
          addItem={(newVal) => add('themes', newVal)}
          delItem={(id) => del('themes', id)}
          editItem={(id, newVal) => edit('themes', id, newVal)}
        />
      );
    }
    if (route.type == 'subthemes') {
      return (
        <SubThemesList
          key="subthemes"
          data={data}
          nav={nav}
          themeId={route.themeId}
          clickHndlr={onSubThemeClick}
          delItem={(id) => del('subthemes', id)}
          editItem={(id, newVal) => edit('subthemes', id, newVal)}
        />
      );
    }
    if (route.type == 'contents') {
      return (
        <Contents
          key="contents"
          data={data}
          subThemeId={route.subThemeId}
          nav={nav}
          delItem={(id) => del('contents', id)}
          editItem={(id, newVal) => edit('contents', id, newVal)}
          upAndDown={upAndDown}
        />
      );
    }
    return null;
  };

  return <Navigator renderPage={renderPage} initialRoute={{ type: 'themes' }} />;
}

export default Choicer;
