# Vasilev Lab — Cordova + React + Onsen UI

Учебное Cordova-приложение с интерфейсом на React + Onsen UI.

Реализовано:

- **Главный экран** — карусель текстовых сообщений (`ons-carousel`) с перелистыванием
  свайпом, точками-индикаторами и кнопками «назад / вперёд» — как в галерее Android.
- **Уведомление об offline** — при потере сети показывается toast и красная плашка
  «Вы offline» на главном экране; состояние сети также видно в боковом меню и в тулбаре.
- **Боковое меню** — открывается по иконке ☰, содержит пункты: «Сообщения»,
  «Device Info», «Геолокация», «Статус батареи».
- **Device Info** — версия Android, версия Cordova, модель, платформа, UUID,
  Device version, производитель, серийный номер, запуск на симуляторе, SDK version.
- **Геолокация** — отдельная вкладка с опциями `enableHighAccuracy`, `timeout`,
  `maximumAge`; настройки сохраняются в IndexedDB и переживают перезапуск; вкладка
  «Диагностика» с координатами, ошибками и журналом; одна кнопка
  старт/стоп `navigator.geolocation.watchPosition`.
- **Статус батареи** — обработка `batterystatus`, `batterylow`, `batterycritical`,
  вывод заряда в %, состояние питания, иконка шнура (подключено/отключено) и журнал
  событий.

## Технологии

- Cordova 12 / cordova-android 13 (targetSdk 34, minSdk 24)
- React 18
- Onsen UI 2 + `react-onsenui`
- Vite 5 (сборка кладётся в `www`)

## Структура проекта

```
.
├── config.xml                 # конфигурация Cordova (id, permissions, плагины)
├── index.html                 # точка входа Vite
├── vite.config.js             # сборка в www/
├── src/
│   ├── App.jsx                # Splitter, боковое меню, тулбар, offline-уведомления
│   ├── pages/
│   │   ├── MessagesPage.jsx   # карусель сообщений
│   │   ├── DeviceInfoPage.jsx # device info
│   │   ├── GeolocationPage.jsx# опции + диагностика
│   │   └── BatteryPage.jsx    # статус батареи
│   ├── services/
│   │   ├── deviceInfo.js      # cordova-plugin-device + sdkVersion
│   │   ├── geolocation.js     # singleton watchPosition
│   │   ├── network.js         # online/offline
│   │   └── settings.js        # IndexedDB (fallback: localStorage)
│   ├── hooks/useBatteryStatus.js
│   ├── components/            # SVG-иконки, строки таблиц
│   ├── data/messages.js
│   └── styles.css
├── www/                       # готовая веб-сборка (используется Cordova)
└── .github/workflows/android-apk.yml  # сборка APK в GitHub Actions
```

## Запуск в браузере

```bash
npm install
npm run dev
```

Откройте `http://localhost:5173`. Это удобно для проверки интерфейса:

- Device Info покажет приблизительные данные из `navigator.userAgent`;
- геолокация использует браузерный `navigator.geolocation`;
- батарея — `navigator.getBattery()` (Chrome/Edge), на странице есть кнопки
  «Демонстрация событий»;
- карусель и меню работают полностью.

В браузере Cordova-плагинов нет, поэтому значения помечены как fallback. В APK
используются реальные плагины.

## Сборка APK

> Готовый APK в этой папке не приложен: в среде подготовки проекта не было Java и
> Android SDK. Есть два рабочих способа собрать APK — через GitHub Actions или
> локально. Подробная инструкция: [BUILD_ANDROID.md](BUILD_ANDROID.md).

### Способ 1. GitHub Actions (APK собирается автоматически)

1. Запушьте репозиторий на GitHub.
2. Откройте **Actions → Build Android APK → Run workflow**.
3. После сборки скачайте артефакт **vasilev-lab-debug-apk** — внутри
   `app-debug.apk`.

Workflow ставит Node.js, JDK 17 и Android SDK 34, собирает `www` и запускает
`cordova build android`.

### Способ 2. Локально

Нужны Node.js 18+, JDK 17 и Android SDK (проще всего установить Android Studio
и SDK Platform 34).

```bash
npm install
npm run build
npx cordova platform add android
npx cordova build android
```

Готовый APK:

```
platforms/android/app/build/outputs/apk/debug/app-debug.apk
```

Можно одной командой: `npm run cordova:android`.

Установка на подключённый телефон:

```bash
adb install -r platforms/android/app/build/outputs/apk/debug/app-debug.apk
```

## Плагины Cordova

| Плагин | Назначение |
|---|---|
| `cordova-plugin-device` | Device Info, `device.sdkVersion` |
| `cordova-plugin-geolocation` | `navigator.geolocation.watchPosition` |
| `cordova-plugin-battery-status` | `batterystatus`, `batterylow`, `batterycritical` |
| `cordova-plugin-network-information` | определение offline и типа сети |

Плагины перечислены в `package.json` → `cordova.plugins`, поэтому при
`cordova platform add android` они устанавливаются автоматически.

## Как это устроено

### Карусель сообщений

`ons-carousel` в режиме `fullscreen` + `swipeable`. Индекс синхронизирован с
точками и кнопками (`setActiveIndex`), `onPostChange` обновляет счётчик. На
компьютере работают стрелки клавиатуры.

### Offline

`src/services/network.js` подписывается на `online`/`offline` (window и document)
и раз в 5 секунд проверяет `navigator.connection.type`. При переходе
`online → offline` показывается toast, на главной странице — плашка.

### Device Info

Данные берутся из `window.device` (`cordova-plugin-device`). SDK version
предпочтительно берётся из `device.sdkVersion` (плагин отдаёт его на Android),
иначе вычисляется по версии Android. Серийный номер на Android 10+ недоступен
обычным приложениям — это ожидаемо, в интерфейсе выводится «недоступен».

### Геолокация

Настройки хранятся в IndexedDB (`vasilev-lab` → store `settings`), при
недоступности — в `localStorage`. Сервис `geolocation.js` держит watchPosition
вне React-компонентов, поэтому наблюдение продолжается при переходах между
экранами; остановка — той же кнопкой. В диагностике видны применённые опции,
`watchId`, количество обновлений, последняя позиция, ошибки и журнал.

### Батарея

События слушаются на `window`: `batterystatus`, `batterylow`,
`batterycritical`. `batterystatus` приходит при изменении заряда минимум на 1%
и при подключении/отключении питания — приложение не фильтрует события, все
попадают в журнал. Иконка шнура: зелёная — питание подключено, серая с
перечёркиванием — отключено.

## Примечания

- `cordova-plugin-geolocation` добавляет `ACCESS_COARSE_LOCATION` и
  `ACCESS_FINE_LOCATION`; разрешение запрашивается при первом старте
  `watchPosition`.
- `GPS_REQUIRED=false`, чтобы приложение устанавливалось и на устройства без GPS.
- Ориентация зафиксирована портретная (`config.xml`), minSdk 24, targetSdk 34.
- Для release-сборки нужен keystore — см. [BUILD_ANDROID.md](BUILD_ANDROID.md).
