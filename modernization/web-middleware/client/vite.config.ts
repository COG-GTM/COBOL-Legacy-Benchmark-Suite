import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'node:path';

// The theme tokens module lives at modernization/web-middleware/src/theme/ocbc.ts
const themeDir = path.resolve(__dirname, '../src');

export default defineConfig({
  plugins: [react()],
  resolve: { alias: { '@theme': path.resolve(themeDir, 'theme/ocbc.ts') } },
  server: {
    host: '0.0.0.0',
    port: 5173,
    fs: { allow: [__dirname, themeDir] },
    proxy: { '/api': { target: process.env.API_URL || 'http://localhost:4000', changeOrigin: true } },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './tests/setup.ts',
    include: ['tests/**/*.test.tsx'],
  },
});
