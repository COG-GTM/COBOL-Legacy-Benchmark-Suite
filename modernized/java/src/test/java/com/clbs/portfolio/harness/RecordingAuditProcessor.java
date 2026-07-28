package com.clbs.portfolio.harness;

import com.clbs.portfolio.model.AuditRecord;
import com.clbs.portfolio.service.AuditProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Test double for {@code AUDPROC}: keeps every record it is handed and reports a configurable
 * return code, so tests can drive both the success and the write-failure branch of
 * {@code 2310-WRITE-AUDIT-RECORD}.
 *
 * <p>Each record is snapshotted on arrival because callers reuse a single audit area, exactly as
 * the COBOL program does.
 */
public class RecordingAuditProcessor implements AuditProcessor {

    private final List<AuditRecord> records = new ArrayList<>();
    private int returnCode = RETURN_SUCCESS;

    @Override
    public int process(AuditRecord auditRecord) {
        records.add(new AuditRecord(auditRecord));
        return returnCode;
    }

    /** Makes the next and all subsequent calls report a failed write. */
    public RecordingAuditProcessor failing() {
        this.returnCode = RETURN_ERROR;
        return this;
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }

    /** Every record written, in call order. */
    public List<AuditRecord> records() {
        return Collections.unmodifiableList(records);
    }

    public int count() {
        return records.size();
    }

    /** The most recent record, or {@code null} when nothing has been written. */
    public AuditRecord last() {
        return records.isEmpty() ? null : records.get(records.size() - 1);
    }

    public void reset() {
        records.clear();
    }
}
