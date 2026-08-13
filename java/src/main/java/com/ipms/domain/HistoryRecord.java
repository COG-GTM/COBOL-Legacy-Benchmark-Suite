package com.ipms.domain;

import com.ipms.common.fixedwidth.FixedWidthReader;
import com.ipms.common.fixedwidth.FixedWidthWriter;

/**
 * HISTORY-RECORD from {@code src/copybook/common/HISTREC.cpy}.
 *
 * <pre>
 * HIST-KEY:   HIST-PORTFOLIO-ID X(8), HIST-DATE X(8), HIST-TIME X(6), HIST-SEQ-NO X(4)
 * HIST-DATA:  HIST-RECORD-TYPE X(2), HIST-ACTION-CODE X(1),
 *             HIST-BEFORE-IMAGE X(400), HIST-AFTER-IMAGE X(400), HIST-REASON-CODE X(4)
 * HIST-AUDIT: HIST-PROCESS-DATE X(26), HIST-PROCESS-USER X(8)
 * HIST-FILLER X(50)
 * </pre>
 */
public record HistoryRecord(
        String histPortfolioId,
        String histDate,
        String histTime,
        String histSeqNo,
        HistoryRecordType histRecordType,
        HistoryActionCode histActionCode,
        String histBeforeImage,
        String histAfterImage,
        String histReasonCode,
        String histProcessDate,
        String histProcessUser) {

    public static final int RECORD_LENGTH = 917;

    public static HistoryRecord parse(String record) {
        FixedWidthReader r = new FixedWidthReader(record);
        return new HistoryRecord(
                r.string(8),
                r.string(8),
                r.string(6),
                r.string(4),
                HistoryRecordType.fromCode(r.string(2)),
                HistoryActionCode.fromCode(r.string(1)),
                r.string(400),
                r.string(400),
                r.string(4),
                r.string(26),
                r.string(8));
    }

    public String toRecord() {
        return new FixedWidthWriter(RECORD_LENGTH)
                .string(histPortfolioId, 8)
                .string(histDate, 8)
                .string(histTime, 6)
                .string(histSeqNo, 4)
                .string(histRecordType.code(), 2)
                .string(histActionCode.code(), 1)
                .string(histBeforeImage, 400)
                .string(histAfterImage, 400)
                .string(histReasonCode, 4)
                .string(histProcessDate, 26)
                .string(histProcessUser, 8)
                .filler(50)
                .toRecord();
    }
}
