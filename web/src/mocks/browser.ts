import { setupWorker } from 'msw/browser';
import { handlers } from './handlers';

/** MSW worker used in the browser to intercept the mock API requests. */
export const worker = setupWorker(...handlers);
