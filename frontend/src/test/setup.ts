import '@testing-library/jest-dom/vitest';

// Mock sessionStorage for AuthContext tests
const store: Record<string, string> = {};
const mockSessionStorage = {
  getItem: (key: string) => store[key] ?? null,
  setItem: (key: string, value: string) => { store[key] = value; },
  removeItem: (key: string) => { delete store[key]; },
  clear: () => { Object.keys(store).forEach((k) => delete store[k]); },
  get length() { return Object.keys(store).length; },
  key: (i: number) => Object.keys(store)[i] ?? null,
};
Object.defineProperty(window, 'sessionStorage', { value: mockSessionStorage });

// Mock URL.createObjectURL / revokeObjectURL for CSV download tests
URL.createObjectURL = vi.fn(() => 'blob:mock');
URL.revokeObjectURL = vi.fn();

// Mock requestAnimationFrame / cancelAnimationFrame for ErrorToast
window.requestAnimationFrame = (cb: FrameRequestCallback) => {
  const id = setTimeout(() => cb(Date.now()), 0);
  return id as unknown as number;
};
window.cancelAnimationFrame = (id: number) => clearTimeout(id);
