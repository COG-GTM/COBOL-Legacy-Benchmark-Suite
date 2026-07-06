import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { RouterProvider } from 'react-router-dom';
import { router } from './App';
import './index.css';

/**
 * Start the MSW mock API worker before rendering. This makes the whole UI
 * runnable end-to-end without the mainframe backend. Swap this out (or point
 * VITE_API_BASE_URL at a real gateway) once a z/OS Connect / CICS Web Services
 * backend is available.
 */
async function enableMocking(): Promise<void> {
  const { worker } = await import('./mocks/browser');
  await worker.start({
    onUnhandledRequest: 'bypass',
    quiet: true,
  });
}

enableMocking().then(() => {
  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <RouterProvider router={router} />
    </StrictMode>,
  );
});
