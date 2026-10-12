import { setupWorker } from 'msw/browser';
import { createHandlers, initMockUserFromUrl } from './handlers';

export async function startMockWorker(baseUrl: string) {
  initMockUserFromUrl(window.location.search);
  const worker = setupWorker(...createHandlers(baseUrl));
  await worker.start({
    onUnhandledRequest: 'bypass',
    serviceWorker: { url: `${import.meta.env.BASE_URL}mockServiceWorker.js` },
    quiet: !import.meta.env.DEV,
  });
}
