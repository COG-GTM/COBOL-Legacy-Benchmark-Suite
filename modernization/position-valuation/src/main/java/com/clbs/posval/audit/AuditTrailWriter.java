package com.clbs.posval.audit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Port of {@code src/programs/common/AUDPROC.cbl} — the audit trail processing subroutine, which
 * appends one record to the {@code AUDFILE} sequential file per call and returns 0 on success or 8
 * when the file cannot be opened or written.
 *
 * <p>Two defects in the COBOL are <b>not</b> reproduced here, because reproducing them would
 * corrupt data rather than preserve behaviour; both are raised in the spec (OQ-8):
 *
 * <ol>
 *   <li>{@code AUDPROC 2000-PROCESS-AUDIT} moves the timestamp into {@code AUD-TIMESTAMP} and then
 *       moves the 32 byte {@code LS-SYSTEM-INFO} over the 58 byte {@code AUD-HEADER}, which starts
 *       at {@code AUD-TIMESTAMP}. The timestamp it just set is overwritten by the system id, and
 *       the audit record carries no usable time.
 *   <li>{@code PORTTRAN 2310-WRITE-AUDIT-RECORD} calls {@code AUDPROC} passing {@code AUDIT-RECORD}
 *       (the AUDITLOG layout) where {@code AUDPROC} expects {@code LS-AUDIT-REQUEST} (a different
 *       layout), and then tests the special register {@code RETURN-CODE}, which {@code AUDPROC}
 *       never sets — it sets {@code LS-RETURN-CODE}. The audit failure path in PORTTRAN is
 *       therefore unreachable and the record written is misaligned.
 * </ol>
 */
@Service
public class AuditTrailWriter {

    private final List<AuditRecord> records = new ArrayList<>();

    /** {@code CALL 'AUDPROC' USING LS-AUDIT-REQUEST}; returns {@code LS-RETURN-CODE}. */
    public int write(AuditRecord record) {
        records.add(record);
        return 0;
    }

    /** The contents of the {@code AUDFILE} file, in write order. */
    public List<AuditRecord> records() {
        return Collections.unmodifiableList(records);
    }

    public void clear() {
        records.clear();
    }
}
