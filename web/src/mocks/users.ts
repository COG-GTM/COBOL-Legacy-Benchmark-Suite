import type { UserCredential } from '../types/auth';

/**
 * Mock user credential fixture.
 *
 * Stands in for the legacy AUTHFILE / RACF user registry that SECMGR validates
 * against. User ids are kept to 8 characters to match COBOL USER-ID PIC X(8).
 *
 * NOTE: this is a front-end-only fixture for the modernization prototype. Real
 * authentication will be delegated to a backend service via the same
 * {@link authService} interface (see services/authService.ts).
 */
export const MOCK_USERS: readonly UserCredential[] = [
  {
    userId: 'ADMIN001',
    password: 'admin123',
    displayName: 'Alice Admin',
    role: 'ADMIN',
  },
  {
    userId: 'PMGR0001',
    password: 'manage123',
    displayName: 'Paula Manager',
    role: 'ADMIN',
  },
  {
    userId: 'READ0001',
    password: 'readonly123',
    displayName: 'Ravi Reader',
    role: 'READONLY',
  },
  {
    userId: 'ANALYST1',
    password: 'analyst123',
    displayName: 'Ana Analyst',
    role: 'READONLY',
  },
] as const;
