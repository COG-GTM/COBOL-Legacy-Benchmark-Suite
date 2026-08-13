package com.ipms.domain;

import com.ipms.common.fixedwidth.FixedWidthReader;
import com.ipms.common.fixedwidth.FixedWidthWriter;

import java.math.BigDecimal;

/**
 * ERR-MESSAGE structure from {@code src/copybook/common/ERRHAND.cpy}.
 *
 * <pre>
 * ERR-TIMESTAMP: ERR-DATE X(10), ERR-TIME X(8)
 * ERR-PROGRAM X(8), ERR-CATEGORY X(2), ERR-CODE X(4),
 * ERR-SEVERITY S9(4) COMP, ERR-TEXT X(80), ERR-DETAILS X(256)
 * </pre>
 */
public record ErrorMessage(
        String errDate,
        String errTime,
        String errProgram,
        ErrorCategory errCategory,
        String errCode,
        int errSeverity,
        String errText,
        String errDetails) {

    public static final int RECORD_LENGTH = 373;

    public static ErrorMessage parse(String record) {
        FixedWidthReader r = new FixedWidthReader(record);
        return new ErrorMessage(
                r.string(10),
                r.string(8),
                r.string(8),
                ErrorCategory.fromCode(r.string(2)),
                r.string(4),
                r.signedDecimal(4, 0).intValueExact(),
                r.string(80),
                r.string(256));
    }

    public String toRecord() {
        return new FixedWidthWriter(RECORD_LENGTH)
                .string(errDate, 10)
                .string(errTime, 8)
                .string(errProgram, 8)
                .string(errCategory.code(), 2)
                .string(errCode, 4)
                .signedDecimal(BigDecimal.valueOf(errSeverity), 4, 0)
                .string(errText, 80)
                .string(errDetails, 256)
                .toRecord();
    }
}
