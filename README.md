# Громкость голоса (Voice Decibel Meter)

Простое Android-приложение на Kotlin и Jetpack Compose, которое сразу после запуска измеряет громкость голоса с микрофона в децибелах (дБ) и отображает её на плавном графике в реальном времени.

## Возможности

- **Мгновенный старт:** при наличии разрешения на микрофон измерение и график запускаются автоматически сразу при открытии приложения.
- **График в реальном времени:** скользящее окно 15 секунд (20 обновлений/сек), цветовая градация от тишины до громкой речи и линия пикового максимума.
- **Фильтрация под голос:** ВЧ-фильтр (~85 Гц) отсекает постоянную составляющую и низкочастотный гул/ветер, сохраняя голосовой диапазон.
- **Статистика за сессию:** минимум (`Мин`), среднее (`Сред`) и максимум (`Макс`) в дБ.
- **Простое управление:** кнопки **Пауза / Включить**, **Сброс** графика и статистики, а также диалог **Калибровки** (`±20 дБ`).
- **Приватность:** аудиопоток обрабатывается только в оперативной памяти устройства, ничего не записывается в файлы и не отправляется в сеть.

## Сборка и запуск

### В Android Studio
1. Откройте корневую папку репозитория в **Android Studio** (Ladybug / Meerkat или новее, JDK 17).
2. Дождитесь синхронизации Gradle.
3. Нажмите **Run** (`Shift + F10`) для запуска на телефоне или эмуляторе (Android 7.0+, `minSdk 24`).

### Через консоль (Gradle)
```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```
Готовый APK будет создан по пути:
`app/build/outputs/apk/debug/app-debug.apk`

### Готовый APK из GitHub Actions
При пуше в репозиторий запускается workflow **Android APK Build** (`.github/workflows/android-build.yml`), который прогоняет unit-тесты, lint и выкладывает готовый `VoiceDecibelMeter-debug-apk` в разделе **Actions → Artifacts**.

## Структура проекта

- `app/src/main/java/com/vasilev/voicedb/audio/DecibelCalculator.kt` — ВЧ-фильтр, расчёт RMS → dBFS → дБ SPL и сглаживание показаний.
- `app/src/main/java/com/vasilev/voicedb/audio/VoiceMeterEngine.kt` — захват аудиопотока через `AudioRecord` (44.1 / 16 кГц, моно PCM 16-bit).
- `app/src/main/java/com/vasilev/voicedb/ui/VoiceStatsTracker.kt` — трекер истории и статистики (мин/сред/макс).
- `app/src/main/java/com/vasilev/voicedb/ui/VoiceMeterViewModel.kt` — управление состоянием, жизненным циклом и калибровкой.
- `app/src/main/java/com/vasilev/voicedb/ui/components/DecibelGraph.kt` — кастомный Canvas-график в реальном времени.
- `app/src/main/java/com/vasilev/voicedb/ui/VoiceMeterScreen.kt` — основной экран приложения.
- `app/src/test/java/com/vasilev/voicedb/` — модульные unit-тесты (JUnit4).
