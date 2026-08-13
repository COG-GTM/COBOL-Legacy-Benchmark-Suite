package com.ipms.domain;

import com.ipms.common.fixedwidth.FixedWidthReader;
import com.ipms.common.fixedwidth.FixedWidthWriter;

/**
 * AUDIT-RECORD from {@code src/copybook/common/AUDITLOG.cpy}.
 *
 * <pre>
 * AUD-HEADER:   AUD-TIMESTAMP X(26), AUD-SYSTEM-ID X(8), AUD-USER-ID X(8),
 *               AUD-PROGRAM X(8), AUD-TERMINAL X(8)
 * AUD-TYPE X(4), AUD-ACTION X(8), AUD-STATUS X(4)
 * AUD-KEY-INFO: AUD-PORTFOLIO-ID X(8), AUD-ACCOUNT-NO X(10)
 * AUD-BEFORE-IMAGE X(100), AUD-AFTER-IMAGE X(100), AUD-MESSAGE X(100)
 * </pre>
 */
public record AuditRecord(
        String audTimestamp,
        String audSystemId,
        String audUserId,
        String audProgram,
        String audTerminal,
        AuditType audType,
        AuditAction audAction,
        AuditStatus audStatus,
        String audPortfolioId,
        String audAccountNo,
        String audBeforeImage,
        String audAfterImage,
        String audMessage) {

    public static final int RECORD_LENGTH = 392;

    public static AuditRecord parse(String record) {
        FixedWidthReader r = new FixedWidthReader(record);
        return new AuditRecord(
                r.string(26),
                r.string(8),
                r.string(8),
                r.string(8),
                r.string(8),
                AuditType.fromCode(r.string(4)),
                AuditAction.fromCode(r.rawString(8)),
                AuditStatus.fromCode(r.string(4)),
                r.string(8),
                r.string(10),
                r.string(100),
                r.string(100),
                r.string(100));
    }

    public String toRecord() {
        return new FixedWidthWriter(RECORD_LENGTH)
                .string(audTimestamp, 26)
                .string(audSystemId, 8)
                .string(audUserId, 8)
                .string(audProgram, 8)
                .string(audTerminal, 8)
                .string(audType.code(), 4)
                .string(audAction.code(), 8)
                .string(audStatus.code(), 4)
                .string(audPortfolioId, 8)
                .string(audAccountNo, 10)
                .string(audBeforeImage, 100)
                .string(audAfterImage, 100)
                .string(audMessage, 100)
                .toRecord();
    }
}
