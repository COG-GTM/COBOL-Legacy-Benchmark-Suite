package com.clbs.portfolio;

import com.clbs.common.AuditProcessor;
import com.clbs.common.ProgramResult;
import com.clbs.domain.AuditRecord;
import com.clbs.domain.PortfolioRecord;
import com.clbs.store.DatasetCatalog;
import com.clbs.store.FileStatus;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * PORTDEL — deletes portfolios listed in DELEFILE and writes one AUDFILE record per deletion.
 */
@Service
public class PortfolioDeleteProgram {

    public static final String PROGRAM = "PORTDEL";

    /** DEL-REASON-CODE 88-levels. */
    public static final String REASON_CLOSED = "01";
    public static final String REASON_TRANSFERRED = "02";
    public static final String REASON_REQUESTED = "03";

    private final DatasetCatalog datasets;
    private final AuditProcessor auditProcessor;

    public PortfolioDeleteProgram(DatasetCatalog datasets, AuditProcessor auditProcessor) {
        this.datasets = datasets;
        this.auditProcessor = auditProcessor;
    }

    /** DELETE-RECORD: DEL-KEY plus DEL-REASON-CODE. */
    public record DeleteRequest(String portId, String accountNo, String reasonCode) {
    }

    /** 0000-MAIN. */
    public ProgramResult run(List<DeleteRequest> requests) {
        ProgramResult result = new ProgramResult(PROGRAM);
        long deleted = 0;
        long notFound = 0;
        long errors = 0;

        for (DeleteRequest request : requests) {
            String key = keyOf(request.portId(), request.accountNo());
            PortfolioRecord record = datasets.portfolioFile().read(key);

            // 2100-PROCESS-DELETE
            if (record == null) {
                notFound++;
                result.display("Record not found: " + key);
                continue;
            }

            // 2200-DELETE-RECORD
            if (FileStatus.SUCCESS.equals(datasets.portfolioFile().delete(key))) {
                deleted++;
                writeAudit(record, request.reasonCode());
            } else {
                errors++;
                result.display("Delete failed for: " + key);
            }
        }

        // 3000-TERMINATE
        result.display("Records deleted:  " + deleted);
        result.display("Records not found:" + notFound);
        result.display("Errors occurred:  " + errors);
        result.count("deleted", deleted).count("notFound", notFound).count("errors", errors);
        return result;
    }

    /** 2300-WRITE-AUDIT. */
    private void writeAudit(PortfolioRecord record, String reasonCode) {
        AuditRecord audit = new AuditRecord();
        audit.setProgramId(PROGRAM);
        audit.setAction(AuditRecord.ACTION_DELETE);
        audit.setPortfolioId(record.getPortId());
        audit.setAccountNo(record.getAccountNo());
        audit.setStatus(String.valueOf(record.getStatus()));
        audit.setBeforeImage(record.image());
        audit.setMessage("Deletion reason " + reasonCode);
        auditProcessor.write(audit);
    }

    private static String keyOf(String portId, String accountNo) {
        PortfolioRecord probe = new PortfolioRecord();
        probe.setPortId(portId);
        probe.setAccountNo(accountNo);
        return probe.key();
    }
}
