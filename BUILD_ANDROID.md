# Сборка APK

Проект — обычное Cordova-приложение: `config.xml` в корне, готовая веб-сборка в
`www/`. Плагины перечислены в `package.json`, поэтому их не нужно добавлять
вручную.

## Что нужно

- Node.js 18+ (проверено на Node 22)
- JDK 17 (cordova-android 13 не собирается на JDK 8/11)
- Android SDK: Platform 34, Build-Tools 34.0.0
- Переменная окружения `ANDROID_HOME` (или `ANDROID_SDK_ROOT`)

Самый простой путь — установить [Android Studio](https://developer.android.com/studio),
открыть SDK Manager и отметить **Android 14.0 (API 34)** и **Android SDK Build-Tools 34**.

Проверка:

```bash
java -version        # должно быть 17.x
echo $ANDROID_HOME   # путь к Android/Sdk
```

## Открыть проект в Android Studio

Android Studio открывает не корень репозитория, а сгенерированную Android-платформу.
Папка `platforms/` не хранится в git, её нужно создать один раз.

1. Подготовьте платформу:

   ```bash
   npm install
   npm run build
   npx cordova platform add android
   ```

2. Запустите Android Studio → **Open** и выберите папку:

   ```
   <путь-к-проекту>/platforms/android
   ```

   Не выбирайте корень проекта (там нет Gradle-проекта) и не `platforms/android/app`.
   Если появится запрос — **Trust Project**.

3. Дождитесь окончания **Gradle Sync**.

4. Проверьте SDK: **Settings → Languages & Frameworks → Android SDK**:
   - SDK Platforms: **Android 14.0 (API 34)**;
   - SDK Tools: **Android SDK Build-Tools 34.0.0**.

5. Проверьте JDK: **Settings → Build, Execution, Deployment → Build Tools → Gradle
   → Gradle JDK**. Для cordova-android 13 нужен **JDK 17**. Встроенный в новые
   Android Studio JBR 21 обычно тоже подходит; если Sync падает с ошибкой Java —
   выберите JDK 17.

6. Вверху выберите конфигурацию **app**, эмулятор или подключённый телефон и нажмите
   **Run**. Если конфигурации нет: **Run → Edit Configurations → + → Android App**,
   module `app`, Launch: `Default Activity`.

### После изменения веб-кода

Android Studio собирает уже подготовленные файлы из
`platforms/android/app/src/main/assets/www`, поэтому после правок в `src/`:

```bash
npm run build
npx cordova prepare android
```

и снова **Run** (при необходимости **Build → Rebuild Project**). Папку
`platforms/android` вручную не редактируйте — она генерируется Cordova.

### Полезно в Android Studio

- APK: **Build → Build App Bundle(s)/APK(s) → Build APK(s)**. Файл:
  `platforms/android/app/build/outputs/apk/debug/app-debug.apk`.
- Геолокация в эмуляторе: **Extended Controls → Location** — задайте координаты и
  нажмите **Send**.
- Батарея в эмуляторе: **Extended Controls → Battery** — меняйте **Charge level** и
  **Charger connection** (AC/USB/None), чтобы получить `batterystatus`.
- Offline в эмуляторе: включите авиарежим или выполните
  `adb shell cmd connectivity airplane-mode enable`.

### Если Gradle Sync ругается на wrapper

Cordova 13 создаёт Gradle wrapper системным Gradle при первой сборке. Если Android
Studio пишет, что wrapper не найден:

- установите Gradle 8.7 и выполните `npx cordova build android` (появится
  `platforms/android/tools/gradlew`), затем **File → Sync Project with Gradle Files**;
- либо в **Settings → Build Tools → Gradle → Use Gradle from** выберите локальную
  установку Gradle / встроенный Gradle Android Studio.

## Быстрая сборка debug APK

```bash
# 1. зависимости
npm install

# 2. веб-сборка в www/
npm run build

# 3. android-платформа (один раз; плагины установятся из package.json)
npx cordova platform add android

# 4. сборка
npx cordova build android
```

Готовый файл:

```
platforms/android/app/build/outputs/apk/debug/app-debug.apk
```

Он подписан debug-ключом и устанавливается на телефон:

```bash
adb install -r platforms/android/app/build/outputs/apk/debug/app-debug.apk
```

Либо одной командой (сборка www + APK):

```bash
npm run cordova:android
```

Если папка `platforms/android` уже есть, `platform add` повторять не нужно.
После изменения `config.xml` (например, permissions) выполните
`npx cordova platform rm android && npx cordova platform add android` или
`npx cordova prepare android`.

## Release APK

1. Создайте keystore (один раз):

```bash
keytool -genkeypair -v -keystore vasilev-lab.keystore \
  -alias vasilevlab -keyalg RSA -keysize 2048 -validity 10000
```

2. Создайте `build.json` в корне проекта:

```json
{
  "android": {
    "release": {
      "keystore": "vasilev-lab.keystore",
      "storePassword": "ПАРОЛЬ_ХРАНИЛИЩА",
      "alias": "vasilevlab",
      "password": "ПАРОЛЬ_КЛЮЧА",
      "packageType": "apk"
    }
  }
}
```

3. Соберите:

```bash
npm run build
npx cordova build android --release
```

APK появится в:

```
platforms/android/app/build/outputs/apk/release/app-release.apk
```

`build.json` и keystore не коммитьте в публичный репозиторий (они уже в
`.gitignore`).

## Сборка через GitHub Actions

В репозитории есть `.github/workflows/android-apk.yml`. Он запускается вручную
(**Actions → Build Android APK → Run workflow**) или при push в `main`.

Workflow:

1. ставит Node.js 20 и JDK 17;
2. ставит Android SDK 34;
3. выполняет `npm ci`, `npm run build`, `cordova platform add android`,
   `cordova build android`;
4. загружает `app-debug.apk` как артефакт **vasilev-lab-debug-apk**.

## Частые проблемы

**`JAVA_HOME is not set` / `Unsupported class file major version`** — установлена
неподходящая Java. Нужна JDK 17, проверьте `java -version` и `JAVA_HOME`.

**`Failed to find target with hash string 'android-34'`** — не установлен
SDK Platform 34. Установите через SDK Manager или:

```bash
sdkmanager "platforms;android-34" "build-tools;34.0.0"
sdkmanager --licenses
```

**`SDK location not found`** — задайте `ANDROID_HOME`:

```bash
# Linux/macOS
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools
```

**Белый экран после запуска** — проверьте, что `www/index.html` существует
(`npm run build`) и выполнен `npx cordova prepare android`. Логи:

```bash
adb logcat | grep -i -E "chromium|capacitor|cordova"
```

**Геолокация не работает на эмуляторе** — задайте координаты через Extended
Controls → Location и разрешите доступ в настройках приложения.

**Серийный номер пустой** — на Android 10+ `Build.SERIAL` недоступен
приложениям, это ограничение системы, а не ошибка приложения.
