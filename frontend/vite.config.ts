/// <reference types="vitest/config" />
import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const proxyTarget = env.VITE_API_PROXY_TARGET;

  return {
    plugins: [react()],
    server: {
      port: 5173,
      strictPort: true,
      proxy: proxyTarget ? { '/api': { target: proxyTarget, changeOrigin: true } } : undefined,
    },
    preview: { port: 4173, strictPort: true },
    build: { sourcemap: true },
    test: {
      globals: true,
      environment: 'jsdom',
      setupFiles: ['./src/test/setup.ts'],
      include: ['src/**/*.test.{ts,tsx}'],
      css: false,
      env: { VITE_API_MODE: 'mock', VITE_API_BASE_URL: '/api/v1', VITE_MOCK_DELAY_MS: '0' },
    },
  };
});
