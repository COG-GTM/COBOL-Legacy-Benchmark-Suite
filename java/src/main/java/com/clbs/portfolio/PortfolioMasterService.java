package com.clbs.portfolio;

import com.clbs.common.AuditProcessor;
import com.clbs.domain.AuditRecord;
import com.clbs.domain.PortfolioRecord;
import com.clbs.domain.ReturnCode;
import com.clbs.store.DatasetCatalog;
import com.clbs.store.FileStatus;
import com.clbs.store.IndexedFile;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

/**
 * PORTMSTR — portfolio master file maintenance. The LS-COMMAND dispatch becomes one method per
 * command; each returns the LS-RETURN-CODE (0 or 8) together with WS-ERROR-TEXT.
 *
 * <p>PORTMSTR declares its own 100-byte FD layout rather than copying PORTFLIO, and its
 * 2100-VALIDATE-PORTFOLIO accepts statuses 'A', 'I' and 'C' where PORTFLIO.cpy defines 'A', 'C'
 * and 'S'. The program's own rules are kept here.
 */
@Service
public class PortfolioMasterService {

    public static final String PROGRAM = "PORTMSTR";

    /** WS-ERROR-TEXT literals. */
    public static final String ERR_INVALID_COMMAND = "Invalid command";
    public static final String ERR_INVALID_ID = "Invalid Portfolio ID format";
    public static final String ERR_NAME_REQUIRED = "Portfolio Name is required";
    public static final String ERR_INVALID_STATUS = "Invalid Portfolio Status";
    public static final String ERR_DUPLICATE = "Portfolio ID already exists";
    public static final String ERR_WRITE = "Error writing Portfolio record";
    public static final String ERR_NOT_FOUND = "Portfolio not found";
    public static final String ERR_READ = "Error reading Portfolio";
    public static final String ERR_NOT_FOUND_UPDATE = "Portfolio not found for update";
    public static final String ERR_UPDATE = "Error updating Portfolio";
    public static final String ERR_NOT_FOUND_DELETE = "Portfolio not found for deletion";
    public static final String ERR_DELETE = "Error deleting Portfolio";

    /** WS-VALID-STATUS 88-level of PORTMSTR. */
    private static final String VALID_STATUSES = "AIC";

    private final DatasetCatalog datasets;
    private final AuditProcessor auditProcessor;

    public PortfolioMasterService(DatasetCatalog datasets, AuditProcessor auditProcessor) {
        this.datasets = datasets;
        this.auditProcessor = auditProcessor;
    }

    /** LS-COMMAND-AREA after the call: the record, LS-RETURN-CODE and WS-ERROR-TEXT. */
    public record Response(int returnCode, String errorText, PortfolioRecord record) {

        public boolean ok() {
            return returnCode == ReturnCode.SUCCESS;
        }
    }

    /** 0000-MAIN. */
    public Response execute(char command, PortfolioRecord record) {
        return switch (command) {
            case 'C' -> create(record);
            case 'R' -> read(record.getPortId(), record.getAccountNo());
            case 'U' -> update(record);
            case 'D' -> delete(record.getPortId(), record.getAccountNo());
            default -> new Response(ReturnCode.ERROR, ERR_INVALID_COMMAND, record);
        };
    }

    /** 2000-CREATE-PORTFOLIO. */
    public Response create(PortfolioRecord record) {
        String validation = validate(record);
        if (validation != null) {
            return new Response(ReturnCode.ERROR, validation, record);
        }
        if (record.getCreateDate() == 0) {
            record.setCreateDate(currentDate());
        }
        record.setLastMaint(currentDate());

        String status = file().write(record);
        if (FileStatus.DUPLICATE_KEY.equals(status)) {
            return new Response(ReturnCode.ERROR, ERR_DUPLICATE, record);
        }
        if (!FileStatus.SUCCESS.equals(status)) {
            return new Response(ReturnCode.ERROR, ERR_WRITE, record);
        }
        return new Response(ReturnCode.SUCCESS, "", record);
    }

    /** 3000-READ-PORTFOLIO. */
    public Response read(String portId, String accountNo) {
        PortfolioRecord found = file().read(keyOf(portId, accountNo));
        if (found == null) {
            return new Response(ReturnCode.ERROR, ERR_NOT_FOUND, null);
        }
        return new Response(ReturnCode.SUCCESS, "", found);
    }

    /** 4000-UPDATE-PORTFOLIO, including 2100-LOG-PORTFOLIO-UPDATE. */
    public Response update(PortfolioRecord record) {
        String validation = validate(record);
        if (validation != null) {
            return new Response(ReturnCode.ERROR, validation, record);
        }

        PortfolioRecord before = file().read(record.key());
        if (before == null) {
            return new Response(ReturnCode.ERROR, ERR_NOT_FOUND_UPDATE, record);
        }

        record.setLastMaint(currentDate());
        String status = file().rewrite(record);
        if (FileStatus.NOT_FOUND.equals(status)) {
            return new Response(ReturnCode.ERROR, ERR_NOT_FOUND_UPDATE, record);
        }
        if (!FileStatus.SUCCESS.equals(status)) {
            return new Response(ReturnCode.ERROR, ERR_UPDATE, record);
        }

        logPortfolioUpdate(before, record);
        return new Response(ReturnCode.SUCCESS, "", record);
    }

    /** 5000-DELETE-PORTFOLIO. */
    public Response delete(String portId, String accountNo) {
        String key = keyOf(portId, accountNo);
        PortfolioRecord existing = file().read(key);
        String status = file().delete(key);
        if (FileStatus.NOT_FOUND.equals(status)) {
            return new Response(ReturnCode.ERROR, ERR_NOT_FOUND_DELETE, null);
        }
        if (!FileStatus.SUCCESS.equals(status)) {
            return new Response(ReturnCode.ERROR, ERR_DELETE, null);
        }
        return new Response(ReturnCode.SUCCESS, "", existing);
    }

    /** 2100-VALIDATE-PORTFOLIO; returns the WS-ERROR-TEXT or null when valid. */
    public String validate(PortfolioRecord record) {
        String id = record.getPortId() == null ? "" : record.getPortId();
        if (id.length() < 8 || !id.startsWith("PORT") || !isNumeric(id.substring(4, 8))) {
            return ERR_INVALID_ID;
        }
        if (record.getClientName() == null || record.getClientName().isBlank()) {
            return ERR_NAME_REQUIRED;
        }
        if (VALID_STATUSES.indexOf(record.getStatus()) < 0) {
            return ERR_INVALID_STATUS;
        }
        return null;
    }

    /** 2100-LOG-PORTFOLIO-UPDATE: CALL 'AUDPROC'. */
    private void logPortfolioUpdate(PortfolioRecord before, PortfolioRecord after) {
        AuditRecord audit = new AuditRecord();
        audit.setSystemId("PORTFOLI");
        audit.setUserId(after.getLastUser());
        audit.setProgramId(PROGRAM);
        audit.setType(AuditRecord.TYPE_TRANSACTION);
        audit.setAction(AuditRecord.ACTION_UPDATE);
        audit.setStatus(AuditRecord.STATUS_SUCCESS);
        audit.setPortfolioId(after.getPortId());
        audit.setAccountNo(after.getAccountNo());
        audit.setBeforeImage(before.image());
        audit.setAfterImage(after.image());
        audit.setMessage("Portfolio updated successfully");
        auditProcessor.write(audit);
    }

    private IndexedFile<PortfolioRecord> file() {
        return datasets.portfolioFile();
    }

    private static String keyOf(String portId, String accountNo) {
        PortfolioRecord probe = new PortfolioRecord();
        probe.setPortId(portId);
        probe.setAccountNo(accountNo);
        return probe.key();
    }

    private static int currentDate() {
        return Integer.parseInt(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
    }

    private static boolean isNumeric(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return !value.isEmpty();
    }
}
