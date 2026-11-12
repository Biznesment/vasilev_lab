// Исходные данные приложения: темы -> подтемы -> главы (содержимое).
// Модель данных полностью повторяет структуру оригинального проекта
// (themes / subthemes / contents, поле ord — порядок сортировки).
const data = {
  themes: [
    { id: 1, title: 'Лесков Н.С.', ord: 1 },
    { id: 2, title: 'Толстой Л.Н.', ord: 2 },
    { id: 3, title: 'Достоевский Н.С.', ord: 3 },
  ],
  subthemes: [
    { id: 1, themeId: 1, title: 'Железная воля', ord: 1 },
    { id: 2, themeId: 1, title: 'На краю света', ord: 2 },
    { id: 3, themeId: 1, title: 'Очарованный странник', ord: 3 },
    { id: 4, themeId: 1, title: 'Аскалонский злодей', ord: 4 },
    { id: 5, themeId: 1, title: 'Час воли божьей', ord: 5 },
    { id: 6, themeId: 2, title: 'Фальшивый купон', ord: 1 },
    { id: 7, themeId: 2, title: 'Исповедь', ord: 2 },
    { id: 8, themeId: 3, title: 'Сон смешного человека', ord: 1 },
    { id: 9, themeId: 3, title: 'Преступление и наказание', ord: 2 },
  ],
  contents: [
    // Лесков Н.С. / Железная воля
    ...['ch_1', 'ch_2', 'ch_3', 'ch_4', 'ch_5', 'ch_6', 'ch_7'].map((title, ndx) => ({
      id: ndx + 1,
      subthemeId: 1,
      title,
      ord: ndx + 1,
      loaded: false,
    })),
    // Лесков Н.С. / На краю света
    ...['ch_01', 'ch_02', 'ch_03', 'ch_04', 'ch_05', 'ch_06', 'ch_07', 'ch_08', 'ch_09', 'ch_10'].map(
      (title, ndx) => ({
        id: 8 + ndx,
        subthemeId: 2,
        title,
        ord: ndx + 1,
        loaded: false,
      })
    ),
    // Лесков Н.С. / Очарованный странник
    ...['ch_01', 'ch_02', 'ch_03', 'ch_04', 'ch_05'].map((title, ndx) => ({
      id: 18 + ndx,
      subthemeId: 3,
      title,
      ord: ndx + 1,
      loaded: false,
    })),
    // Лесков Н.С. / Час воли божьей
    ...['ch_01', 'ch_02'].map((title, ndx) => ({
      id: 23 + ndx,
      subthemeId: 5,
      title,
      ord: ndx + 1,
      loaded: false,
    })),
    // Лесков Н.С. / Аскалонский злодей
    ...['ch_01', 'ch_02', 'ch_03'].map((title, ndx) => ({
      id: 25 + ndx,
      subthemeId: 4,
      title,
      ord: ndx + 1,
      loaded: false,
    })),
    // Достоевский Н.С. / Преступление и наказание
    ...['part_01', 'part_02', 'part_03', 'part_04', 'part_05', 'part_06'].map((title, ndx) => ({
      id: 28 + ndx,
      subthemeId: 9,
      title,
      ord: ndx + 1,
      loaded: false,
    })),
    // Достоевский Н.С. / Сон смешного человека
    ...['ch_01', 'ch_02', 'ch_03'].map((title, ndx) => ({
      id: 34 + ndx,
      subthemeId: 8,
      title,
      ord: ndx + 1,
      loaded: false,
    })),
    // Толстой Л.Н. / Исповедь
    ...['01', '02', '03', '04'].map((title, ndx) => ({
      id: 37 + ndx,
      subthemeId: 7,
      title,
      ord: ndx + 1,
      loaded: false,
    })),
    // Толстой Л.Н. / Фальшивый купон
    ...['01', '02', '03', '04'].map((title, ndx) => ({
      id: 41 + ndx,
      subthemeId: 6,
      title,
      ord: ndx + 1,
      loaded: false,
    })),
  ],
};

export default data;
