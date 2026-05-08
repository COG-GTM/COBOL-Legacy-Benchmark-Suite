import {
  AuditType,
  AuditAction,
  AuditStatus,
  HTTP_METHOD_TO_ACTION,
  AUTH_ACTION_PATTERNS,
  MAX_IMAGE_SIZE,
  SYSTEM_ID,
} from '../../types/audit';

describe('AuditType enum', () => {
  it('should have correct COBOL-mapped values', () => {
    expect(AuditType.TRANSACTION).toBe('TRAN');
    expect(AuditType.USER_ACTION).toBe('USER');
    expect(AuditType.SYSTEM_EVENT).toBe('SYST');
  });

  it('should have exactly 3 values', () => {
    expect(Object.values(AuditType)).toHaveLength(3);
  });
});

describe('AuditAction enum', () => {
  it('should have correct COBOL-mapped values', () => {
    expect(AuditAction.CREATE).toBe('CREATE');
    expect(AuditAction.UPDATE).toBe('UPDATE');
    expect(AuditAction.DELETE).toBe('DELETE');
    expect(AuditAction.INQUIRE).toBe('INQUIRE');
    expect(AuditAction.LOGIN).toBe('LOGIN');
    expect(AuditAction.LOGOUT).toBe('LOGOUT');
    expect(AuditAction.STARTUP).toBe('STARTUP');
    expect(AuditAction.SHUTDOWN).toBe('SHUTDOWN');
  });

  it('should have exactly 8 values', () => {
    expect(Object.values(AuditAction)).toHaveLength(8);
  });
});

describe('AuditStatus enum', () => {
  it('should have correct COBOL-mapped values', () => {
    expect(AuditStatus.SUCCESS).toBe('SUCC');
    expect(AuditStatus.FAILURE).toBe('FAIL');
    expect(AuditStatus.WARNING).toBe('WARN');
  });

  it('should have exactly 3 values', () => {
    expect(Object.values(AuditStatus)).toHaveLength(3);
  });
});

describe('HTTP_METHOD_TO_ACTION mapping', () => {
  it('should map GET to INQUIRE', () => {
    expect(HTTP_METHOD_TO_ACTION['GET']).toBe(AuditAction.INQUIRE);
  });

  it('should map HEAD to INQUIRE', () => {
    expect(HTTP_METHOD_TO_ACTION['HEAD']).toBe(AuditAction.INQUIRE);
  });

  it('should map POST to CREATE', () => {
    expect(HTTP_METHOD_TO_ACTION['POST']).toBe(AuditAction.CREATE);
  });

  it('should map PUT to UPDATE', () => {
    expect(HTTP_METHOD_TO_ACTION['PUT']).toBe(AuditAction.UPDATE);
  });

  it('should map PATCH to UPDATE', () => {
    expect(HTTP_METHOD_TO_ACTION['PATCH']).toBe(AuditAction.UPDATE);
  });

  it('should map DELETE to DELETE', () => {
    expect(HTTP_METHOD_TO_ACTION['DELETE']).toBe(AuditAction.DELETE);
  });
});

describe('AUTH_ACTION_PATTERNS', () => {
  it('should match login paths', () => {
    const loginPattern = AUTH_ACTION_PATTERNS.find(
      (p) => p.action === AuditAction.LOGIN,
    );
    expect(loginPattern).toBeDefined();
    expect(loginPattern!.pattern.test('/auth/login')).toBe(true);
    expect(loginPattern!.pattern.test('/api/auth/login')).toBe(true);
    expect(loginPattern!.pattern.test('/auth/loginx')).toBe(false);
  });

  it('should match logout paths', () => {
    const logoutPattern = AUTH_ACTION_PATTERNS.find(
      (p) => p.action === AuditAction.LOGOUT,
    );
    expect(logoutPattern).toBeDefined();
    expect(logoutPattern!.pattern.test('/auth/logout')).toBe(true);
  });
});

describe('Constants', () => {
  it('should have MAX_IMAGE_SIZE of 10KB', () => {
    expect(MAX_IMAGE_SIZE).toBe(10240);
  });

  it('should have SYSTEM_ID set to PORTFOLIO-API', () => {
    expect(SYSTEM_ID).toBe('PORTFOLIO-API');
  });
});
