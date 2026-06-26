import { describe, expect, it } from 'vitest';
import { authService } from './authService';

describe('authService.authenticate', () => {
  it('authenticates a valid admin user and omits the password', async () => {
    const result = await authService.authenticate('ADMIN001', 'admin123');
    expect(result.ok).toBe(true);
    if (result.ok) {
      expect(result.user.role).toBe('ADMIN');
      expect(result.user.userId).toBe('ADMIN001');
      expect((result.user as Record<string, unknown>).password).toBeUndefined();
    }
  });

  it('authenticates a read-only user', async () => {
    const result = await authService.authenticate('READ0001', 'readonly123');
    expect(result.ok).toBe(true);
    if (result.ok) expect(result.user.role).toBe('READONLY');
  });

  it('treats the user id as case-insensitive', async () => {
    const result = await authService.authenticate('admin001', 'admin123');
    expect(result.ok).toBe(true);
  });

  it('rejects a wrong password', async () => {
    const result = await authService.authenticate('ADMIN001', 'nope');
    expect(result).toEqual({ ok: false, reason: 'INVALID_CREDENTIALS' });
  });

  it('rejects an unknown user', async () => {
    const result = await authService.authenticate('NOBODY', 'whatever');
    expect(result).toEqual({ ok: false, reason: 'INVALID_CREDENTIALS' });
  });

  it('reports empty input', async () => {
    const result = await authService.authenticate('  ', '');
    expect(result).toEqual({ ok: false, reason: 'EMPTY_INPUT' });
  });
});
