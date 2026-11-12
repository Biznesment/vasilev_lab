// Утилиты для работы с иерархией themes -> subthemes -> contents.
const whereNames = ['themes', 'subthemes', 'contents'];

class Loader {
  static getMax(where, what) {
    let maxVal = 0;
    where.forEach((itm) => {
      if (itm[what] > maxVal) maxVal = itm[what];
    });
    return maxVal;
  }

  static add(data, where, newVal) {
    const maxId = this.getMax(data[where], 'id');
    const maxOrd = this.getMax(data[where], 'ord');
    data[where].push({
      id: maxId + 1,
      title: newVal,
      ord: maxOrd + 1,
    });
  }

  // Удаляет элемент и всё, что от него зависит по цепочке вниз.
  static del(data, where, id) {
    const whereNdx = whereNames.findIndex((itm) => itm === where);
    data[where] = data[where].filter((itm) => id != itm.id);
    let deletedIdsInMain = [id];
    let ndx = whereNdx;
    while (whereNames.length - ndx > 1) {
      const main = whereNames[ndx];
      const sub = whereNames[ndx + 1];
      // формируем внешний ключ, например themeId
      const prntId = main.substring(0, main.length - 1) + 'Id';
      const deletedIdsInSub = [];
      data[sub] = data[sub].filter((itm) => {
        if (deletedIdsInMain.includes(itm[prntId])) {
          deletedIdsInSub.push(itm.id);
          return false;
        }
        return true;
      });
      ndx++;
      deletedIdsInMain = [...deletedIdsInSub];
    }
    return deletedIdsInMain;
  }

  // Синхронизация подтем/глав по объекту {подтема: [главы]}
  static editSubThemes(data, obj, themeId, themePath) {
    const subThemes = data.subthemes.filter((subTheme) => subTheme.themeId == themeId);
    const subThemesTtls = subThemes.map((itm) => itm.title);
    const newSubThemes = Object.keys(obj);

    let maxSubThemeId = this.getMax(data.subthemes, 'id');
    let maxTrackId = this.getMax(data.contents, 'id');
    let maxSubThemeOrd = this.getMax(subThemes, 'ord');

    newSubThemes.forEach((newSubTheme) => {
      const ndx = subThemesTtls.indexOf(newSubTheme);
      if (ndx > -1) {
        // существующая подтема — добавляем недостающие главы
        const subThemeId = subThemes[ndx].id;
        const contents = data.contents.filter((itm) => itm.subthemeId == subThemeId);
        const contentsTtls = contents.map((itm) => itm.title);
        let maxTrackOrd = this.getMax(contents, 'ord');
        obj[newSubTheme].forEach((nm) => {
          if (contentsTtls.includes(nm)) return;
          data.contents.push({
            id: ++maxTrackId,
            subthemeId: subThemeId,
            title: nm,
            path: `${themePath}/${newSubTheme}/${nm}.mp3`,
            ord: ++maxTrackOrd,
            loaded: false,
          });
        });
      } else {
        // новая подтема
        const subThemeId = ++maxSubThemeId;
        data.subthemes.push({
          id: subThemeId,
          themeId,
          title: newSubTheme,
          ord: ++maxSubThemeOrd,
        });
        let maxTrackOrd = 0;
        obj[newSubTheme].forEach((nm) => {
          data.contents.push({
            id: ++maxTrackId,
            subthemeId: subThemeId,
            title: nm,
            path: `${themePath}/${newSubTheme}/${nm}.mp3`,
            ord: ++maxTrackOrd,
            album: '',
            artist: '',
            title1: '',
            loaded: false,
          });
        });
      }
    });
  }

  // Сортировка по ord — используется во всех списках.
  static sorted(list) {
    return [...list].sort((a, b) => a.ord - b.ord);
  }
}

export default Loader;
