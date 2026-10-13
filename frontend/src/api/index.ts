import { createHttpApi } from './http';
import { createMockApi } from './mock';
import type { PortfolioApi } from './types';

const baseUrl = import.meta.env.VITE_API_BASE_URL as string | undefined;

export const api: PortfolioApi = baseUrl ? createHttpApi(baseUrl) : createMockApi();
export const usingMockData = !baseUrl;
