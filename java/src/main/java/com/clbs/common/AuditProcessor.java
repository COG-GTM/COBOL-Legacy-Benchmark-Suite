package com.clbs.common;

import com.clbs.domain.AuditRecord;
import com.clbs.domain.ReturnCode;
import com.clbs.store.DatasetCatalog;
import com.clbs.store.FileStatus;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

/**
 * AUDPROC — audit trail processing subroutine. Stamps the record with the current timestamp and
 * appends it to the AUDFILE dataset (OPEN EXTEND / WRITE), returning 0 or 8 like LS-RETURN-CODE.
 */
@Service
public class AuditProcessor {

    /** COBOL ACCEPT ... FROM TIME STAMP layout, PIC X(26). */
    public static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS");

    private final DatasetCatalog datasets;

    public AuditProcessor(DatasetCatalog datasets) {
        this.datasets = datasets;
    }

    public int write(AuditRecord request) {
        AuditRecord record = request.copy();
        record.setTimestamp(LocalDateTime.now().format(TIMESTAMP));
        String status = datasets.auditFile().write(record);
        return FileStatus.SUCCESS.equals(status) ? ReturnCode.SUCCESS : ReturnCode.ERROR;
    }
}
