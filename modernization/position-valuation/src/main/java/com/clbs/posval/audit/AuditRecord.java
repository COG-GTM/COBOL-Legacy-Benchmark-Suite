package com.clbs.posval.audit;

/**
 * {@code AUDIT-RECORD} of {@code src/copybook/common/AUDITLOG.cpy}, written by {@code AUDPROC}.
 *
 * @param timestamp {@code AUD-TIMESTAMP PIC X(26)}
 * @param systemId {@code AUD-SYSTEM-ID PIC X(8)}
 * @param userId {@code AUD-USER-ID PIC X(8)}
 * @param program {@code AUD-PROGRAM PIC X(8)}
 * @param terminal {@code AUD-TERMINAL PIC X(8)}
 * @param type {@code AUD-TYPE PIC X(4)} — {@code TRAN}, {@code USER} or {@code SYST}
 * @param action {@code AUD-ACTION PIC X(8)} — {@code CREATE}, {@code UPDATE}, {@code DELETE}, …
 * @param status {@code AUD-STATUS PIC X(4)} — {@code SUCC}, {@code FAIL} or {@code WARN}
 * @param portfolioId {@code AUD-PORTFOLIO-ID PIC X(8)}
 * @param accountNo {@code AUD-ACCOUNT-NO PIC X(10)}
 * @param beforeImage {@code AUD-BEFORE-IMAGE PIC X(100)}
 * @param afterImage {@code AUD-AFTER-IMAGE PIC X(100)}
 * @param message {@code AUD-MESSAGE PIC X(100)}
 */
public record AuditRecord(
        String timestamp,
        String systemId,
        String userId,
        String program,
        String terminal,
        String type,
        String action,
        String status,
        String portfolioId,
        String accountNo,
        String beforeImage,
        String afterImage,
        String message) {

    public static final String TYPE_TRANSACTION = "TRAN";
    public static final String ACTION_CREATE = "CREATE  ";
    public static final String ACTION_UPDATE = "UPDATE  ";
    public static final String ACTION_DELETE = "DELETE  ";
    public static final String STATUS_SUCCESS = "SUCC";
    public static final String STATUS_FAILURE = "FAIL";
}
