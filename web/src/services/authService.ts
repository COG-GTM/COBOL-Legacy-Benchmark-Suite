import type { AuthResult, AuthUser, UserCredential } from '../types/auth';
import { MOCK_USERS } from '../mocks/users';

/**
 * Authentication service.
 *
 * Front-end stand-in for the SECMGR credential validation flow
 * (P100-VALIDATE-USER in src/programs/online/SECMGR.cbl). The async signature
 * and {@link AuthResult} shape define the integration point so the mock
 * fixture can later be swapped for a real backend call without touching
 * callers.
 */

function toAuthUser(credential: UserCredential): AuthUser {
  const { password: _password, ...authUser } = credential;
  void _password;
  return authUser;
}

export interface AuthService {
  authenticate(userId: string, password: string): Promise<AuthResult>;
}

const mockAuthService: AuthService = {
  async authenticate(userId, password) {
    const trimmedId = userId.trim();
    if (trimmedId === '' || password === '') {
      return { ok: false, reason: 'EMPTY_INPUT' };
    }

    // User ids are case-insensitive (mirrors CICS USERID handling); passwords
    // are compared exactly.
    const match = MOCK_USERS.find(
      (user) =>
        user.userId.toUpperCase() === trimmedId.toUpperCase() &&
        user.password === password,
    );

    if (!match) {
      return { ok: false, reason: 'INVALID_CREDENTIALS' };
    }

    return { ok: true, user: toAuthUser(match) };
  },
};

export const authService: AuthService = mockAuthService;
