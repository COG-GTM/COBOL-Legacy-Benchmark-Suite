package com.clbs.posval.error;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Port of {@code src/programs/common/ERRPROC.cbl} — the standard error processing subroutine.
 *
 * <p>{@code ERRPROC} appends the error record to the {@code ERRLOG} sequential file
 * ({@code 2100-WRITE-LOG}), echoes it to the job log ({@code 2200-DISPLAY-ERROR}) and returns the
 * severity it was given as its return code ({@code 2000-PROCESS-ERROR}). It never fails the
 * caller: a failure to open or write the log is displayed and swallowed.
 *
 * <p>The log is kept in memory here so that parity tests can assert on it; a production deployment
 * would bind the sink to a file or a log appender.
 */
@Service
public class ErrorProcessor {

    private static final Logger log = LoggerFactory.getLogger(ErrorProcessor.class);

    private final List<ErrorRecord> entries = new ArrayList<>();

    /** {@code CALL 'ERRPROC' USING LS-ERROR-REQUEST}; returns {@code LS-RETURN-CODE}. */
    public int process(ErrorRecord record) {
        entries.add(record);
        log.error("ERROR DETECTED program={} category={} code={} severity={} text={} details={}",
                record.programName(), record.category(), record.code(),
                record.severity(), record.text(), record.details());
        return record.severity();
    }

    /** The contents of the {@code ERRLOG} file, in write order. */
    public List<ErrorRecord> entries() {
        return Collections.unmodifiableList(entries);
    }

    public void clear() {
        entries.clear();
    }
}
