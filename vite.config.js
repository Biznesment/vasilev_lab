import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Cordova берёт готовое приложение из папки www.
export default defineConfig({
  base: './',
  plugins: [react()],
  build: {
    outDir: 'www',
    emptyOutDir: true,
    target: 'es2018',
    assetsDir: 'assets',
    sourcemap: false,
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    strictPort: true,
    allowedHosts: true,
    watch: {
      // не перезагружать страницу из-за Cordova-платформы и готовой сборки
      ignored: ['**/platforms/**', '**/plugins/**', '**/www/**'],
    },
  },
  preview: {
    host: '0.0.0.0',
    port: 4173,
    allowedHosts: true,
  },
});
