#!/usr/bin/env node
/*
 * Привязывает Android-проект к Gradle 8.7.
 *
 * cordova-android 13 рассчитан на Gradle 8.7 (Groovy 3). Android Studio с
 * Gradle 9 (Groovy 4) падает на CordovaLib/cordova.gradle:
 * "unable to resolve class XmlParser".
 *
 * Скрипт создаёт platforms/android/gradle/wrapper/gradle-wrapper.properties,
 * после чего Android Studio скачивает и использует Gradle 8.7.
 */
const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const androidDir = path.join(root, 'platforms', 'android');

if (!fs.existsSync(androidDir)) {
  console.error(
    'Не найдена папка platforms/android.\n' +
      'Сначала выполните: npx cordova platform add android'
  );
  process.exit(1);
}

const wrapperDir = path.join(androidDir, 'gradle', 'wrapper');
const propertiesPath = path.join(wrapperDir, 'gradle-wrapper.properties');

const content = [
  'distributionBase=GRADLE_USER_HOME',
  'distributionPath=wrapper/dists',
  'distributionUrl=https\\://services.gradle.org/distributions/gradle-8.7-bin.zip',
  'zipStoreBase=GRADLE_USER_HOME',
  'zipStorePath=wrapper/dists',
  '',
].join('\n');

fs.mkdirSync(wrapperDir, { recursive: true });
fs.writeFileSync(propertiesPath, content, 'ascii');

console.log(`Gradle 8.7 прописан в:\n  ${propertiesPath}\n`);
console.log('Дальше в Android Studio:');
console.log('  Settings → Build, Execution, Deployment → Build Tools → Gradle');
console.log('  Use Gradle from = gradle-wrapper.properties file');
console.log('  File → Sync Project with Gradle Files');
