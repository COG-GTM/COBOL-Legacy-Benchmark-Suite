/**
 * Authentication domain types.
 *
 * Mirrors the legacy COBOL security model implemented by the SECMGR program
 * (src/programs/online/SECMGR.cbl) and the AUTHFILE / user credential records.
 * In the green-screen system SECMGR validates a user id (PIC X(8)) and checks
 * an ACCESS_TYPE (PIC X(8)) against the requested resource. Here we collapse
 * that into a coarse role used for client-side access gating.
 */

/**
 * Role granted to a user. Mirrors the admin vs. read-only distinction the
 * legacy SECMGR authorization check enforces via AUTHFILE.ACCESS_TYPE.
 */
export type UserRole = 'ADMIN' | 'READONLY';

/**
 * A user record as stored in the mock fixture. The `password` field exists only
 * in the fixture/credential store and is never exposed on the authenticated
 * session (see {@link AuthUser}).
 *
 * `userId` is constrained to 8 characters to match the COBOL `USER-ID PIC X(8)`
 * definition used throughout SECMGR and the audit copybook.
 */
export interface UserCredential {
  /** Login id, max 8 chars (mirrors COBOL USER-ID PIC X(8)). */
  userId: string;
  /** Plaintext password for the mock fixture only. */
  password: string;
  /** Human-readable display name. */
  displayName: string;
  /** Access role used for route gating. */
  role: UserRole;
}

/**
 * The authenticated user as held in session state. Excludes the password.
 */
export type AuthUser = Omit<UserCredential, 'password'>;

/** Result of an authentication attempt against the credential store. */
export type AuthResult =
  | { ok: true; user: AuthUser }
  | { ok: false; reason: 'INVALID_CREDENTIALS' | 'EMPTY_INPUT' };
