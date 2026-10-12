import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { AppRoutes } from './App';
import { apiConfig } from './api/client';
import { ErrorBoundary } from './components/ErrorBoundary';
import { SessionProvider } from './state/SessionContext';
import './styles.css';

async function bootstrap() {
  if (apiConfig.mode === 'mock') {
    const { startMockWorker } = await import('./mocks/browser');
    await startMockWorker(apiConfig.baseUrl);
  }

  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <ErrorBoundary>
        <BrowserRouter>
          <SessionProvider>
            <AppRoutes />
          </SessionProvider>
        </BrowserRouter>
      </ErrorBoundary>
    </StrictMode>,
  );
}

void bootstrap();
