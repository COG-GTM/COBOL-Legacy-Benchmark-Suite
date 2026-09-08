package com.clbs.common;

import com.clbs.db2.ErrorLogEntity;
import com.clbs.db2.ErrorLogRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * ERRPROC — standard error processing subroutine. Writes the formatted ERR-MESSAGE to the error log
 * (ERRLOG table here instead of the sequential ERRLOG dataset), echoes the DISPLAY block and
 * returns LS-SEVERITY as the return code.
 */
@Service
public class ErrorProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ErrorProcessor.class);
    private static final String SEPARATOR =
            "====================================================";

    private final ErrorLogRepository errorLog;
    private final List<ErrorMessage> journal = Collections.synchronizedList(new ArrayList<>());

    public ErrorProcessor(ErrorLogRepository errorLog) {
        this.errorLog = errorLog;
    }

    public int process(ErrorMessage request) {
        request.setDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        request.setTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        journal.add(request);
        writeLog(request);
        displayError(request);
        return request.getSeverity();
    }

    /** ERRPROC 2100-WRITE-LOG. */
    private void writeLog(ErrorMessage message) {
        ErrorLogEntity entity = new ErrorLogEntity();
        entity.setTimestamp(LocalDateTime.now().format(AuditProcessor.TIMESTAMP));
        entity.setProgramId(message.getProgramId());
        entity.setParagraphName(message.getCategory());
        entity.setSeverity(String.valueOf(message.getSeverity()));
        entity.setMessage(truncate(message.getText(), 80));
        errorLog.save(entity);
    }

    /** ERRPROC 2200-DISPLAY-ERROR. */
    private void displayError(ErrorMessage m) {
        LOG.error("{}\nERROR DETECTED: {} {}\nPROGRAM:       {}\nCATEGORY:      {}\n"
                        + "CODE:          {}\nSEVERITY:      {}\nMESSAGE:       {}\n"
                        + "DETAILS:       {}\n{}",
                SEPARATOR, m.getDate(), m.getTime(), m.getProgramId(), m.getCategory(),
                m.getCode(), m.getSeverity(), m.getText(), m.getDetails(), SEPARATOR);
    }

    public List<ErrorMessage> journal() {
        return List.copyOf(journal);
    }

    public void clearJournal() {
        journal.clear();
    }

    private static String truncate(String value, int length) {
        if (value == null) {
            return null;
        }
        return value.length() > length ? value.substring(0, length) : value;
    }
}
